package com.example.btfinder.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Vibración corta cuando la señal mejora (sección 2, "Vibración al mejorar la
 * señal", y caso de uso "Mejorar la señal" en la sección 16).
 */
class VibratorHelper(context: Context) {

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    fun vibrateShort() {
        val effect = VibrationEffect.createOneShot(
            120,
            VibrationEffect.DEFAULT_AMPLITUDE
        )
        vibrator?.vibrate(effect)
    }
}
