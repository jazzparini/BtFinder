package com.example.btfinder.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * RF-07: si la búsqueda continúa cuando el usuario cambia de aplicación,
 * se usa un servicio en primer plano de tipo connectedDevice con una
 * notificación visible (sección 15). MainActivity lo inicia/detiene de forma
 * explícita mientras la app está visible, ya que Android restringe el
 * arranque de servicios en primer plano desde segundo plano.
 */
class BluetoothScanService : Service() {

    companion object {
        private const val CHANNEL_ID = "bluetooth_scan"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setContentTitle("BT Finder está buscando")
            .setContentText("Escaneando dispositivos Bluetooth cercanos")
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Búsqueda Bluetooth",
                NotificationManager.IMPORTANCE_LOW
            )

            getSystemService(
                NotificationManager::class.java
            ).createNotificationChannel(channel)
        }
    }
}
