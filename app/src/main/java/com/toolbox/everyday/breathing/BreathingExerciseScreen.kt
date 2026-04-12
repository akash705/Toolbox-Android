package com.toolbox.everyday.breathing

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

        Spacer(modifier = Modifier.height(16.dp))

        // Breathing circle animation
        BreathingCircle(state = state)

        Spacer(modifier = Modifier.height(16.dp))

        // Cycle counter text
        if (state.isRunning || state.isComplete) {
            Text(
                text = if (state.isComplete) "COMPLETE!" else "CYCLE ${state.currentCycle} OF ${state.totalCycles}",
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 1.sp,
                color = if (state.isComplete) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Cycle dot indicators
            if (!state.isComplete) {
                Spacer(modifier = Modifier.height(8.dp))
                CycleDots(currentCycle = state.currentCycle, totalCycles = state.totalCycles)
            }

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Repeat,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Cycles", style = MaterialTheme.typography.bodyMedium)
                    }
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

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )

                // Sound and Haptic toggles side by side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    // Sound toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sound", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = state.soundEnabled,
                            onCheckedChange = { viewModel.toggleSound() },
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Haptic toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            Icons.Outlined.Vibration,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Haptic", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = state.hapticEnabled,
                            onCheckedChange = { viewModel.toggleHaptic() },
                        )
                    }
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
        BreathingPhase.Exhale -> 0.6f
        BreathingPhase.HoldOut -> 0.6f
        BreathingPhase.Idle -> 0.75f
    }

    val animDuration = when (state.phase) {
        BreathingPhase.Inhale -> state.phaseDuration * 1000
        BreathingPhase.Exhale -> state.phaseDuration * 1000
        else -> 300
    }

    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = animDuration, easing = EaseInOutCubic),
        label = "breathingScale",
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val phaseLabel = state.phase.label
    val countdown = if (state.isRunning) "${state.secondsRemaining}s" else ""

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val maxRadius = size.minDimension / 2f
            val circleRadius = maxRadius * scale
            val center = Offset(size.width / 2f, size.height / 2f)

            // Solid filled circle
            drawCircle(
                color = primaryColor,
                radius = circleRadius,
                center = center,
            )
        }

        // Phase text overlay
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = phaseLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White,
            )
            if (countdown.isNotEmpty()) {
                Text(
                    text = countdown,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun CycleDots(currentCycle: Int, totalCycles: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val activeColor = MaterialTheme.colorScheme.primary
        val inactiveColor = MaterialTheme.colorScheme.outlineVariant

        for (i in 1..totalCycles) {
            Canvas(modifier = Modifier.size(8.dp)) {
                drawCircle(
                    color = if (i <= currentCycle) activeColor else inactiveColor,
                    radius = size.minDimension / 2f,
                )
            }
        }
    }
}
