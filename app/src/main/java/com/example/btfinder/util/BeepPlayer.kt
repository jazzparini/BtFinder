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

    fun play() {
        toneGenerator?.release()

        toneGenerator = ToneGenerator(
            AudioManager.STREAM_ALARM,
            100
        )

        toneGenerator?.startTone(
            ToneGenerator.TONE_PROP_BEEP,
            400
        )
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }
}
