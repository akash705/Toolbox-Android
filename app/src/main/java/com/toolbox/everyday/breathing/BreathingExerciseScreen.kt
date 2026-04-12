package com.toolbox.everyday.breathing

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun BreathingExerciseScreen(
    viewModel: BreathingExerciseViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val haptic = LocalHapticFeedback.current

    // Haptic feedback on phase change
    LaunchedEffect(state.phase) {
        if (state.hapticEnabled && state.isRunning && state.phase != BreathingPhase.Idle) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Technique selector chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BreathingTechnique.entries.forEach { technique ->
                FilterChip(
                    selected = state.technique == technique,
                    onClick = { viewModel.setTechnique(technique) },
                    label = { Text(technique.label, style = MaterialTheme.typography.labelMedium) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Timing description
        val t = state.technique
        val timingText = buildString {
            append("${t.inhaleSec}s in")
            if (t.holdAfterInhaleSec > 0) append(" - ${t.holdAfterInhaleSec}s hold")
            append(" - ${t.exhaleSec}s out")
            if (t.holdAfterExhaleSec > 0) append(" - ${t.holdAfterExhaleSec}s hold")
        }
        Text(
            text = timingText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Breathing circle animation
        BreathingCircle(state = state)

        Spacer(modifier = Modifier.height(16.dp))

        // Cycle counter
        if (state.isRunning || state.isComplete) {
            Text(
                text = if (state.isComplete) "Complete!" else "Cycle ${state.currentCycle} of ${state.totalCycles}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.isComplete) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Play/Pause button
        FilledIconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.toggleRunning()
            },
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
        ) {
            Icon(
                imageVector = if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (state.isRunning) "Stop" else "Start",
                modifier = Modifier.size(32.dp),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Settings section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Cycles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Cycles", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.setCycles(state.totalCycles - 1) },
                            enabled = !state.isRunning,
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease cycles")
                        }
                        Text(
                            text = "${state.totalCycles}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.width(32.dp),
                            textAlign = TextAlign.Center,
                        )
                        IconButton(
                            onClick = { viewModel.setCycles(state.totalCycles + 1) },
                            enabled = !state.isRunning,
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase cycles")
                        }
                    }
                }

                // Sound toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Sound", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = state.soundEnabled,
                        onCheckedChange = { viewModel.toggleSound() },
                    )
                }

                // Haptic toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Haptic", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = state.hapticEnabled,
                        onCheckedChange = { viewModel.toggleHaptic() },
                    )
                }
            }
        }
    }
}

@Composable
private fun BreathingCircle(state: BreathingUiState) {
    // Animate scale based on phase
    val targetScale = when (state.phase) {
        BreathingPhase.Inhale -> 1f
        BreathingPhase.HoldIn -> 1f
        BreathingPhase.Exhale -> 0.5f
        BreathingPhase.HoldOut -> 0.5f
        BreathingPhase.Idle -> 0.7f
    }

    val animDuration = when (state.phase) {
        BreathingPhase.Inhale -> state.phaseDuration * 1000
        BreathingPhase.Exhale -> state.phaseDuration * 1000
        else -> 300
    }

    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = animDuration, easing = LinearEasing),
        label = "breathingScale",
    )

    // Progress arc (countdown within phase)
    val progress = if (state.phaseDuration > 0 && state.isRunning) {
        state.secondsRemaining.toFloat() / state.phaseDuration.toFloat()
    } else {
        0f
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val containerColor = MaterialTheme.colorScheme.primaryContainer
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val phaseLabel = state.phase.label
    val countdown = if (state.isRunning) "${state.secondsRemaining}" else ""
    val textColor = MaterialTheme.colorScheme.onPrimaryContainer

    Box(
        modifier = Modifier.size(220.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val circleRadius = (size.minDimension / 2f) * scale
            val center = Offset(size.width / 2f, size.height / 2f)

            // Filled circle
            drawCircle(
                color = containerColor,
                radius = circleRadius,
                center = center,
            )

            // Track ring
            drawCircle(
                color = trackColor,
                radius = circleRadius,
                center = center,
                style = Stroke(width = 6.dp.toPx()),
            )

            // Progress arc
            if (progress > 0f) {
                drawArc(
                    color = primaryColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(center.x - circleRadius, center.y - circleRadius),
                    size = androidx.compose.ui.geometry.Size(circleRadius * 2, circleRadius * 2),
                    style = Stroke(width = 6.dp.toPx()),
                )
            }
        }

        // Phase text overlay
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = phaseLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
            )
            if (countdown.isNotEmpty()) {
                Text(
                    text = countdown,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                )
            }
        }
    }
}
