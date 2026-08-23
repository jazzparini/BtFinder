package com.example.btfinder.util

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import kotlin.math.PI
import kotlin.math.sin

/**
 * Pitido para ubicar el audífono/dispositivo de audio (RF adicional).
 *
 * A diferencia de ToneGenerator (que solo suena por el altavoz del
 * teléfono salvo que el sistema decida enrutarlo), esto usa AudioTrack con
 * [AudioTrack.setPreferredDevice] para forzar la salida al dispositivo
 * Bluetooth seleccionado, sin importar el perfil (A2DP, HFP, BLE Audio,
 * HearingAid). Si el dispositivo no aparece como salida de audio activa,
 * se hace un fallback silencioso al enrutamiento por defecto (altavoz).
 *
 * El tono es agudo para distinguirse fácil del ruido ambiente, pero corto
 * (220 ms) y a volumen moderado (no al máximo posible), y respeta el
 * volumen de alarma que el usuario tenga configurado: no se sube el
 * volumen del sistema por su cuenta. Esto evita un pitido dañino, sobre
 * todo importante si suena por un audífono que puede amplificar el sonido.
 */
class DeviceBeeper(private val context: Context) {

    private val sampleRate = 44_100
    private val frequencyHz = 3_800.0
    private val durationSeconds = 0.22
    private val toneData: ShortArray by lazy { buildTone() }

    private var audioTrack: AudioTrack? = null

    @SuppressLint("MissingPermission")
    fun beep(targetDeviceAddress: String?) {
        val track = ensureTrack() ?: return

        track.setPreferredDevice(targetDeviceAddress?.let(::findOutputDevice))

        if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
            track.stop()
        }
        track.reloadStaticData()
        track.play()
    }

    fun release() {
        audioTrack?.release()
        audioTrack = null
    }

    private fun ensureTrack(): AudioTrack? {
        audioTrack?.let { return it }

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        return runCatching {
            AudioTrack(
                attributes,
                format,
                toneData.size * 2,
                AudioTrack.MODE_STATIC,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            ).also {
                it.write(toneData, 0, toneData.size)
                audioTrack = it
            }
        }.getOrNull()
    }

    @SuppressLint("MissingPermission")
    private fun findOutputDevice(address: String): AudioDeviceInfo? {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull { device ->
            isBluetoothAudioType(device.type) && device.address.equals(address, ignoreCase = true)
        }
    }

    private fun isBluetoothAudioType(type: Int): Boolean {
        val classicTypes = intArrayOf(
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_HEARING_AID
        )
        if (type in classicTypes) return true

        // Audio BLE (auriculares/altavoces "LE Audio"), disponible desde Android 13.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (type == AudioDeviceInfo.TYPE_BLE_HEADSET || type == AudioDeviceInfo.TYPE_BLE_SPEAKER) {
                return true
            }
        }
        return false
    }

    private fun buildTone(): ShortArray {
        val sampleCount = (sampleRate * durationSeconds).toInt()
        val fadeSamples = (sampleRate * 0.015).toInt()
        val amplitude = Short.MAX_VALUE * 0.5

        return ShortArray(sampleCount) { i ->
            val angle = 2.0 * PI * frequencyHz * i / sampleRate
            val envelope = when {
                i < fadeSamples -> i.toDouble() / fadeSamples
                i > sampleCount - fadeSamples -> (sampleCount - i).toDouble() / fadeSamples
                else -> 1.0
            }
            (sin(angle) * amplitude * envelope).toInt().toShort()
        }
    }
}
