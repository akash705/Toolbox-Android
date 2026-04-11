package com.toolbox.measurement.protractor

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.core.sensor.rememberAccelerometerData
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun ProtractorScreen() {
    val accelState = rememberAccelerometerData()
    var isLocked by remember { mutableStateOf(false) }
    var lockedAngle by remember { mutableStateOf(0f) }

    val accel = accelState.value
    // Calculate tilt angle from vertical (pitch)
    val rawAngle = Math.toDegrees(
        atan2(accel[1].toDouble(), sqrt((accel[0] * accel[0] + accel[2] * accel[2]).toDouble()))
    ).toFloat()

    val angle = if (isLocked) lockedAngle else rawAngle

    val animatedAngle by animateFloatAsState(
        targetValue = angle,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
        label = "angle",
    )

    val textMeasurer = rememberTextMeasurer()
    val primaryColor = MaterialTheme.colorScheme.primary
    val tickColor = MaterialTheme.colorScheme.outlineVariant
    val onSurface = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Angle display
        Text(
            text = "%.1f°".format(abs(animatedAngle)),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = primaryColor,
        )
        Text(
            text = when {
                abs(angle) < 1f -> "Level"
                angle > 0 -> "Tilted Forward"
                else -> "Tilted Backward"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Protractor arc
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(300.dp),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawProtractor(
                    angle = animatedAngle,
                    primaryColor = primaryColor,
                    tickColor = tickColor,
                    textColor = onSurface,
                    textMeasurer = textMeasurer,
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Lock button
        Button(
            onClick = {
                if (!isLocked) {
                    lockedAngle = rawAngle
                }
                isLocked = !isLocked
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text(
                text = if (isLocked) "Unlock Angle" else "Lock Angle",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun DrawScope.drawProtractor(
    angle: Float,
    primaryColor: Color,
    tickColor: Color,
    textColor: Color,
    textMeasurer: TextMeasurer,
) {
    val center = Offset(size.width / 2, size.height * 0.85f)
    val radius = size.minDimension / 2 - 24f

    // Draw protractor arc (semicircle from 180° to 0°)
    drawArc(
        color = tickColor,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
        style = Stroke(width = 2f),
    )

    // Base line
    drawLine(
        color = tickColor,
        start = Offset(center.x - radius, center.y),
        end = Offset(center.x + radius, center.y),
        strokeWidth = 2f,
    )

    // Tick marks and labels every 10 degrees
    for (i in 0..180 step 5) {
        val rad = Math.toRadians(i.toDouble())
        val isMajor = i % 30 == 0
        val isMinor = i % 10 == 0
        val tickLength = when {
            isMajor -> 20f
            isMinor -> 14f
            else -> 8f
        }

        val outerX = center.x - radius * cos(rad).toFloat()
        val outerY = center.y - radius * sin(rad).toFloat()
        val innerX = center.x - (radius - tickLength) * cos(rad).toFloat()
        val innerY = center.y - (radius - tickLength) * sin(rad).toFloat()

        drawLine(
            color = tickColor,
            start = Offset(outerX, outerY),
            end = Offset(innerX, innerY),
            strokeWidth = if (isMajor) 2f else 1f,
        )

        // Label major ticks
        if (isMajor) {
            val labelRadius = radius - 32f
            val lx = center.x - labelRadius * cos(rad).toFloat()
            val ly = center.y - labelRadius * sin(rad).toFloat()
            val label = "$i°"
            val style = TextStyle(fontSize = 11.sp, color = textColor)
            val measured = textMeasurer.measure(label, style)
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    lx - measured.size.width / 2,
                    ly - measured.size.height / 2,
                ),
            )
        }
    }

    // Draw angle indicator needle
    val clampedAngle = angle.coerceIn(-90f, 90f)
    // Map angle: 0° = vertical (90° in protractor coords), positive = forward tilt
    val needleAngle = Math.toRadians((90.0 - clampedAngle))
    val needleLength = radius - 8f
    val needleEndX = center.x - needleLength * cos(needleAngle).toFloat()
    val needleEndY = center.y - needleLength * sin(needleAngle).toFloat()

    // Needle line
    drawLine(
        color = primaryColor,
        start = center,
        end = Offset(needleEndX, needleEndY),
        strokeWidth = 3f,
    )

    // Arrow head
    val arrowSize = 12f
    val arrowAngle1 = needleAngle + Math.toRadians(150.0)
    val arrowAngle2 = needleAngle - Math.toRadians(150.0)
    val arrowPath = Path().apply {
        moveTo(needleEndX, needleEndY)
        lineTo(
            needleEndX - arrowSize * cos(arrowAngle1).toFloat(),
            needleEndY - arrowSize * sin(arrowAngle1).toFloat(),
        )
        moveTo(needleEndX, needleEndY)
        lineTo(
            needleEndX - arrowSize * cos(arrowAngle2).toFloat(),
            needleEndY - arrowSize * sin(arrowAngle2).toFloat(),
        )
    }
    drawPath(arrowPath, primaryColor, style = Stroke(width = 3f))

    // Center dot
    drawCircle(
        color = primaryColor,
        radius = 6f,
        center = center,
    )
}
