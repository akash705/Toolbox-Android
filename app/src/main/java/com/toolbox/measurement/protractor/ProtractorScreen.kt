package com.toolbox.measurement.protractor

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
    val haptic = LocalHapticFeedback.current
    var isLocked by remember { mutableStateOf(false) }
    var lockedAngle by remember { mutableFloatStateOf(0f) }
    var lockedPitch by remember { mutableFloatStateOf(0f) }
    var lockedRoll by remember { mutableFloatStateOf(0f) }
    var zeroOffset by remember { mutableFloatStateOf(0f) }

    val accel = accelState.value
    // Pitch: tilt forward/backward
    val rawPitch = Math.toDegrees(
        atan2(accel[1].toDouble(), sqrt((accel[0] * accel[0] + accel[2] * accel[2]).toDouble()))
    ).toFloat()
    // Roll: tilt left/right
    val rawRoll = Math.toDegrees(
        atan2(accel[0].toDouble(), sqrt((accel[1] * accel[1] + accel[2] * accel[2]).toDouble()))
    ).toFloat()
    // Overall tilt angle from vertical
    val rawAngle = Math.toDegrees(
        atan2(
            sqrt((accel[0] * accel[0] + accel[1] * accel[1]).toDouble()),
            accel[2].toDouble()
        )
    ).toFloat() - zeroOffset

    val angle = if (isLocked) lockedAngle else rawAngle
    val pitch = if (isLocked) lockedPitch else rawPitch
    val roll = if (isLocked) lockedRoll else rawRoll

    val animatedAngle by animateFloatAsState(
        targetValue = angle,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
        label = "angle",
    )

    val textMeasurer = rememberTextMeasurer()
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Circular gauge with angle in center
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(280.dp),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircularGauge(
                    angle = animatedAngle,
                    primaryColor = primaryColor,
                    trackColor = trackColor,
                    textColor = onSurface,
                    textMeasurer = textMeasurer,
                )
            }

            // Hero angle in center
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "%.1f°".format(abs(animatedAngle)),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurface,
                )
                Text(
                    text = "ACTIVE TILT",
                    style = MaterialTheme.typography.labelMedium,
                    color = primaryColor,
                    letterSpacing = 1.sp,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Pitch & Roll combined card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PITCH",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "%.1f°".format(pitch),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ROLL",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "%.1f°".format(roll),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Action buttons - stacked vertically
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (!isLocked) {
                        lockedAngle = rawAngle
                        lockedPitch = rawPitch
                        lockedRoll = rawRoll
                    }
                    isLocked = !isLocked
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isLocked) "Unlock" else "Lock / Hold")
            }

            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    zeroOffset = rawAngle + zeroOffset
                    isLocked = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(),
            ) {
                Icon(
                    Icons.Default.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset to Zero")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun DrawScope.drawCircularGauge(
    angle: Float,
    primaryColor: Color,
    trackColor: Color,
    textColor: Color,
    textMeasurer: TextMeasurer,
) {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = size.minDimension / 2 - 32f
    val strokeWidth = 12f

    // Track circle (full ring background)
    drawCircle(
        color = trackColor,
        radius = radius,
        center = center,
        style = Stroke(width = strokeWidth),
    )

    // Active arc (shows measured angle)
    val clampedAngle = angle.coerceIn(0f, 360f)
    if (clampedAngle > 0.5f) {
        drawArc(
            color = primaryColor,
            startAngle = -90f,
            sweepAngle = clampedAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }

    // Degree markers and labels
    val markerAngles = listOf(0, 90, 180, 270)
    val markerLabels = listOf("0°", "90°", "180°", "270°")

    markerAngles.forEachIndexed { index, deg ->
        val rad = Math.toRadians(deg.toDouble() - 90.0) // -90 to start from top

        // Tick marks
        val outerR = radius + 8f
        val innerR = radius - 8f
        drawLine(
            color = textColor.copy(alpha = 0.5f),
            start = Offset(
                center.x + outerR * cos(rad).toFloat(),
                center.y + outerR * sin(rad).toFloat(),
            ),
            end = Offset(
                center.x + innerR * cos(rad).toFloat(),
                center.y + innerR * sin(rad).toFloat(),
            ),
            strokeWidth = 2f,
        )

        // Labels outside the ring
        val labelR = radius + 24f
        val lx = center.x + labelR * cos(rad).toFloat()
        val ly = center.y + labelR * sin(rad).toFloat()
        val style = TextStyle(fontSize = 12.sp, color = textColor, fontWeight = FontWeight.Medium)
        val measured = textMeasurer.measure(markerLabels[index], style)
        drawText(
            textLayoutResult = measured,
            topLeft = Offset(
                lx - measured.size.width / 2,
                ly - measured.size.height / 2,
            ),
        )
    }

    // Minor tick marks every 30 degrees
    for (deg in 0 until 360 step 30) {
        if (deg % 90 == 0) continue // skip cardinal directions already drawn
        val rad = Math.toRadians(deg.toDouble() - 90.0)
        val outerR = radius + 4f
        val innerR = radius - 4f
        drawLine(
            color = textColor.copy(alpha = 0.3f),
            start = Offset(
                center.x + outerR * cos(rad).toFloat(),
                center.y + outerR * sin(rad).toFloat(),
            ),
            end = Offset(
                center.x + innerR * cos(rad).toFloat(),
                center.y + innerR * sin(rad).toFloat(),
            ),
            strokeWidth = 1.5f,
        )
    }

    // Needle line from ring toward center + dot at the angle position
    val needleRad = Math.toRadians(angle.toDouble() - 90.0)
    val needleOuter = radius
    val needleInner = radius - 40f
    drawLine(
        color = primaryColor,
        start = Offset(
            center.x + needleOuter * cos(needleRad).toFloat(),
            center.y + needleOuter * sin(needleRad).toFloat(),
        ),
        end = Offset(
            center.x + needleInner * cos(needleRad).toFloat(),
            center.y + needleInner * sin(needleRad).toFloat(),
        ),
        strokeWidth = 2.5f,
        cap = StrokeCap.Round,
    )
    // Dot on the ring
    val dotCenter = Offset(
        center.x + radius * cos(needleRad).toFloat(),
        center.y + radius * sin(needleRad).toFloat(),
    )
    drawCircle(
        color = primaryColor,
        radius = 7f,
        center = dotCenter,
    )
}
