package com.example.btfinder.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * RF-01: permisos Bluetooth requeridos según la versión de Android.
 * A partir de Android 12 (API 31) se usan BLUETOOTH_SCAN / BLUETOOTH_CONNECT;
 * en versiones anteriores se depende de ACCESS_FINE_LOCATION.
 */
object Permissions {

    fun requiredBluetoothPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun hasBluetoothPermissions(context: Context): Boolean {
        return requiredBluetoothPermissions().all { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
