package com.toolbox.measurement.barometer

import android.hardware.SensorManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toolbox.core.sensor.rememberPressureData

private const val CHART_SAMPLES = 60

private enum class PressureUnit(val label: String) {
    HPA("hPa"),
    MBAR("mbar"),
    INHG("inHg"),
    MMHG("mmHg"),
}

@Composable
fun BarometerScreen() {
    val pressureState = rememberPressureData()
    val currentPressure = pressureState.value

    var minPressure by remember { mutableFloatStateOf(Float.MAX_VALUE) }
    var maxPressure by remember { mutableFloatStateOf(0f) }
    var sumPressure by remember { mutableFloatStateOf(0f) }
    var sampleCount by remember { mutableFloatStateOf(0f) }

    val chartData = remember { FloatArray(CHART_SAMPLES) }
    var chartIndex by remember { mutableIntStateOf(0) }

    var selectedUnit by remember { mutableStateOf(PressureUnit.HPA) }
    var referencePressure by remember { mutableFloatStateOf(0f) }

    // Update stats and chart
    if (currentPressure > 0f) {
        if (currentPressure < minPressure) minPressure = currentPressure
        if (currentPressure > maxPressure) maxPressure = currentPressure
        sumPressure += currentPressure
        sampleCount++
        chartData[chartIndex % CHART_SAMPLES] = currentPressure
        chartIndex++
    }

    val avgPressure = if (sampleCount > 0) sumPressure / sampleCount else 0f

    val animatedPressure by animateFloatAsState(
        targetValue = currentPressure,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "pressure",
    )

    // Trend detection: compare recent avg vs older avg
    val trend = remember(chartIndex) {
        if (chartIndex < 10) 0 // not enough data
        else {
            val total = minOf(chartIndex, CHART_SAMPLES)
            val startIdx = if (chartIndex >= CHART_SAMPLES) chartIndex else 0
            val recentCount = minOf(5, total)
            val olderCount = minOf(5, total - recentCount)
            if (olderCount <= 0) 0
            else {
                var recentSum = 0f
                var olderSum = 0f
                for (i in 0 until recentCount) {
                    recentSum += chartData[(startIdx + total - 1 - i) % CHART_SAMPLES]
                }
                for (i in 0 until olderCount) {
                    olderSum += chartData[(startIdx + total - recentCount - 1 - i) % CHART_SAMPLES]
                }
                val recentAvg = recentSum / recentCount
                val olderAvg = olderSum / olderCount
                val diff = recentAvg - olderAvg
                when {
                    diff > 0.3f -> 1   // rising
                    diff < -0.3f -> -1 // falling
                    else -> 0          // stable
                }
            }
        }
    }

    val weatherLabel = getWeatherLabel(currentPressure)
    val chartColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    // Altitude calculation
    val altitude = if (currentPressure > 0f) {
        val refPressure = if (referencePressure > 0f) referencePressure else SensorManager.PRESSURE_STANDARD_ATMOSPHERE
        SensorManager.getAltitude(refPressure, currentPressure)
    } else 0f

    val relativeAltitude = if (referencePressure > 0f) {
        SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, currentPressure) -
            SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, referencePressure)
    } else altitude

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Large pressure reading
        Row(
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = formatPressure(animatedPressure, selectedUnit),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = " ${selectedUnit.label}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            // Trend arrow
            val trendIcon = when (trend) {
                1 -> Icons.Default.TrendingUp
                -1 -> Icons.Default.TrendingDown
                else -> Icons.Default.TrendingFlat
            }
            val trendColor = when (trend) {
                1 -> Color(0xFF4CAF50)
                -1 -> Color(0xFFF44336)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Icon(
                imageVector = trendIcon,
                contentDescription = "Pressure trend",
                tint = trendColor,
                modifier = Modifier.padding(start = 8.dp, bottom = 10.dp),
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Weather label badge
        Card(
            colors = CardDefaults.cardColors(
                containerColor = weatherLabel.color.copy(alpha = 0.12f),
            ),
            shape = RoundedCornerShape(20.dp),
        ) {
            Text(
                text = weatherLabel.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = weatherLabel.color,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Unit toggle chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            PressureUnit.entries.forEach { unit ->
                FilterChip(
                    selected = selectedUnit == unit,
                    onClick = { selectedUnit = unit },
                    label = {
                        Text(
                            text = unit.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedUnit == unit) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Relative Altitude card
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
                    text = "RELATIVE ALTITUDE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (referencePressure > 0f) {
                        "%+.1f m".format(relativeAltitude)
                    } else {
                        "%.1f m".format(altitude)
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (referencePressure > 0f) "from reference point" else "from standard atmosphere",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = { referencePressure = currentPressure },
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Text("Set Reference", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pressure Trend chart card
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
                    text = "PRESSURE TREND",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                ) {
                    val w = size.width
                    val h = size.height

                    // Grid lines
                    for (i in 0..4) {
                        val y = h * i / 4f
                        drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 0.5f)
                    }

                    val totalSamples = minOf(chartIndex, CHART_SAMPLES)
                    if (totalSamples > 1) {
                        val startIdx = if (chartIndex >= CHART_SAMPLES) chartIndex else 0
                        val localMin = chartData.take(totalSamples).min()
                        val localMax = chartData.take(totalSamples).max()
                        val range = (localMax - localMin).coerceAtLeast(1f)

                        val path = Path()
                        for (i in 0 until totalSamples) {
                            val idx = (startIdx + i) % CHART_SAMPLES
                            val x = w * i / (CHART_SAMPLES - 1).toFloat()
                            val y = h - ((chartData[idx] - localMin) / range) * h * 0.9f - h * 0.05f
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, chartColor, style = Stroke(width = 2f))
                    }
                }
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
                if (minPressure == Float.MAX_VALUE) "-- ${selectedUnit.label}"
                else "${formatPressure(minPressure, selectedUnit)} ${selectedUnit.label}",
                Modifier.weight(1f),
            )
            StatCard(
                "AVG",
                if (sampleCount == 0f) "-- ${selectedUnit.label}"
                else "${formatPressure(avgPressure, selectedUnit)} ${selectedUnit.label}",
                Modifier.weight(1f),
            )
            StatCard(
                "MAX",
                if (maxPressure == 0f) "-- ${selectedUnit.label}"
                else "${formatPressure(maxPressure, selectedUnit)} ${selectedUnit.label}",
                Modifier.weight(1f),
            )
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

private fun convertPressure(hPa: Float, unit: PressureUnit): Float = when (unit) {
    PressureUnit.HPA -> hPa
    PressureUnit.MBAR -> hPa // mbar == hPa
    PressureUnit.INHG -> hPa * 0.02953f
    PressureUnit.MMHG -> hPa * 0.75006f
}

private fun formatPressure(hPa: Float, unit: PressureUnit): String {
    val value = convertPressure(hPa, unit)
    return when (unit) {
        PressureUnit.HPA, PressureUnit.MBAR -> "%.1f".format(value)
        PressureUnit.INHG -> "%.2f".format(value)
        PressureUnit.MMHG -> "%.1f".format(value)
    }
}

private data class WeatherLabel(val label: String, val color: Color)

private fun getWeatherLabel(hPa: Float): WeatherLabel = when {
    hPa <= 0f -> WeatherLabel("No Data", Color(0xFF9E9E9E))
    hPa < 1000f -> WeatherLabel("Low Pressure", Color(0xFF5C6BC0))
    hPa <= 1025f -> WeatherLabel("Normal Pressure", Color(0xFF4CAF50))
    else -> WeatherLabel("High Pressure", Color(0xFFFF9800))
}
