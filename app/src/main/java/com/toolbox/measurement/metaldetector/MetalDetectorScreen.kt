package com.toolbox.measurement.metaldetector

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.core.sensor.rememberMagnetometerData
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun MetalDetectorScreen() {
    val magState = rememberMagnetometerData()
    val haptic = LocalHapticFeedback.current
    val mag = magState.value

    val x = mag[0]
    val y = mag[1]
    val z = mag[2]
    val totalMagnitude = sqrt(x * x + y * y + z * z)

    var peakMagnitude by remember { mutableFloatStateOf(0f) }
    var isRunning by remember { mutableStateOf(true) }

    if (isRunning && totalMagnitude > peakMagnitude) {
        peakMagnitude = totalMagnitude
    }

    val animatedMagnitude by animateFloatAsState(
        targetValue = if (isRunning) totalMagnitude else totalMagnitude,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "magnitude",
    )

    // Map µT to gauge sweep (0-200 µT -> 0-360 degrees)
    val maxField = 200f
    val gaugeSweep = (animatedMagnitude / maxField * 360f).coerceIn(0f, 360f)

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val signalLevel = getSignalLevel(totalMagnitude)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Circular gauge with µT in center
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(260.dp),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawMetalGauge(
                    sweepAngle = gaugeSweep,
                    primaryColor = primaryColor,
                    trackColor = trackColor,
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "%.0f".format(animatedMagnitude),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                )
                Text(
                    text = "μT",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Signal strength card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "SIGNAL STRENGTH",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = signalLevel.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = signalLevel.color,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (totalMagnitude / maxField).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp),
                    color = signalLevel.color,
                    trackColor = signalLevel.color.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Current and Peak cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MeasurementCard(
                label = "Current",
                value = "%.1f μT".format(totalMagnitude),
                modifier = Modifier.weight(1f),
                isPrimary = true,
            )
            MeasurementCard(
                label = "Peak",
                value = if (peakMagnitude == 0f) "-- μT" else "%.1f μT".format(peakMagnitude),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isRunning = !isRunning
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isRunning) "Stop" else "Start")
            }

            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    peakMagnitude = 0f
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
            ) {
                Icon(
                    Icons.Default.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MeasurementCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isPrimary) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isPrimary) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (isPrimary) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

private fun DrawScope.drawMetalGauge(
    sweepAngle: Float,
    primaryColor: Color,
    trackColor: Color,
) {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = size.minDimension / 2 - 24f
    val strokeWidth = 14f

    // Track ring
    drawCircle(
        color = trackColor,
        radius = radius,
        center = center,
        style = Stroke(width = strokeWidth),
    )

    // Active arc
    if (sweepAngle > 0.5f) {
        drawArc(
            color = primaryColor,
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }

    // Tick marks every 30 degrees
    for (deg in 0 until 360 step 30) {
        val rad = Math.toRadians(deg.toDouble() - 90.0)
        val isMajor = deg % 90 == 0
        val outerR = radius + if (isMajor) 8f else 4f
        val innerR = radius - if (isMajor) 8f else 4f
        drawLine(
            color = primaryColor.copy(alpha = if (isMajor) 0.5f else 0.25f),
            start = Offset(
                center.x + outerR * cos(rad).toFloat(),
                center.y + outerR * sin(rad).toFloat(),
            ),
            end = Offset(
                center.x + innerR * cos(rad).toFloat(),
                center.y + innerR * sin(rad).toFloat(),
            ),
            strokeWidth = if (isMajor) 2f else 1.5f,
        )
    }

    // Needle
    val needleRad = Math.toRadians(sweepAngle.toDouble() - 90.0)
    val needleInner = radius - strokeWidth
    val needleOuter = radius + strokeWidth
    drawLine(
        color = primaryColor,
        start = Offset(
            center.x + needleInner * cos(needleRad).toFloat(),
            center.y + needleInner * sin(needleRad).toFloat(),
        ),
        end = Offset(
            center.x + needleOuter * cos(needleRad).toFloat(),
            center.y + needleOuter * sin(needleRad).toFloat(),
        ),
        strokeWidth = 3f,
        cap = StrokeCap.Round,
    )

    // Center dot
    drawCircle(
        color = primaryColor,
        radius = 4f,
        center = center,
    )
}

private data class SignalLevel(val label: String, val color: Color)

private fun getSignalLevel(magnitude: Float): SignalLevel = when {
    magnitude < 65 -> SignalLevel("Normal", Color(0xFF4CAF50))
    magnitude < 100 -> SignalLevel("Elevated", Color(0xFFFFC107))
    magnitude < 150 -> SignalLevel("High", Color(0xFFFF9800))
    else -> SignalLevel("Very High", Color(0xFFF44336))
}
