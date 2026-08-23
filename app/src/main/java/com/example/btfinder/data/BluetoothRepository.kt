package com.example.btfinder.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.example.btfinder.util.Permissions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class BluetoothRepository(
    private val context: Context
) {
    private val adapter: BluetoothAdapter?
        get() = BluetoothAdapter.getDefaultAdapter()

    private fun hasBluetoothPermissions(): Boolean =
        Permissions.hasBluetoothPermissions(context)

    /**
     * El adaptador puede existir (hardware compatible) pero estar apagado.
     * RF: pedir al usuario que active Bluetooth si no lo está.
     */
    fun isBluetoothEnabled(): Boolean = adapter?.isEnabled == true

    fun isBluetoothSupported(): Boolean = adapter != null

    /**
     * RF-02: dispositivos vinculados (emparejados) que el usuario puede seleccionar.
     * Incluye audífonos Bluetooth clásicos, que pueden no anunciarse por BLE
     * (ver "Consideración importante" en la sección 10 del documento de diseño).
     */
    @SuppressLint("MissingPermission")
    fun pairedDevices(): List<BluetoothDevice> {
        if (!hasBluetoothPermissions()) return emptyList()
        return adapter?.bondedDevices?.toList().orEmpty()
    }

    /**
     * RF-03/RF-04: flujo de resultados de escaneo BLE con su RSSI crudo.
     * El ViewModel filtra por dirección MAC y aplica el promedio móvil.
     */
    @SuppressLint("MissingPermission")
    fun scanResults(): Flow<ScanResult> = callbackFlow {
        if (!hasBluetoothPermissions()) {
            close(SecurityException("Faltan permisos Bluetooth"))
            return@callbackFlow
        }

        if (adapter?.isEnabled != true) {
            close(IllegalStateException("Bluetooth está desactivado"))
            return@callbackFlow
        }

        val scanner: BluetoothLeScanner =
            adapter?.bluetoothLeScanner
                ?: run {
                    close(IllegalStateException("BLE no disponible"))
                    return@callbackFlow
                }

        val callback = object : ScanCallback() {
            override fun onScanResult(
                callbackType: Int,
                result: ScanResult
            ) {
                trySend(result)
            }

            override fun onScanFailed(errorCode: Int) {
                close(
                    IllegalStateException(
                        "El escaneo falló: $errorCode"
                    )
                )
            }
        }

        scanner.startScan(callback)

        awaitClose {
            scanner.stopScan(callback)
        }
    }

    /**
     * RF-03 fallback (sección 10, "Consideración importante"): los audífonos
     * Bluetooth clásicos (A2DP/HFP/HearingAid) no se anuncian por BLE, así
     * que no aparecen en [scanResults]. Aquí se expone su estado de conexión
     * clásico (conectado/no conectado) como sustituto del RSSI, combinando
     * un chequeo inicial vía los proxies de perfil con las notificaciones
     * ACL en vivo del sistema.
     */
    @SuppressLint("MissingPermission")
    fun classicConnectionUpdates(device: BluetoothDevice): Flow<Boolean> = callbackFlow {
        val bluetoothAdapter = adapter
        if (bluetoothAdapter == null || !hasBluetoothPermissions()) {
            trySend(false)
            awaitClose { }
            return@callbackFlow
        }

        val classicProfiles = intArrayOf(
            BluetoothProfile.HEADSET,
            BluetoothProfile.A2DP,
            BluetoothProfile.HEARING_AID
        )
        val connectedProfiles = mutableSetOf<Int>()
        val activeProxies = mutableListOf<Pair<Int, BluetoothProfile>>()

        fun isConnectedNow(): Boolean =
            activeProxies.any { (_, proxy) ->
                proxy.connectedDevices.any { it.address == device.address }
            }

        val serviceListener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                activeProxies.add(profile to proxy)
                trySend(isConnectedNow())
            }

            override fun onServiceDisconnected(profile: Int) {
                activeProxies.removeAll { it.first == profile }
            }
        }

        classicProfiles.forEach { profile ->
            bluetoothAdapter.getProfileProxy(context, serviceListener, profile)
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                val eventDevice = intent.getBluetoothDeviceExtra() ?: return
                if (eventDevice.address != device.address) return

                when (intent.action) {
                    BluetoothDevice.ACTION_ACL_CONNECTED -> trySend(true)
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> trySend(isConnectedNow())
                }
            }
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        awaitClose {
            context.unregisterReceiver(receiver)
            activeProxies.forEach { (profile, proxy) ->
                bluetoothAdapter.closeProfileProxy(profile, proxy)
            }
        }
    }

    private fun Intent.getBluetoothDeviceExtra(): BluetoothDevice? {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
    }
}
