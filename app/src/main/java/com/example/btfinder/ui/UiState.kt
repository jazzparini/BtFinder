package com.example.btfinder.ui

import android.bluetooth.BluetoothDevice
import com.example.btfinder.domain.Proximity

data class UiState(
    val bluetoothSupported: Boolean = true,
    val bluetoothEnabled: Boolean = false,
    val pairedDevices: List<BluetoothDevice> = emptyList(),
    val selectedDevice: BluetoothDevice? = null,
    val isScanning: Boolean = false,
    val rssi: Int? = null,
    val proximity: Proximity = Proximity.NOT_FOUND,
    val lastSeenMillis: Long? = null,
    val error: String? = null
)
