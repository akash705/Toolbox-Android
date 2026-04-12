package com.toolbox.motion.pedometer

import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.core.permission.PermissionGate
import com.toolbox.core.sensor.rememberStepCount
import kotlinx.coroutines.delay
import java.text.NumberFormat
import com.toolbox.core.sharing.ShareButton

@Composable
fun PedometerScreen() {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(SensorManager::class.java) }
    val hasSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null }

    if (!hasSensor) {
        SensorNotAvailable()
        return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        PermissionGate(
            permission = android.Manifest.permission.ACTIVITY_RECOGNITION,
            rationale = "The pedometer needs activity recognition permission to count your steps.",
        ) {
            PedometerContent()
        }
    } else {
        PedometerContent()
    }
}

@Composable
private fun PedometerContent() {
    val totalSteps by rememberStepCount()

    // Track baseline for daily steps (resets when screen opens)
    var baseline by rememberSaveable { mutableIntStateOf(-1) }
    val dailySteps = if (baseline < 0 && totalSteps > 0) {
        baseline = totalSteps
        0
    } else if (baseline >= 0) {
        (totalSteps - baseline).coerceAtLeast(0)
    } else {
        0
    }

    // Daily goal
    var dailyGoal by rememberSaveable { mutableIntStateOf(10000) }
    val goalProgress = if (dailyGoal > 0) (dailySteps.toFloat() / dailyGoal).coerceIn(0f, 1f) else 0f
    val stepsLeft = (dailyGoal - dailySteps).coerceAtLeast(0)

    // Session tracking
    var sessionActive by rememberSaveable { mutableStateOf(false) }
    var sessionStartSteps by rememberSaveable { mutableIntStateOf(0) }
    var sessionElapsedMs by rememberSaveable { mutableLongStateOf(0L) }
    var sessionStartTimeMs by rememberSaveable { mutableLongStateOf(0L) }
    val sessionSteps = if (sessionActive) (totalSteps - sessionStartSteps).coerceAtLeast(0) else 0

    // Timer tick for session duration
    LaunchedEffect(sessionActive) {
        if (sessionActive) {
            sessionStartTimeMs = System.currentTimeMillis() - sessionElapsedMs
            while (true) {
                sessionElapsedMs = System.currentTimeMillis() - sessionStartTimeMs
                delay(1000)
            }
        }
    }

    // Derived metrics
    val strideLength = 0.762 // meters (average stride)
    val distanceKm = dailySteps * strideLength / 1000.0
    val caloriesBurned = (dailySteps * 0.04).toInt() // rough estimate
    val activeMinutes = if (dailySteps > 0) (dailySteps / 100).coerceAtLeast(1) else 0

    val sessionDurationFormatted = formatDuration(sessionElapsedMs)
    val numberFormat = remember { NumberFormat.getNumberInstance() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Circular progress ring with step count
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(200.dp),
        ) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 14.dp.toPx()
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val arcOffset = Offset(strokeWidth / 2, strokeWidth / 2)

                // Background track
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = arcOffset,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
                // Progress arc
                drawArc(
                    color = primaryColor,
                    startAngle = -90f,
                    sweepAngle = 360f * goalProgress,
                    useCenter = false,
                    topLeft = arcOffset,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = numberFormat.format(dailySteps),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "STEPS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.5.sp,
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Stats chips: distance, calories, time
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            StatChip(
                icon = Icons.Default.DirectionsWalk,
                value = "%.1f km".format(distanceKm),
            )
            StatChip(
                icon = Icons.Default.LocalFireDepartment,
                value = "$caloriesBurned cal",
            )
            StatChip(
                icon = Icons.Default.Timer,
                value = "$activeMinutes min",
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Daily Goal card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "DAILY GOAL",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${numberFormat.format(dailyGoal)} steps",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    TextButton(onClick = {
                        dailyGoal = when (dailyGoal) {
                            5000 -> 10000
                            10000 -> 15000
                            15000 -> 20000
                            else -> 5000
                        }
                    }) {
                        Text("Adjust Goal")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { goalProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round,
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${(goalProgress * 100).toInt()}% completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${numberFormat.format(stepsLeft)} left",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Session card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "SESSION",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp,
                    )
                    FilledTonalButton(
                        onClick = {
                            if (sessionActive) {
                                sessionActive = false
                            } else {
                                sessionActive = true
                                sessionStartSteps = totalSteps
                                sessionElapsedMs = 0L
                            }
                        },
                    ) {
                        Icon(
                            imageVector = if (sessionActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (sessionActive) "Stop" else "Start")
                    }
                }

                if (sessionActive) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Duration box
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "DURATION",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 0.5.sp,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = sessionDurationFormatted,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        // Session steps box
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "SESSION STEPS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 0.5.sp,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = numberFormat.format(sessionSteps),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            sessionActive = false
                            sessionElapsedMs = 0L
                            sessionStartSteps = totalSteps
                        },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text("Reset Session")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ShareButton(
            toolName = "Pedometer",
            value = NumberFormat.getNumberInstance().format(dailySteps),
            unit = "steps",
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Footer
        Text(
            text = "Step counter active while screen is open",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun StatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SensorNotAvailable() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.DirectionsWalk,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Step Counter Sensor Not Available",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "This device does not have a step counter sensor.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}
