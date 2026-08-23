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
                        val now = System.currentTimeMillis()
                        val previousProximity = _uiState.value.proximity
                        val nextProximity = proximityFromRssi(filteredRssi)

                        _uiState.value = _uiState.value.copy(
                            rssi = filteredRssi,
                            proximity = nextProximity,
                            lastSeenMillis = now
                        )

                        // MVP sección 2: "Vibración cuando la señal mejore".
                        if (isImprovement(previousProximity, nextProximity)) {
                            vibratorHelper.vibrateShort()
                        }

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
    }

    private fun restartNotFoundTimeout() {
        timeoutJob?.cancel()

        // RF-05: sin recepción durante 8 s -> "No detectado".
        timeoutJob = viewModelScope.launch {
            delay(8_000)

            _uiState.value = _uiState.value.copy(
                rssi = null,
                proximity = Proximity.NOT_FOUND
            )
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        timeoutJob?.cancel()

        scanJob = null
        timeoutJob = null

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
