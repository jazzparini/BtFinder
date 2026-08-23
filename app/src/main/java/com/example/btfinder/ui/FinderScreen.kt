package com.example.btfinder.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * displayName no es un campo estándar de BluetoothDevice: se usa getName()
 * (accesible como .name en Kotlin) y, si el dispositivo no reporta nombre,
 * se muestra su dirección MAC.
 */
@get:SuppressLint("MissingPermission")
private val BluetoothDevice.displayLabel: String
    get() = name ?: address

@Composable
fun FinderScreen(
    state: UiState,
    onSelectDevice: (BluetoothDevice) -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onTestSound: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "BT Finder",
            style = MaterialTheme.typography.headlineMedium
        )

        if (!state.bluetoothSupported) {
            Text(
                text = "Este dispositivo no tiene Bluetooth compatible.",
                color = MaterialTheme.colorScheme.error
            )
        } else if (!state.bluetoothEnabled) {
            Text(
                text = "Bluetooth está desactivado. Actívalo para buscar tu audífono.",
                color = MaterialTheme.colorScheme.error
            )
        }

        Text(text = "Selecciona un dispositivo vinculado")

        Button(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = state.selectedDevice?.displayLabel
                    ?: "Seleccionar dispositivo"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (state.pairedDevices.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No hay dispositivos vinculados") },
                    onClick = { expanded = false }
                )
            }
            state.pairedDevices.forEach { device ->
                DropdownMenuItem(
                    text = { Text(device.displayLabel) },
                    onClick = {
                        onSelectDevice(device)
                        expanded = false
                    }
                )
            }
        }

        Text(
            text = "${state.proximity.emoji} ${state.proximity.label}",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = state.rssi?.let { "RSSI: $it dBm" }
                ?: "RSSI: sin señal"
        )

        if (state.isScanning) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onStopScan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Detener búsqueda")
            }
        } else {
            Button(
                onClick = onStartScan,
                enabled = state.selectedDevice != null && state.bluetoothEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Buscar dispositivo")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // RF-06 / MVP: botón de prueba de sonido. El pitido suena en el
            // teléfono; solo saldrá por el audífono si el sistema enruta el
            // audio de alarma hacia él, lo cual depende del modelo.
            OutlinedButton(
                onClick = onTestSound,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Probar sonido")
            }
        }

        state.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }

        state.lastSeenMillis?.let {
            Text(
                text = "Última detección registrada",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
