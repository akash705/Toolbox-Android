package com.toolbox.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.random.Random

/**
 * Minimal, self-contained white-noise generator for the Focus Timer's optional work-phase ambience.
 *
 * Deliberately independent of the shipped White Noise tool: extracting that tool's Compose-owned
 * audio path carried regression risk, and driving audio from the timer's `specialUse` foreground
 * service would be a `mediaPlayback` FGS-type mismatch (a Play-policy concern flagged in review).
 * This engine plays only while the Focus Timer screen is in the foreground, so it needs no
 * foreground-service audio and introduces no new FGS type. Full background ambience is deferred.
 */
class NoiseEngine {

    @Volatile private var playing = false
    private var track: AudioTrack? = null

    fun start() {
        if (playing) return
        playing = true
        val sampleRate = 44100
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(2048)

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(minBuf)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track = audioTrack
        audioTrack.play()

        thread(name = "focus-noise") {
            val buffer = ShortArray(minBuf / 2)
            val gain = 0.25 // keep it gentle
            while (playing) {
                for (i in buffer.indices) {
                    buffer[i] = (Random.nextInt(-32768, 32768) * gain).toInt().toShort()
                }
                val t = track ?: break
                t.write(buffer, 0, buffer.size)
            }
        }
    }

    fun stop() {
        playing = false
        runCatching { track?.pause() }
        runCatching { track?.flush() }
        runCatching { track?.release() }
        track = null
    }
}
