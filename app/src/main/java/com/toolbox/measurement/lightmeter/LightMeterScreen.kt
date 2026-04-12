package com.toolbox.measurement.lightmeter

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
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.core.sensor.rememberLightSensorData
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import com.toolbox.core.sharing.ShareButton

private const val CHART_SAMPLES = 300

@Composable
fun LightMeterScreen() {
    val luxState = rememberLightSensorData()
    val currentLux = luxState.value
    val haptic = LocalHapticFeedback.current

    var peakLux by remember { mutableFloatStateOf(0f) }
    val chartData = remember { FloatArray(CHART_SAMPLES) }
    var chartIndex by remember { mutableIntStateOf(0) }

    if (currentLux >= 0f) {
        if (currentLux > peakLux) peakLux = currentLux
        chartData[chartIndex % CHART_SAMPLES] = currentLux
        chartIndex++
    }

    val animatedLux by animateFloatAsState(
        targetValue = currentLux,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "lux",
    )

    // Map lux to gauge sweep using logarithmic scale (0-100k lux -> 0-360 degrees)
    val gaugeSweep = if (animatedLux > 0f) {
        (ln(animatedLux + 1f) / ln(100001f) * 360f).coerceIn(0f, 360f)
    } else 0f

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val lightCondition = getLightCondition(currentLux)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Circular gauge with lux in center
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(260.dp),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLightGauge(
                    sweepAngle = gaugeSweep,
                    primaryColor = primaryColor,
                    trackColor = trackColor,
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "%.0f".format(animatedLux),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                )
                Text(
                    text = "lux",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Light history chart card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
            ) {
                Text(
                    text = "LIGHT HISTORY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    drawLightChart(
                        chartData = chartData,
                        chartIndex = chartIndex,
                        totalSamples = CHART_SAMPLES,
                        lineColor = primaryColor,
                        gridColor = trackColor,
                    )
                }
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
                value = "%.0f lx".format(currentLux),
                modifier = Modifier.weight(1f),
                isPrimary = true,
            )
            MeasurementCard(
                label = "Peak",
                value = if (peakLux == 0f) "-- lx" else "%.0f lx".format(peakLux),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Light condition label
        Card(
            colors = CardDefaults.cardColors(
                containerColor = lightCondition.color.copy(alpha = 0.12f),
            ),
            shape = RoundedCornerShape(20.dp),
        ) {
            Text(
                text = lightCondition.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = lightCondition.color,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Reset and Share buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    peakLux = 0f
                    chartIndex = 0
                    chartData.fill(0f)
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
            ShareButton(
                toolName = "Light Meter",
                value = "${currentLux.toInt()}",
                unit = "lux",
                label = lightCondition.label,
                modifier = Modifier.height(56.dp),
            )
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

private fun DrawScope.drawLightGauge(
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

private fun DrawScope.drawLightChart(
    chartData: FloatArray,
    chartIndex: Int,
    totalSamples: Int,
    lineColor: Color,
    gridColor: Color,
) {
    val w = size.width
    val h = size.height

    // Grid lines
    for (i in 0..4) {
        val y = h * i / 4f
        drawLine(gridColor.copy(alpha = 0.3f), Offset(0f, y), Offset(w, y), strokeWidth = 0.5f)
    }

    val samplesAvailable = minOf(chartIndex, totalSamples)
    if (samplesAvailable > 1) {
        val startIdx = if (chartIndex >= totalSamples) chartIndex else 0
        val localMax = chartData.take(samplesAvailable).max().coerceAtLeast(100f)
        val path = Path()

        for (i in 0 until samplesAvailable) {
            val idx = (startIdx + i) % totalSamples
            val x = w * i / (totalSamples - 1).toFloat()
            val y = h - (chartData[idx] / localMax) * h * 0.9f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(path, lineColor, style = Stroke(width = 2f))
    }
}

private data class LightCondition(val label: String, val color: Color)

private fun getLightCondition(lux: Float): LightCondition = when {
    lux < 10 -> LightCondition("Dark", Color(0xFF5C6BC0))
    lux < 50 -> LightCondition("Dim room", Color(0xFF7E57C2))
    lux < 500 -> LightCondition("Indoor lighting", Color(0xFF4CAF50))
    lux < 10000 -> LightCondition("Bright light", Color(0xFFFF9800))
    else -> LightCondition("Direct sunlight", Color(0xFFF44336))
}
