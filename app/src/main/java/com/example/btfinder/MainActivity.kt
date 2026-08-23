package com.example.btfinder

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.btfinder.data.BluetoothRepository
import com.example.btfinder.data.PreferencesRepository
import com.example.btfinder.service.BluetoothScanService
import com.example.btfinder.ui.FinderScreen
import com.example.btfinder.ui.FinderViewModel
import com.example.btfinder.ui.FinderViewModelFactory
import com.example.btfinder.util.DeviceBeeper
import com.example.btfinder.util.Permissions
import com.example.btfinder.util.VibratorHelper

class MainActivity : ComponentActivity() {

    private var scanServiceStarted = false

    private val permissionsLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            recreate()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestBluetoothPermissions()

        val repository = BluetoothRepository(applicationContext)
        val preferences = PreferencesRepository(applicationContext)
        val beepPlayer = DeviceBeeper(applicationContext)
        val vibratorHelper = VibratorHelper(applicationContext)

        setContent {
            val finderViewModel: FinderViewModel = viewModel(
                factory = FinderViewModelFactory(
                    repository,
                    preferences,
                    beepPlayer,
                    vibratorHelper
                )
            )

            val state by finderViewModel.uiState.collectAsState()

            LaunchedEffect(Unit) {
                finderViewModel.loadPairedDevices()
            }

            // RF-07: mantiene el servicio en primer plano sincronizado con
            // el estado de escaneo, iniciado desde una acción visible del
            // usuario (cumple la restricción de Android sobre arranque de
            // servicios en primer plano desde segundo plano).
            LaunchedEffect(state.isScanning) {
                if (state.isScanning && !scanServiceStarted) {
                    startForegroundScanService()
                } else if (!state.isScanning && scanServiceStarted) {
                    stopForegroundScanService()
                }
            }

            FinderScreen(
                state = state,
                onSelectDevice = finderViewModel::selectDevice,
                onStartScan = finderViewModel::startScan,
                onStopScan = finderViewModel::stopScan,
                onTestSound = finderViewModel::testSound
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (scanServiceStarted) {
            stopForegroundScanService()
        }
    }

    private fun requestBluetoothPermissions() {
        if (Permissions.hasBluetoothPermissions(applicationContext)) return
        permissionsLauncher.launch(Permissions.requiredBluetoothPermissions())
    }

    private fun startForegroundScanService() {
        val intent = Intent(this, BluetoothScanService::class.java)
        startForegroundService(intent)
        scanServiceStarted = true
    }

    private fun stopForegroundScanService() {
        stopService(Intent(this, BluetoothScanService::class.java))
        scanServiceStarted = false
    }
}
