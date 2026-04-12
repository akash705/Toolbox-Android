package com.toolbox.measurement.metaldetector

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.core.sensor.rememberMagnetometerData
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import com.toolbox.core.sharing.ShareButton

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

    val maxField = 200f
    val gaugeSweep = (animatedMagnitude / maxField * 270f).coerceIn(0f, 270f)

    val gaugeColor = Color(0xFF0277BD)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val signalLevel = getSignalLevel(totalMagnitude)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Circular gauge
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(220.dp),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawMetalGauge(
                    sweepAngle = gaugeSweep,
                    primaryColor = gaugeColor,
                    trackColor = trackColor,
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "%.1f".format(animatedMagnitude),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "μT",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            text = "LIVE INTENSITY",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Signal Strength section
        Column(modifier = Modifier.fillMaxWidth()) {
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
                    letterSpacing = 0.5.sp,
                )
                Text(
                    text = signalLevel.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .background(
                            color = signalLevel.color,
                            shape = RoundedCornerShape(4.dp),
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Multi-segment signal bar
            val progress = (totalMagnitude / maxField).coerceIn(0f, 1f)
            SegmentedSignalBar(progress = progress)

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "None",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
                Text(
                    text = "Strong",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
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
                icon = Icons.Default.Bolt,
                label = "CURRENT",
                value = "%.1f".format(totalMagnitude),
                subtitle = "μT Reading",
                modifier = Modifier.weight(1f),
                isPrimary = true,
            )
            MeasurementCard(
                icon = Icons.Default.BarChart,
                label = "PEAK",
                value = if (peakMagnitude == 0f) "--" else "%.1f".format(peakMagnitude),
                subtitle = "Max Detected",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // System Metadata section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SYSTEM METADATA",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))
                MetadataRow("Sensor Accuracy", "High (±0.1 μT)")
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
                MetadataRow("X-Axis", "%.1f".format(x))
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
                MetadataRow("Y-Axis", "%.1f".format(y))
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
                MetadataRow("Z-Axis", "%.1f".format(z))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isRunning = !isRunning
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0D2137),
                ),
                shape = RoundedCornerShape(26.dp),
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isRunning) "Stop Detection" else "Start Detection")
            }
            ShareButton(
                toolName = "Metal Detector",
                value = "%.1f".format(totalMagnitude),
                unit = "μT",
                label = signalLevel.label,
                modifier = Modifier.height(52.dp),
            )
        }

        TextButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                peakMagnitude = 0f
            },
        ) {
            Icon(
                Icons.Default.RestartAlt,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reset Readings")
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SegmentedSignalBar(progress: Float) {
    val greenColor = Color(0xFF4CAF50)
    val yellowColor = Color(0xFFFFC107)
    val orangeColor = Color(0xFFFF9800)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        val greenEnd = 0.33f
        val yellowEnd = 0.66f

        // Green segment
        if (progress > 0f) {
            Box(
                modifier = Modifier
                    .weight(progress.coerceAtMost(greenEnd) / greenEnd * 0.33f + 0.001f)
                    .height(18.dp)
                    .background(greenColor),
            )
        }
        // Yellow segment
        if (progress > greenEnd) {
            Box(
                modifier = Modifier
                    .weight(
                        ((progress - greenEnd).coerceAtMost(yellowEnd - greenEnd) /
                            (yellowEnd - greenEnd) * 0.33f) + 0.001f,
                    )
                    .height(18.dp)
                    .background(yellowColor),
            )
        }
        // Orange segment
        if (progress > yellowEnd) {
            Box(
                modifier = Modifier
                    .weight(
                        ((progress - yellowEnd).coerceAtMost(1f - yellowEnd) /
                            (1f - yellowEnd) * 0.34f) + 0.001f,
                    )
                    .height(18.dp)
                    .background(orangeColor),
            )
        }
        // Remaining track
        if (progress < 1f) {
            Spacer(modifier = Modifier.weight((1f - progress).coerceAtLeast(0.001f)))
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun MeasurementCard(
    icon: ImageVector,
    label: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
) {
    val cardColor = if (isPrimary) Color(0xFF0277BD) else MaterialTheme.colorScheme.surfaceContainerLow
    val contentColor = if (isPrimary) Color.White else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = contentColor.copy(alpha = 0.8f),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.8f),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.6f),
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
    val radius = size.minDimension / 2 - 20f
    val strokeWidth = 12f

    // Track arc (270 degrees, open at bottom)
    drawArc(
        color = trackColor,
        startAngle = 135f,
        sweepAngle = 270f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
    )

    // Active arc
    if (sweepAngle > 0.5f) {
        drawArc(
            color = primaryColor,
            startAngle = 135f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }

    // Tick marks along the 270-degree arc
    for (i in 0..18) {
        val deg = 135.0 + (i * 15.0)
        val rad = Math.toRadians(deg)
        val isMajor = i % 3 == 0
        val outerR = radius + if (isMajor) 6f else 3f
        val innerR = radius - if (isMajor) 6f else 3f
        drawLine(
            color = primaryColor.copy(alpha = if (isMajor) 0.4f else 0.2f),
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

    // Needle indicator at the end of active arc
    val needleRad = Math.toRadians(135.0 + sweepAngle)
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
}

private data class SignalLevel(val label: String, val color: Color)

private fun getSignalLevel(magnitude: Float): SignalLevel = when {
    magnitude < 65 -> SignalLevel("Normal", Color(0xFF4CAF50))
    magnitude < 100 -> SignalLevel("Elevated", Color(0xFFFFC107))
    magnitude < 150 -> SignalLevel("High", Color(0xFFFF9800))
    else -> SignalLevel("Very High", Color(0xFFF44336))
}
