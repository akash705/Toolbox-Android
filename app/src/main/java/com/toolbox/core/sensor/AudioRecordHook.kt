package com.toolbox.core.sensor

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Returns the current sound level in dB SPL (approximate).
 * CRITICAL: Uses DisposableEffect with AudioRecord.release() in onDispose.
 * LaunchedEffect cancellation alone does NOT release native mic resources.
 */
@Composable
fun rememberAudioLevel(): State<Float> {
    val context = LocalContext.current
    val dbLevel = remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return@DisposableEffect onDispose {}
        }

        val sampleRate = 22050
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(1024)

        val audioRecord = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
            )
        } catch (_: SecurityException) {
            null
        }

        if (audioRecord == null || audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord?.release()
            return@DisposableEffect onDispose {}
        }

        audioRecord.startRecording()

        val scope = CoroutineScope(Dispatchers.IO)
        val job: Job = scope.launch {
            val buffer = ShortArray(bufferSize / 2)
            while (isActive) {
                val read = audioRecord.read(buffer, 0, buffer.size)
                if (read > 0) {
                    var sum = 0.0
                    for (i in 0 until read) {
                        sum += buffer[i].toDouble() * buffer[i].toDouble()
                    }
                    val rms = sqrt(sum / read)
                    // Convert to approximate dB SPL (reference: Short.MAX_VALUE as 90 dB)
                    val db = if (rms > 0) 20 * log10(rms / Short.MAX_VALUE) + 90 else 0.0
                    dbLevel.floatValue = db.toFloat().coerceAtLeast(0f)
                }
            }
        }

        onDispose {
            job.cancel()
            audioRecord.stop()
            audioRecord.release()
        }
    }

    return dbLevel
}
