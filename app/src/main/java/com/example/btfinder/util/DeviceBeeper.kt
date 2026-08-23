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
 * Sonidos para ubicar el audífono/dispositivo de audio (RF adicional).
 *
 * A diferencia de ToneGenerator (que solo suena por el altavoz del
 * teléfono salvo que el sistema decida enrutarlo), esto usa AudioTrack con
 * [AudioTrack.setPreferredDevice] para forzar la salida al dispositivo
 * Bluetooth seleccionado, sin importar el perfil (A2DP, HFP, BLE Audio,
 * HearingAid). Si el dispositivo no aparece como salida de audio activa,
 * se hace un fallback silencioso al enrutamiento por defecto (altavoz).
 *
 * Hay dos tonos, ambos cortos y a volumen moderado (nunca al máximo) para
 * no dañar el oído si suenan por un audífono que amplifica:
 * - [beep]: agudo (3800 Hz), más seguido cuanto más cerca (RF-05).
 * - [ping]: tipo "sonar" (1200 Hz, con caída), sincronizado con el barrido
 *   visual del radar, para confirmar que la búsqueda sigue activa aunque
 *   el dispositivo no se detecte todavía.
 */
class DeviceBeeper(private val context: Context) {

    private val sampleRate = 44_100

    private val beepToneData: ShortArray by lazy {
        buildTone(frequencyHz = 3_800.0, durationSeconds = 0.22, amplitudeScale = 0.5)
    }
    private val pingToneData: ShortArray by lazy {
        buildTone(frequencyHz = 1_200.0, durationSeconds = 0.3, amplitudeScale = 0.35)
    }

    private var beepTrack: AudioTrack? = null
    private var pingTrack: AudioTrack? = null

    fun beep(targetDeviceAddress: String?) {
        val track = beepTrack ?: buildTrack(beepToneData)?.also { beepTrack = it } ?: return
        play(track, targetDeviceAddress)
    }

    fun ping(targetDeviceAddress: String?) {
        val track = pingTrack ?: buildTrack(pingToneData)?.also { pingTrack = it } ?: return
        play(track, targetDeviceAddress)
    }

    fun release() {
        beepTrack?.release()
        beepTrack = null
        pingTrack?.release()
        pingTrack = null
    }

    @SuppressLint("MissingPermission")
    private fun play(track: AudioTrack, targetDeviceAddress: String?) {
        track.setPreferredDevice(targetDeviceAddress?.let(::findOutputDevice))

        if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
            track.stop()
        }
        track.reloadStaticData()
        track.play()
    }

    private fun buildTrack(toneData: ShortArray): AudioTrack? {
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

    private fun buildTone(
        frequencyHz: Double,
        durationSeconds: Double,
        amplitudeScale: Double
    ): ShortArray {
        val sampleCount = (sampleRate * durationSeconds).toInt()
        val fadeSamples = (sampleRate * 0.015).toInt()
        val amplitude = Short.MAX_VALUE * amplitudeScale

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
