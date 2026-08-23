package com.example.btfinder.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.btfinder.data.BluetoothRepository
import com.example.btfinder.data.PreferencesRepository
import com.example.btfinder.domain.Proximity
import com.example.btfinder.domain.RssiFilter
import com.example.btfinder.domain.isImprovement
import com.example.btfinder.domain.proximityFromRssi
import com.example.btfinder.util.BeepPlayer
import com.example.btfinder.util.VibratorHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Caso de uso "Buscar audífono" (sección 16) implementado como flujo
 * unidireccional UI -> Evento -> ViewModel -> Estado -> UI.
 */
class FinderViewModel(
    private val repository: BluetoothRepository,
    private val preferences: PreferencesRepository,
    private val beepPlayer: BeepPlayer,
    private val vibratorHelper: VibratorHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val rssiFilter = RssiFilter()
    private var scanJob: Job? = null
    private var timeoutJob: Job? = null
    private var classicJob: Job? = null
    private var beepJob: Job? = null

    private var bleProximity: Proximity = Proximity.NOT_FOUND
    private var classicConnected: Boolean = false

    init {
        _uiState.value = _uiState.value.copy(
            bluetoothSupported = repository.isBluetoothSupported(),
            bluetoothEnabled = repository.isBluetoothEnabled()
        )
    }

    /**
     * RF-02 + persistencia: carga los dispositivos vinculados y, si existe,
     * vuelve a seleccionar el último dispositivo usado.
     */
    @SuppressLint("MissingPermission")
    fun loadPairedDevices() {
        val devices = repository.pairedDevices()

        _uiState.value = _uiState.value.copy(
            bluetoothEnabled = repository.isBluetoothEnabled(),
            pairedDevices = devices
        )

        viewModelScope.launch {
            val lastAddress = preferences.lastDeviceAddress.first()

            if (lastAddress != null && _uiState.value.selectedDevice == null) {
                devices.firstOrNull { it.address == lastAddress }?.let { device ->
                    selectDevice(device)
                }
            }
        }
    }

    fun selectDevice(device: BluetoothDevice) {
        bleProximity = Proximity.NOT_FOUND
        classicConnected = false

        _uiState.value = _uiState.value.copy(
            selectedDevice = device,
            rssi = null,
            proximity = Proximity.NOT_FOUND
        )

        viewModelScope.launch {
            preferences.saveLastDeviceAddress(device.address)
        }
    }

    fun startScan() {
        val target = _uiState.value.selectedDevice ?: return

        stopScan()

        rssiFilter.clear()
        bleProximity = Proximity.NOT_FOUND
        classicConnected = false

        _uiState.value = _uiState.value.copy(
            isScanning = true,
            error = null
        )

        scanJob = viewModelScope.launch {
            runCatching {
                repository.scanResults().collect { result ->
                    val device = result.device

                    if (device.address == target.address) {
                        val filteredRssi = rssiFilter.add(result.rssi)
                        bleProximity = proximityFromRssi(filteredRssi)

                        _uiState.value = _uiState.value.copy(
                            rssi = filteredRssi,
                            lastSeenMillis = System.currentTimeMillis()
                        )

                        applyProximity()
                        restartNotFoundTimeout()
                    }
                }
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    error = throwable.message ?: "Error durante el escaneo"
                )
            }
        }

        // Fallback para audífonos Bluetooth clásicos (sección 10): no se
        // anuncian por BLE, así que se detecta su conexión por separado.
        classicJob = viewModelScope.launch {
            repository.classicConnectionUpdates(target).collect { connected ->
                classicConnected = connected
                applyProximity()
            }
        }

        // RF adicional: pitidos tipo "detector de metales", más seguidos
        // cuanto más cerca está el audífono, para ubicarlo sin mirar la
        // pantalla. En NOT_FOUND no suena nada.
        beepJob = viewModelScope.launch {
            while (isActive) {
                val interval = beepIntervalMillis(_uiState.value.proximity)

                if (interval != null) {
                    beepPlayer.play(durationMillis = (interval * 0.6).toInt())
                    delay(interval)
                } else {
                    delay(300)
                }
            }
        }
    }

    private fun beepIntervalMillis(proximity: Proximity): Long? = when (proximity) {
        Proximity.CONNECTED, Proximity.VERY_CLOSE -> 220L
        Proximity.CLOSE -> 450L
        Proximity.FAR -> 900L
        Proximity.WEAK -> 1600L
        Proximity.NOT_FOUND -> null
    }

    /**
     * Combina la señal BLE (con distancia aproximada) y la conexión clásica
     * (sin distancia) en el estado único que ve la UI. La conexión clásica
     * tiene prioridad porque, a diferencia del RSSI BLE, confirma que el
     * audífono está realmente conectado en este momento.
     */
    private fun applyProximity() {
        val previousProximity = _uiState.value.proximity
        val nextProximity = if (classicConnected) Proximity.CONNECTED else bleProximity

        if (nextProximity == previousProximity) return

        _uiState.value = _uiState.value.copy(proximity = nextProximity)

        // MVP sección 2: "Vibración cuando la señal mejore".
        if (isImprovement(previousProximity, nextProximity)) {
            vibratorHelper.vibrateShort()
        }
    }

    private fun restartNotFoundTimeout() {
        timeoutJob?.cancel()

        // RF-05: sin recepción BLE durante 8 s -> vuelve al estado clásico
        // (conectado o no detectado, según [classicConnected]).
        timeoutJob = viewModelScope.launch {
            delay(8_000)

            bleProximity = Proximity.NOT_FOUND
            _uiState.value = _uiState.value.copy(rssi = null)
            applyProximity()
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        timeoutJob?.cancel()
        classicJob?.cancel()
        beepJob?.cancel()

        scanJob = null
        timeoutJob = null
        classicJob = null
        beepJob = null

        _uiState.value = _uiState.value.copy(
            isScanning = false
        )
    }

    /** Botón de prueba de sonido (incluido en el MVP, sección 2 / RF-06). */
    fun testSound() {
        beepPlayer.play()
    }

    override fun onCleared() {
        stopScan()
        beepPlayer.release()
        super.onCleared()
    }
}
