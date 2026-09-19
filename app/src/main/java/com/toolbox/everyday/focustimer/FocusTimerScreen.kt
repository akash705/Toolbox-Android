package com.toolbox.everyday.focustimer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.toolbox.core.audio.NoiseEngine
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val FaceColor = Color(0xFF17171B)
private val OrangeAccent = Color(0xFFFF7A18)
private val DimTick = Color(0xFF3A3A42)

@Composable
fun FocusTimerScreen(viewModel: FocusTimerViewModel = viewModel()) {
    val context = LocalContext.current
    val active by viewModel.active.collectAsState()
    val running by viewModel.running.collectAsState()
    val phase by viewModel.phase.collectAsState()
    val remaining by viewModel.remainingMs.collectAsState()
    val completed by viewModel.completedWorkSessions.collectAsState()
    val config by viewModel.config.collectAsState()
    val interrupted by viewModel.interrupted.collectAsState()

    var ambientEnabled by remember { mutableStateOf(false) }
    val noiseEngine = remember { NoiseEngine() }
    DisposableEffect(ambientEnabled, active, running, phase) {
        if (ambientEnabled && active && running && phase == FocusPhase.Work) noiseEngine.start()
        else noiseEngine.stop()
        onDispose { noiseEngine.stop() }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.start() }

    fun startWithPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) viewModel.start()
            else notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.start()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (interrupted) {
            InterruptedBanner(onDismiss = viewModel::dismissInterrupted)
        }

        if (active) {
            Spacer(Modifier.height(8.dp))
            PhasePill(phase)
            val totalMs = config.durationMsFor(phase).coerceAtLeast(1L)
            val progress = (remaining.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
            TimerGauge(progress = progress, timeText = formatMs(remaining), running = running)
            Text(
                "Completed today: $completed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (running) {
                    Button(
                        onClick = viewModel::pause,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                    ) { Text("Pause") }
                } else {
                    Button(
                        onClick = viewModel::resume,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                    ) { Text("Resume") }
                }
                FilledTonalButton(onClick = viewModel::skip, shape = RoundedCornerShape(50)) { Text("Skip") }
                OutlinedButton(onClick = viewModel::reset, shape = RoundedCornerShape(50)) { Text("Reset") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ambient sound (while open)", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(8.dp))
                Switch(checked = ambientEnabled, onCheckedChange = { ambientEnabled = it })
            }
        } else {
            Spacer(Modifier.height(8.dp))
            Text("Quick start", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(15, 25, 50).forEach { minutes ->
                    PresetChip(
                        minutes = minutes,
                        selected = config.workMin == minutes,
                        onClick = { viewModel.setConfig { it.copy(workMin = minutes) } },
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            stepperRow("Focus (min)", config.workMin) { v -> viewModel.setConfig { it.copy(workMin = v) } }
            stepperRow("Short break (min)", config.shortBreakMin) { v -> viewModel.setConfig { it.copy(shortBreakMin = v) } }
            stepperRow("Long break (min)", config.longBreakMin) { v -> viewModel.setConfig { it.copy(longBreakMin = v) } }
            stepperRow("Sessions before long break", config.sessionsBeforeLongBreak) { v -> viewModel.setConfig { it.copy(sessionsBeforeLongBreak = v) } }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { startWithPermission() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
            ) {
                Text("Start focus session", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun PhasePill(phase: FocusPhase) {
    Surface(
        shape = RoundedCornerShape(50),
        color = OrangeAccent.copy(alpha = 0.16f),
    ) {
        Text(
            text = phase.label.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = OrangeAccent,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun TimerGauge(progress: Float, timeText: String, running: Boolean) {
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600),
        label = "progress",
    )
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(280.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val outer = size.minDimension / 2f
            val tickLen = outer * 0.12f
            val inner = outer - tickLen
            val faceRadius = inner - outer * 0.06f

            // Dark circular face.
            drawCircle(color = FaceColor, radius = faceRadius, center = Offset(cx, cy))

            // Tick ring: lit (orange) ticks represent remaining time, from the top clockwise.
            val tickCount = 60
            val lit = (animated * tickCount).roundToInt()
            for (i in 0 until tickCount) {
                val angle = Math.toRadians((-90.0 + i * (360.0 / tickCount)))
                val c = cos(angle).toFloat()
                val s = sin(angle).toFloat()
                val start = Offset(cx + inner * c, cy + inner * s)
                val end = Offset(cx + outer * c, cy + outer * s)
                drawLine(
                    color = if (i < lit) OrangeAccent else DimTick,
                    start = start,
                    end = end,
                    strokeWidth = outer * 0.03f,
                    cap = StrokeCap.Round,
                )
            }
        }
        Text(
            text = timeText,
            color = Color.White,
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
        )
    }
}

@Composable
private fun PresetChip(minutes: Int, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (selected) OrangeAccent else MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.size(72.dp),
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$minutes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "min",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun InterruptedBanner(onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Your last focus session was interrupted.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

@Composable
private fun stepperRow(label: String, value: Int, onChange: (Int) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            IconButton(onClick = { onChange((value - 1).coerceAtLeast(1)) }) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease")
            }
            Text(value.toString(), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { onChange((value + 1).coerceAtMost(180)) }) {
                Icon(Icons.Default.Add, contentDescription = "Increase")
            }
        }
    }
}
