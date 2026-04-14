package com.toolbox.measurement.compass

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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.core.sensor.rememberOrientationData
import com.toolbox.core.sharing.ShareButton
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CompassScreen() {
    val orientationState = rememberOrientationData()

    // azimuth is in radians, convert to degrees
    val rawAzimuth = Math.toDegrees(orientationState.value[0].toDouble()).toFloat()
    val pitch = Math.toDegrees(orientationState.value[1].toDouble()).toFloat()
    val roll = Math.toDegrees(orientationState.value[2].toDouble()).toFloat()

    // Normalize to 0-360
    val bearing = ((rawAzimuth % 360) + 360) % 360

    var useMagneticNorth by remember { mutableStateOf(true) }
    val haptic = LocalHapticFeedback.current

    // Haptic tick when cardinal direction changes
    val currentCardinal = getCardinalDirection(bearing)
    var previousCardinal by remember { mutableStateOf(currentCardinal) }
    LaunchedEffect(currentCardinal) {
        if (currentCardinal != previousCardinal) {
            previousCardinal = currentCardinal
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val animatedBearing by animateFloatAsState(
        targetValue = -bearing,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
        label = "bearing",
    )

    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Current bearing header
        Text(
            text = "CURRENT BEARING",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = getCardinalDirection(bearing),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${bearing.toInt()}°",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Compass rose — larger to match Stitch
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(300.dp),
        ) {
            val cardinalColor = MaterialTheme.colorScheme.onSurface
            val tickColor = MaterialTheme.colorScheme.outlineVariant
            val northColor = Color(0xFFD32F2F)
            val needleRed = Color(0xFFD32F2F)
            val needleWhite = MaterialTheme.colorScheme.surfaceContainerHighest
            val degreeTextColor = MaterialTheme.colorScheme.onSurfaceVariant

            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCompassRose(
                    rotation = animatedBearing,
                    cardinalColor = cardinalColor,
                    tickColor = tickColor,
                    northColor = northColor,
                    needleRed = needleRed,
                    needleWhite = needleWhite,
                    degreeTextColor = degreeTextColor,
                    textMeasurer = textMeasurer,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Incline and Level values with progress bars
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ValueCard(
                label = "INCLINE",
                value = "%.1f°".format(abs(pitch)),
                progress = (abs(pitch) / 90f).coerceIn(0f, 1f),
                modifier = Modifier.weight(1f),
            )
            ValueCard(
                label = "LEVEL",
                value = "%.1f°".format(abs(roll)),
                progress = (abs(roll) / 90f).coerceIn(0f, 1f),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Magnetic North toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Magnetic North",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Using internal magnetometer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = useMagneticNorth,
                    onCheckedChange = { useMagneticNorth = it },
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Coordinates card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Location unavailable",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Enable location for coordinates",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        ShareButton(
            toolName = "Compass",
            value = "${bearing.toInt()}",
            unit = "°",
            label = getCardinalDirection(bearing),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        )
    }
}

@Composable
private fun ValueCard(
    label: String,
    value: String,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                strokeCap = StrokeCap.Round,
            )
        }
    }
}

private fun getCardinalDirection(degrees: Float): String {
    val directions = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
        "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
    val index = ((degrees + 11.25f) / 22.5f).toInt() % 16
    return directions[index]
}

private fun DrawScope.drawCompassRose(
    rotation: Float,
    cardinalColor: Color,
    tickColor: Color,
    northColor: Color,
    needleRed: Color,
    needleWhite: Color,
    degreeTextColor: Color,
    textMeasurer: TextMeasurer,
) {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = size.minDimension / 2 - 16f

    // Outer circle
    drawCircle(
        color = tickColor,
        radius = radius,
        center = center,
        style = Stroke(width = 1.5f),
    )

    // Rotate the entire compass rose
    rotate(degrees = rotation, pivot = center) {
        // Draw tick marks
        for (i in 0 until 360 step 5) {
            val angle = Math.toRadians(i.toDouble())
            val isCardinal = i % 90 == 0
            val isMajor = i % 30 == 0
            val tickLength = when {
                isCardinal -> 20f
                isMajor -> 14f
                else -> 8f
            }
            val tickWidth = when {
                isCardinal -> 2.5f
                isMajor -> 1.5f
                else -> 1f
            }
            val outerX = center.x + (radius - 4f) * sin(angle).toFloat()
            val outerY = center.y - (radius - 4f) * cos(angle).toFloat()
            val innerX = center.x + (radius - 4f - tickLength) * sin(angle).toFloat()
            val innerY = center.y - (radius - 4f - tickLength) * cos(angle).toFloat()

            val color = if (i == 0) northColor else tickColor
            drawLine(
                color = color,
                start = Offset(outerX, outerY),
                end = Offset(innerX, innerY),
                strokeWidth = tickWidth,
            )
        }

        // Degree labels at 30° intervals (skip cardinals — they get letter labels)
        val degreeLabels = listOf(30, 60, 120, 150, 210, 240, 300, 330)
        for (deg in degreeLabels) {
            val radians = Math.toRadians(deg.toDouble())
            val textRadius = radius - 34f
            val x = center.x + textRadius * sin(radians).toFloat()
            val y = center.y - textRadius * cos(radians).toFloat()

            val style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = degreeTextColor,
            )
            val measured = textMeasurer.measure(deg.toString(), style)
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    x - measured.size.width / 2,
                    y - measured.size.height / 2,
                ),
            )
        }

        // Cardinal direction labels
        val cardinals = listOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f)
        for ((label, angle) in cardinals) {
            val radians = Math.toRadians(angle.toDouble())
            val textRadius = radius - 40f
            val x = center.x + textRadius * sin(radians).toFloat()
            val y = center.y - textRadius * cos(radians).toFloat()

            val color = if (label == "N") northColor else cardinalColor
            val style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            val measured = textMeasurer.measure(label, style)
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    x - measured.size.width / 2,
                    y - measured.size.height / 2,
                ),
            )
        }
    }

    // Draw compass needle (fixed, always pointing up)
    val needleLength = radius * 0.5f
    val needleWidth = 12f

    // Red (north) half
    val northPath = Path().apply {
        moveTo(center.x, center.y - needleLength)
        lineTo(center.x - needleWidth / 2, center.y)
        lineTo(center.x + needleWidth / 2, center.y)
        close()
    }
    drawPath(northPath, needleRed)

    // White (south) half
    val southPath = Path().apply {
        moveTo(center.x, center.y + needleLength)
        lineTo(center.x - needleWidth / 2, center.y)
        lineTo(center.x + needleWidth / 2, center.y)
        close()
    }
    drawPath(southPath, needleWhite)

    // Center circle
    drawCircle(
        color = needleRed,
        radius = 6f,
        center = center,
    )
    drawCircle(
        color = Color.White,
        radius = 3f,
        center = center,
    )
}
