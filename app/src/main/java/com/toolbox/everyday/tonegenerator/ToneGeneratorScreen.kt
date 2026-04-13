package com.toolbox.everyday.tonegenerator

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

private enum class Waveform(val label: String) {
    Sine("Sine"), Square("Square"), Triangle("Triangle"), Sawtooth("Sawtooth"),
}

private data class Preset(val name: String, val frequency: Float)

private val presets = listOf(
    Preset("C4", 261.63f),
    Preset("D4", 293.66f),
    Preset("E4", 329.63f),
    Preset("F4", 349.23f),
    Preset("G4", 392.00f),
    Preset("A4 (440)", 440.00f),
    Preset("B4", 493.88f),
    Preset("18 kHz", 18000f),
)

private const val SAMPLE_RATE = 44100
private const val MIN_FREQ = 20f
private const val MAX_FREQ = 20000f

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToneGeneratorScreen() {
    var frequency by rememberSaveable { mutableFloatStateOf(440f) }
    var freqInputText by rememberSaveable { mutableStateOf("440") }
    var waveform by rememberSaveable { mutableStateOf(Waveform.Sine.name) }
    var volume by rememberSaveable { mutableFloatStateOf(0.7f) }
    var isPlaying by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Log scale for slider: map [0..1] → [log2(20)..log2(20000)]
    val logMin = log2(MIN_FREQ)
    val logMax = log2(MAX_FREQ)
    val sliderPosition = (log2(frequency) - logMin) / (logMax - logMin)

    // Audio engine
    val audioTrack = remember { mutableStateOf<AudioTrack?>(null) }
    val currentFreq = remember { mutableStateOf(440f) }
    val currentWaveform = remember { mutableStateOf(Waveform.Sine) }
    val currentVolume = remember { mutableFloatStateOf(0.7f) }

    fun startPlayback() {
        stopPlayback(audioTrack.value)
        currentFreq.value = frequency
        currentWaveform.value = Waveform.valueOf(waveform)
        currentVolume.floatValue = volume

        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(bufferSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        track.play()
        audioTrack.value = track

        Thread {
            val buffer = ShortArray(bufferSize / 2)
            var phase = 0.0

            while (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                val freq = currentFreq.value.toDouble()
                val wf = currentWaveform.value
                val vol = currentVolume.floatValue

                for (i in buffer.indices) {
                    val phaseIncrement = freq / SAMPLE_RATE
                    phase += phaseIncrement
                    if (phase >= 1.0) phase -= 1.0

                    val sample = when (wf) {
                        Waveform.Sine -> sin(2.0 * PI * phase)
                        Waveform.Square -> if (phase < 0.5) 1.0 else -1.0
                        Waveform.Triangle -> 4.0 * abs(phase - 0.5) - 1.0
                        Waveform.Sawtooth -> 2.0 * phase - 1.0
                    }
                    buffer[i] = (sample * vol * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                track.write(buffer, 0, buffer.size)
            }
        }.start()
    }

    // Update running params without restart
    fun updateParams() {
        currentFreq.value = frequency
        currentWaveform.value = Waveform.valueOf(waveform)
        currentVolume.floatValue = volume
    }

    DisposableEffect(Unit) {
        onDispose {
            stopPlayback(audioTrack.value)
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
        // Frequency display
        Text(
            "${"%.1f".format(frequency)} Hz",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        // Frequency slider (logarithmic)
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = sliderPosition,
                onValueChange = { pos ->
                    val newFreq = 2f.pow(logMin + pos * (logMax - logMin))
                    frequency = newFreq.coerceIn(MIN_FREQ, MAX_FREQ)
                    freqInputText = frequency.roundToInt().toString()
                },
                onValueChangeFinished = {
                    if (isPlaying) updateParams()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("20 Hz", style = MaterialTheme.typography.labelSmall)
                Text("20 kHz", style = MaterialTheme.typography.labelSmall)
            }
        }

        // Numeric input
        OutlinedTextField(
            value = freqInputText,
            onValueChange = { text ->
                freqInputText = text
                text.toFloatOrNull()?.let { f ->
                    if (f in MIN_FREQ..MAX_FREQ) {
                        frequency = f
                    }
                }
            },
            label = { Text("Frequency (Hz)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (isPlaying) updateParams()
                },
            ),
            singleLine = true,
        )

        // Presets
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Presets", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    presets.forEach { preset ->
                        FilledTonalButton(
                            onClick = {
                                frequency = preset.frequency
                                freqInputText = preset.frequency.roundToInt().toString()
                                if (isPlaying) updateParams()
                            },
                        ) {
                            Text(preset.name, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Waveform selector
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Waveform.entries.forEach { wf ->
                FilterChip(
                    selected = waveform == wf.name,
                    onClick = {
                        waveform = wf.name
                        if (isPlaying) updateParams()
                    },
                    label = { Text(wf.label) },
                )
            }
        }

        // Volume slider
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Volume: ${(volume * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = volume,
                onValueChange = { volume = it },
                onValueChangeFinished = {
                    if (isPlaying) updateParams()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Waveform preview
        WaveformPreview(
            waveform = Waveform.valueOf(waveform),
            color = MaterialTheme.colorScheme.primary,
        )

        // Play/Stop button
        LargeFloatingActionButton(
            onClick = {
                if (isPlaying) {
                    stopPlayback(audioTrack.value)
                    isPlaying = false
                } else {
                    startPlayback()
                    isPlaying = true
                }
            },
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Stop" else "Play",
                modifier = Modifier.padding(8.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun WaveformPreview(waveform: Waveform, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(16.dp),
        ) {
            val w = size.width
            val h = size.height
            val midY = h / 2
            val path = Path()
            val steps = 200

            for (i in 0..steps) {
                val x = w * i / steps
                val phase = (i.toDouble() / steps) * 2.0 // 2 cycles

                val y = when (waveform) {
                    Waveform.Sine -> sin(2.0 * PI * phase)
                    Waveform.Square -> if ((phase % 1.0) < 0.5) 1.0 else -1.0
                    Waveform.Triangle -> 4.0 * abs((phase % 1.0) - 0.5) - 1.0
                    Waveform.Sawtooth -> 2.0 * (phase % 1.0) - 1.0
                }

                val py = (midY - y.toFloat() * midY * 0.8f)
                if (i == 0) path.moveTo(x, py) else path.lineTo(x, py)
            }

            drawPath(path, color, style = Stroke(width = 3f))
            // Center line
            drawLine(
                color.copy(alpha = 0.3f),
                start = Offset(0f, midY),
                end = Offset(w, midY),
                strokeWidth = 1f,
            )
        }
    }
}

private fun stopPlayback(track: AudioTrack?) {
    try {
        track?.stop()
        track?.release()
    } catch (_: Exception) { }
}
