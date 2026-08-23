package com.example.btfinder.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
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
}
