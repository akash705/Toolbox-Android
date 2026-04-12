package com.toolbox.measurement.pitchtuner

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.AudioAttributes
import android.media.MediaRecorder
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.core.permission.PermissionGate
import com.toolbox.measurement.spectrum.FFTProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

private fun frequencyToNote(freq: Float): Triple<String, Int, Float> {
    // Returns (noteName, octave, centsDeviation)
    if (freq <= 0) return Triple("--", 0, 0f)
    val semitonesFromA4 = 12.0 * log2(freq.toDouble() / 440.0)
    val nearestSemitone = semitonesFromA4.roundToInt()
    val cents = ((semitonesFromA4 - nearestSemitone) * 100).toFloat()
    val midiNote = 69 + nearestSemitone
    val noteName = noteNames[((midiNote % 12) + 12) % 12]
    val octave = (midiNote / 12) - 1
    return Triple(noteName, octave, cents)
}

private fun noteToFrequency(noteName: String, octave: Int): Float {
    val noteIndex = noteNames.indexOf(noteName)
    if (noteIndex == -1) return 0f
    val midiNote = (octave + 1) * 12 + noteIndex
    return (440.0 * 2.0.pow((midiNote - 69.0) / 12.0)).toFloat()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PitchTunerScreen() {
    PermissionGate(
        permission = Manifest.permission.RECORD_AUDIO,
        rationale = "Pitch Tuner needs microphone access to detect musical notes from audio.",
    ) {
        var isListening by remember { mutableStateOf(false) }
        var detectedFreq by remember { mutableFloatStateOf(0f) }
        var detectedNote by remember { mutableStateOf("--") }
        var detectedOctave by remember { mutableStateOf(4) }
        var detectedCents by remember { mutableFloatStateOf(0f) }
        var job by remember { mutableStateOf<Job?>(null) }

        fun startListening() {
            job = CoroutineScope(Dispatchers.IO).launch {
                val sampleRate = 44100
                val bufferSize = AudioRecord.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                )
                val recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize * 2,
                )
                recorder.startRecording()
                val fftSize = 4096
                val buffer = ShortArray(fftSize)

                while (isActive) {
                    val read = recorder.read(buffer, 0, fftSize)
                    if (read <= 0) continue

                    // Autocorrelation-based pitch detection
                    val floatBuffer = FloatArray(read) { buffer[it].toFloat() }

                    // Check if signal is strong enough
                    val rms = sqrt(floatBuffer.map { it * it }.average().toFloat())
                    if (rms < 200) {
                        detectedFreq = 0f
                        detectedNote = "--"
                        detectedCents = 0f
                        continue
                    }

                    val freq = autocorrelationPitch(floatBuffer, sampleRate)
                    if (freq > 20 && freq < 5000) {
                        detectedFreq = freq
                        val (note, octave, cents) = frequencyToNote(freq)
                        detectedNote = note
                        detectedOctave = octave
                        detectedCents = cents
                    }
                }

                recorder.stop()
                recorder.release()
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                job?.cancel()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Note display
            Text(
                text = if (detectedNote != "--") "$detectedNote$detectedOctave" else "--",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = when {
                    detectedNote == "--" -> MaterialTheme.colorScheme.onSurfaceVariant
                    abs(detectedCents) <= 5 -> Color(0xFF66BB6A)
                    abs(detectedCents) <= 15 -> Color(0xFFFFA726)
                    else -> Color(0xFFEF5350)
                },
            )

            // Frequency display
            Text(
                text = if (detectedFreq > 0) "%.1f Hz".format(detectedFreq) else "-- Hz",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Tuning gauge
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Cents display
                    val centsText = when {
                        detectedNote == "--" -> "No signal"
                        detectedCents > 0 -> "+%.0f cents (sharp)".format(detectedCents)
                        detectedCents < 0 -> "%.0f cents (flat)".format(detectedCents)
                        else -> "In tune!"
                    }
                    Text(
                        text = centsText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Visual gauge bar
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                    ) {
                        val centerX = size.width / 2
                        val barY = size.height / 2
                        val barWidth = size.width * 0.8f
                        val barStart = centerX - barWidth / 2
                        val barEnd = centerX + barWidth / 2

                        // Track
                        drawLine(
                            color = Color(0xFFE0E0E0),
                            start = Offset(barStart, barY),
                            end = Offset(barEnd, barY),
                            strokeWidth = 8f,
                            cap = StrokeCap.Round,
                        )

                        // Center tick
                        drawLine(
                            color = Color(0xFF66BB6A),
                            start = Offset(centerX, barY - 16),
                            end = Offset(centerX, barY + 16),
                            strokeWidth = 3f,
                        )

                        // Indicator
                        if (detectedNote != "--") {
                            val centsNorm = (detectedCents / 50f).coerceIn(-1f, 1f)
                            val indicatorX = centerX + centsNorm * (barWidth / 2)
                            val indicatorColor = when {
                                abs(detectedCents) <= 5 -> Color(0xFF66BB6A)
                                abs(detectedCents) <= 15 -> Color(0xFFFFA726)
                                else -> Color(0xFFEF5350)
                            }
                            drawCircle(
                                color = indicatorColor,
                                radius = 12f,
                                center = Offset(indicatorX, barY),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("-50¢", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("0", style = MaterialTheme.typography.labelSmall, color = Color(0xFF66BB6A))
                        Text("+50¢", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Start/stop button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isListening) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                    )
                    .clickable {
                        isListening = !isListening
                        if (isListening) {
                            startListening()
                        } else {
                            job?.cancel()
                            detectedFreq = 0f
                            detectedNote = "--"
                            detectedCents = 0f
                        }
                    }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = null,
                    tint = Color.White,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isListening) "Stop Listening" else "Start Listening",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // Reference tones card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Reference Tones",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf("C4", "D4", "E4", "F4", "G4", "A4", "B4").forEach { noteLabel ->
                            val noteName = noteLabel.dropLast(1)
                            val octave = noteLabel.last().digitToInt()
                            FilterChip(
                                selected = false,
                                onClick = { playReferenceTone(noteToFrequency(noteName, octave)) },
                                label = { Text(noteLabel) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun autocorrelationPitch(samples: FloatArray, sampleRate: Int): Float {
    val n = samples.size
    // Search for pitch between ~50Hz and ~2000Hz
    val minLag = sampleRate / 2000
    val maxLag = sampleRate / 50

    var bestCorrelation = 0f
    var bestLag = 0

    for (lag in minLag..maxLag.coerceAtMost(n / 2)) {
        var correlation = 0f
        var norm1 = 0f
        var norm2 = 0f
        val len = n - lag
        for (i in 0 until len) {
            correlation += samples[i] * samples[i + lag]
            norm1 += samples[i] * samples[i]
            norm2 += samples[i + lag] * samples[i + lag]
        }
        val normFactor = sqrt(norm1 * norm2)
        if (normFactor > 0) correlation /= normFactor

        if (correlation > bestCorrelation) {
            bestCorrelation = correlation
            bestLag = lag
        }
    }

    return if (bestCorrelation > 0.5f && bestLag > 0) {
        sampleRate.toFloat() / bestLag
    } else 0f
}

private fun playReferenceTone(freq: Float) {
    Thread {
        try {
            val sampleRate = 44100
            val durationMs = 1000
            val numSamples = sampleRate * durationMs / 1000
            val buffer = ShortArray(numSamples)

            for (i in buffer.indices) {
                val t = i.toDouble() / sampleRate
                val envelope = if (i < numSamples / 10) i.toDouble() / (numSamples / 10)
                else if (i > numSamples * 9 / 10) (numSamples - i).toDouble() / (numSamples / 10)
                else 1.0
                buffer[i] = (sin(2.0 * Math.PI * freq * t) * envelope * 16000).toInt()
                    .coerceIn(-32767, 32767).toShort()
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            Thread.sleep(durationMs.toLong() + 50)
            track.release()
        } catch (_: Exception) {}
    }.start()
}
