package com.example.btfinder.util

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * RF-06: emite un pitido desde el teléfono (botón "Probar sonido").
 * Si el teléfono está conectado al audífono mediante un perfil de audio,
 * el sonido podría salir por el audífono; no se garantiza para todos los modelos.
 */
class BeepPlayer {

    private var toneGenerator: ToneGenerator? = null

    /**
     * [durationMillis] es corto por defecto para permitir pitidos repetidos
     * a intervalos cortos (RF: pitidos más frecuentes cerca del audífono)
     * sin que se solapen entre sí. Reutiliza el mismo ToneGenerator en vez
     * de recrearlo en cada llamada para evitar cortes audibles al repetir.
     */
    fun play(durationMillis: Int = 400) {
        val generator = toneGenerator ?: ToneGenerator(
            AudioManager.STREAM_ALARM,
            100
        ).also { toneGenerator = it }

        generator.startTone(ToneGenerator.TONE_PROP_BEEP, durationMillis)
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }
}
