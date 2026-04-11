package com.toolbox.measurement.humidity

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toolbox.core.sensor.rememberHumidityData
import com.toolbox.core.sensor.rememberTemperatureData
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HumidityScreen() {
    val humidityState = rememberHumidityData()
    val currentHumidity = humidityState.value

    if (currentHumidity == null) {
        SensorUnavailableContent()
        return
    }

    val temperatureState = rememberTemperatureData()
    val currentTemperature = temperatureState.value

    var minHumidity by remember { mutableFloatStateOf(Float.MAX_VALUE) }
    var maxHumidity by remember { mutableFloatStateOf(0f) }
    var sumHumidity by remember { mutableFloatStateOf(0f) }
    var sampleCount by remember { mutableFloatStateOf(0f) }

    // Update stats
    if (currentHumidity >= 0f) {
        if (currentHumidity < minHumidity) minHumidity = currentHumidity
        if (currentHumidity > maxHumidity) maxHumidity = currentHumidity
        sumHumidity += currentHumidity
        sampleCount++
    }

    val avgHumidity = if (sampleCount > 0) sumHumidity / sampleCount else 0f

    val animatedHumidity by animateFloatAsState(
        targetValue = currentHumidity,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "humidity",
    )

    val comfortLevel = getComfortLevel(currentHumidity)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Large humidity reading
        Text(
            text = "%.1f%%".format(animatedHumidity),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Comfort zone label badge
        Card(
            colors = CardDefaults.cardColors(
                containerColor = comfortLevel.color.copy(alpha = 0.12f),
            ),
            shape = RoundedCornerShape(20.dp),
        ) {
            Text(
                text = comfortLevel.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = comfortLevel.color,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Arc gauge
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
        ) {
            drawHumidityGauge(animatedHumidity)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Comfort Zones reference card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    text = "COMFORT ZONES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                ComfortZoneRow("Dry", "< 30%", Color(0xFFFF9800))
                ComfortZoneRow("Comfortable", "30% - 60%", Color(0xFF4CAF50))
                ComfortZoneRow("Humid", "> 60%", Color(0xFF2196F3))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatCard(
                "MIN",
                if (minHumidity == Float.MAX_VALUE) "--%" else "%.1f%%".format(minHumidity),
                Modifier.weight(1f),
            )
            StatCard(
                "AVG",
                if (sampleCount == 0f) "--%" else "%.1f%%".format(avgHumidity),
                Modifier.weight(1f),
            )
            StatCard(
                "MAX",
                if (maxHumidity == 0f) "--%" else "%.1f%%".format(maxHumidity),
                Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dew Point card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    text = "DEW POINT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (currentTemperature != null) {
                    val dewPoint = currentTemperature - ((100f - currentHumidity) / 5f)
                    Text(
                        text = "%.1f\u00B0C".format(dewPoint),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ambient temperature: %.1f\u00B0C".format(currentTemperature),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = "Temperature sensor unavailable",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Available on supported devices only",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SensorUnavailableContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Humidity Sensor Not Available",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This device does not have a relative humidity sensor. This feature is available on select Samsung and Pixel devices.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ComfortZoneRow(label: String, range: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Canvas(modifier = Modifier.size(10.dp)) {
                drawCircle(color = color)
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = range,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun DrawScope.drawHumidityGauge(humidity: Float) {
    val gaugeWidth = size.width
    val gaugeHeight = size.height
    val centerX = gaugeWidth / 2
    val centerY = gaugeHeight
    val radius = gaugeWidth / 2 - 20f

    val startAngle = 180f
    val totalSweep = 180f
    val arcTopLeft = Offset(centerX - radius, centerY - radius)
    val arcSize = Size(radius * 2, radius * 2)
    val strokeWidth = 24f

    // Background segments: red (dry) -> yellow -> green (comfortable) -> yellow -> blue (humid)
    val segments = listOf(
        Color(0xFFF44336) to 0.15f,   // 0-15% very dry (red)
        Color(0xFFFF9800) to 0.15f,   // 15-30% dry (orange)
        Color(0xFF4CAF50) to 0.30f,   // 30-60% comfortable (green)
        Color(0xFFFF9800) to 0.15f,   // 60-75% getting humid (orange)
        Color(0xFF2196F3) to 0.25f,   // 75-100% humid (blue)
    )

    var drawnSweep = 0f
    for ((color, fraction) in segments) {
        val segmentSweep = totalSweep * fraction
        drawArc(
            color = color.copy(alpha = 0.3f),
            startAngle = startAngle + drawnSweep,
            sweepAngle = segmentSweep,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
        )
        drawnSweep += segmentSweep
    }

    // Active arc up to current value
    val progress = (humidity / 100f).coerceIn(0f, 1f)
    val activeSweep = totalSweep * progress
    var activeDrawn = 0f
    for ((color, fraction) in segments) {
        val segmentSweep = totalSweep * fraction
        val segmentStart = activeDrawn
        val segmentEnd = activeDrawn + segmentSweep
        if (segmentStart < activeSweep) {
            val drawSweep = (activeSweep - segmentStart).coerceAtMost(segmentSweep)
            drawArc(
                color = color,
                startAngle = startAngle + segmentStart,
                sweepAngle = drawSweep,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
            )
        }
        activeDrawn += segmentSweep
    }

    // Needle indicator
    val needleAngle = startAngle + totalSweep * progress
    val needleRad = Math.toRadians(needleAngle.toDouble())
    val needleOuterRadius = radius + 4f
    val needleInnerRadius = radius - strokeWidth - 4f
    val outerX = centerX + needleOuterRadius * cos(needleRad).toFloat()
    val outerY = centerY + needleOuterRadius * sin(needleRad).toFloat()
    val innerX = centerX + needleInnerRadius * cos(needleRad).toFloat()
    val innerY = centerY + needleInnerRadius * sin(needleRad).toFloat()

    drawLine(
        color = Color(0xFF1A3A5C),
        start = Offset(innerX, innerY),
        end = Offset(outerX, outerY),
        strokeWidth = 3f,
        cap = StrokeCap.Round,
    )

    // Small dot at needle tip
    drawCircle(
        color = Color(0xFF1A3A5C),
        radius = 5f,
        center = Offset(outerX, outerY),
    )
}

private data class ComfortLevel(val label: String, val color: Color)

private fun getComfortLevel(humidity: Float): ComfortLevel = when {
    humidity < 20f -> ComfortLevel("Very Dry", Color(0xFFF44336))
    humidity < 30f -> ComfortLevel("Dry", Color(0xFFFF9800))
    humidity <= 60f -> ComfortLevel("Comfortable", Color(0xFF4CAF50))
    humidity <= 75f -> ComfortLevel("Humid", Color(0xFFFF9800))
    else -> ComfortLevel("Very Humid", Color(0xFFF44336))
}
