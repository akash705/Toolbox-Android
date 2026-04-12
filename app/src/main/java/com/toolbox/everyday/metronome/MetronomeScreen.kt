package com.toolbox.everyday.metronome

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.VibrationEffect
import android.os.VibratorManager
import android.os.Vibrator
import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class TimeSignature(val label: String, val beats: Int) {
    TwoFour("2/4", 2),
    ThreeFour("3/4", 3),
    FourFour("4/4", 4),
    SixEight("6/8", 6),
}

@Composable
fun MetronomeScreen() {
    var bpm by rememberSaveable { mutableIntStateOf(120) }
    var isPlaying by rememberSaveable { mutableStateOf(false) }
    var timeSig by rememberSaveable { mutableStateOf(TimeSignature.FourFour.name) }
    var hapticEnabled by rememberSaveable { mutableStateOf(true) }
    var currentBeat by remember { mutableIntStateOf(0) }

    // Tap tempo state
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var tapIntervals = remember { mutableListOf<Long>() }

    val timeSignature = TimeSignature.valueOf(timeSig)
    val context = LocalContext.current

    // Metronome tick loop
    LaunchedEffect(isPlaying, bpm) {
        if (!isPlaying) {
            currentBeat = 0
            return@LaunchedEffect
        }
        val intervalMs = (60_000.0 / bpm).toLong()
        currentBeat = 0
        while (isPlaying) {
            val isAccent = currentBeat == 0
            // Play click sound
            playClick(isAccent)
            // Haptic
            if (hapticEnabled) {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(
                        if (isAccent) 30 else 15,
                        if (isAccent) 255 else 128,
                    )
                )
            }
            currentBeat = (currentBeat + 1) % timeSignature.beats
            delay(intervalMs)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // BPM display
        Text(
            text = "$bpm",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "BPM",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Beat indicator dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (i in 0 until timeSignature.beats) {
                val isActive = isPlaying && currentBeat == i
                val isFirst = i == 0
                Box(
                    modifier = Modifier
                        .size(if (isFirst) 24.dp else 20.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isActive && isFirst -> MaterialTheme.colorScheme.primary
                                isActive -> MaterialTheme.colorScheme.tertiary
                                isFirst -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Play/pause button
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable { isPlaying = !isPlaying },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Stop" else "Start",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(40.dp),
            )
        }

        // BPM controls card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Tempo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    IconButton(onClick = { if (bpm > 20) bpm-- }) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease BPM")
                    }
                    Slider(
                        value = bpm.toFloat(),
                        onValueChange = { bpm = it.roundToInt() },
                        valueRange = 20f..300f,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { if (bpm < 300) bpm++ }) {
                        Icon(Icons.Default.Add, contentDescription = "Increase BPM")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tap tempo button
                OutlinedButton(
                    onClick = {
                        val now = System.currentTimeMillis()
                        if (lastTapTime > 0) {
                            val interval = now - lastTapTime
                            if (interval < 2000) {
                                tapIntervals.add(interval)
                                if (tapIntervals.size > 5) tapIntervals.removeAt(0)
                                if (tapIntervals.size >= 2) {
                                    val avgInterval = tapIntervals.average()
                                    bpm = (60_000.0 / avgInterval).roundToInt().coerceIn(20, 300)
                                }
                            } else {
                                tapIntervals.clear()
                            }
                        }
                        lastTapTime = now
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.TouchApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tap Tempo")
                }
            }
        }

        // Time signature & options card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Time Signature",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeSignature.entries.forEach { ts ->
                        FilterChip(
                            selected = ts.name == timeSig,
                            onClick = { timeSig = ts.name },
                            label = { Text(ts.label) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Haptic feedback", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = hapticEnabled,
                        onCheckedChange = { hapticEnabled = it },
                    )
                }
            }
        }
    }
}

private fun playClick(accent: Boolean) {
    Thread {
        try {
            val sampleRate = 44100
            val durationMs = if (accent) 30 else 20
            val numSamples = sampleRate * durationMs / 1000
            val buffer = ShortArray(numSamples)
            val freq = if (accent) 1200.0 else 800.0

            for (i in buffer.indices) {
                val t = i.toDouble() / sampleRate
                val envelope = 1.0 - (i.toDouble() / numSamples) // linear decay
                val sample = sin(2.0 * Math.PI * freq * t) * envelope * 20000
                buffer[i] = sample.toInt().coerceIn(-32767, 32767).toShort()
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
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

            Thread.sleep(durationMs.toLong() + 10)
            track.release()
        } catch (_: Exception) {}
    }.start()
}
