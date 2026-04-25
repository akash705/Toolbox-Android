package com.toolbox.everyday.vibration

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class VibPattern(
    val id: String,
    val label: String,
    val description: String,
    /** Pairs: (offMs, onMs) starting with offMs at index 0 (Android API expects this). */
    val timings: LongArray,
)

private val Patterns = listOf(
    VibPattern(
        id = "tap",
        label = "Tap",
        description = "Single short pulse",
        timings = longArrayOf(0, 80),
    ),
    VibPattern(
        id = "double",
        label = "Double tap",
        description = "Two short pulses",
        timings = longArrayOf(0, 60, 80, 60),
    ),
    VibPattern(
        id = "heartbeat",
        label = "Heartbeat",
        description = "Lub-dub repeated three times",
        timings = longArrayOf(0, 120, 100, 220, 600, 120, 100, 220, 600, 120, 100, 220),
    ),
    VibPattern(
        id = "pulse",
        label = "Pulse",
        description = "Steady on/off rhythm",
        timings = longArrayOf(0, 200, 200, 200, 200, 200, 200, 200, 200, 200),
    ),
    VibPattern(
        id = "wave",
        label = "Wave",
        description = "Rising then falling pulses",
        timings = longArrayOf(0, 80, 120, 160, 120, 240, 120, 320, 120, 240, 120, 160, 120, 80),
    ),
    VibPattern(
        id = "sos",
        label = "SOS",
        description = "Morse: · · · — — — · · ·",
        timings = longArrayOf(
            0, 100, 100, 100, 100, 100, 250,
            300, 100, 300, 100, 300, 250,
            100, 100, 100, 100, 100,
        ),
    ),
)

@Composable
fun VibrationPatternsScreen() {
    val context = LocalContext.current
    val vibrator = remember { getVibrator(context) }
    var playingId by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose { vibrator?.cancel() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Vibration,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (vibrator?.hasVibrator() == true) "Tap a pattern to play" else "No vibrator on this device",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        Patterns.forEach { p ->
            PatternRow(
                pattern = p,
                isPlaying = playingId == p.id,
                onPlay = {
                    playingId = p.id
                    vibrator?.let { play(it, p.timings) }
                },
                onStop = {
                    vibrator?.cancel()
                    playingId = null
                },
                vibratorAvailable = vibrator?.hasVibrator() == true,
            )
        }

        if (vibrator?.hasVibrator() == true) {
            FilledTonalButton(
                onClick = { vibrator.cancel(); playingId = null },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Stop all")
            }
        }
    }
}

@Composable
private fun PatternRow(
    pattern: VibPattern,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    vibratorAvailable: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(pattern.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    pattern.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(
                onClick = if (isPlaying) onStop else onPlay,
                enabled = vibratorAvailable,
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Stop" else "Play",
                )
                Spacer(Modifier.width(6.dp))
                Text(if (isPlaying) "Stop" else "Play")
            }
        }
    }
}

private fun getVibrator(context: Context): Vibrator? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
}

private fun play(vibrator: Vibrator, timings: LongArray) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(timings, -1)
    }
}
