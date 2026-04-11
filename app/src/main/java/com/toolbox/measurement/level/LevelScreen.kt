package com.toolbox.measurement.level

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toolbox.core.sensor.rememberAccelerometerData
import kotlin.math.atan2
import kotlin.math.sqrt

@Composable
fun LevelScreen() {
    val accelState = rememberAccelerometerData()
    var isLocked by remember { mutableStateOf(false) }
    var lockedPitch by remember { mutableStateOf(0f) }
    var lockedRoll by remember { mutableStateOf(0f) }

    // Calculate pitch and roll from accelerometer
    val accel = accelState.value
    val rawPitch = Math.toDegrees(
        atan2(accel[1].toDouble(), sqrt((accel[0] * accel[0] + accel[2] * accel[2]).toDouble()))
    ).toFloat()
    val rawRoll = Math.toDegrees(
        atan2(accel[0].toDouble(), sqrt((accel[1] * accel[1] + accel[2] * accel[2]).toDouble()))
    ).toFloat()

    val pitch = if (isLocked) lockedPitch else rawPitch
    val roll = if (isLocked) lockedRoll else rawRoll

    val totalDeviation = sqrt(pitch * pitch + roll * roll)

    // Animate bubble position
    val animatedPitch by animateFloatAsState(
        targetValue = pitch,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
        label = "pitch",
    )
    val animatedRoll by animateFloatAsState(
        targetValue = roll,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
        label = "roll",
    )

    val bubbleColor = Color(0xFFFF9800) // Orange
    val ringColor = MaterialTheme.colorScheme.outlineVariant
    val centerDotColor = MaterialTheme.colorScheme.outline
    val levelGreen = Color(0xFF4CAF50)
    val isLevel = totalDeviation < 1f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Circular level
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(280.dp),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircularLevel(
                    pitch = animatedPitch,
                    roll = animatedRoll,
                    bubbleColor = if (isLevel) levelGreen else bubbleColor,
                    ringColor = ringColor,
                    centerDotColor = centerDotColor,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Total deviation
        Text(
            text = "%.1f°".format(totalDeviation),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = if (isLevel) levelGreen else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Total Deviation",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Linear level bar
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 16.dp),
        ) {
            drawLinearLevel(
                roll = animatedRoll,
                bubbleColor = if (isLevel) levelGreen else bubbleColor,
                trackColor = ringColor,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Pitch and Roll values
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ValueDisplay(label = "PITCH", value = "%.1f°".format(pitch))
            ValueDisplay(label = "ROLL", value = "%.1f°".format(roll))
        }

        Spacer(modifier = Modifier.weight(1f))

        // Lock button
        Button(
            onClick = {
                if (!isLocked) {
                    lockedPitch = rawPitch
                    lockedRoll = rawRoll
                }
                isLocked = !isLocked
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = if (isLocked) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                )
            } else {
                ButtonDefaults.buttonColors()
            },
        ) {
            Text(
                text = if (isLocked) "Unlock Reading" else "Lock Reading",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ValueDisplay(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun DrawScope.drawCircularLevel(
    pitch: Float,
    roll: Float,
    bubbleColor: Color,
    ringColor: Color,
    centerDotColor: Color,
) {
    val center = Offset(size.width / 2, size.height / 2)
    val maxRadius = size.minDimension / 2 - 8f

    // Draw concentric rings
    val ringCount = 4
    for (i in 1..ringCount) {
        val ringRadius = maxRadius * i / ringCount
        drawCircle(
            color = ringColor,
            radius = ringRadius,
            center = center,
            style = Stroke(width = if (i == ringCount) 2.5f else 1.5f),
        )
    }

    // Draw crosshair lines
    drawLine(
        color = ringColor,
        start = Offset(center.x - maxRadius, center.y),
        end = Offset(center.x + maxRadius, center.y),
        strokeWidth = 1f,
    )
    drawLine(
        color = ringColor,
        start = Offset(center.x, center.y - maxRadius),
        end = Offset(center.x, center.y + maxRadius),
        strokeWidth = 1f,
    )

    // Center dot
    drawCircle(
        color = centerDotColor,
        radius = 4f,
        center = center,
    )

    // Bubble position — clamp to outer ring
    val maxTilt = 15f // degrees for full range
    val bubbleOffsetX = (roll / maxTilt).coerceIn(-1f, 1f) * maxRadius
    val bubbleOffsetY = (-pitch / maxTilt).coerceIn(-1f, 1f) * maxRadius
    val bubbleCenter = Offset(center.x + bubbleOffsetX, center.y + bubbleOffsetY)

    // Bubble
    drawCircle(
        color = bubbleColor.copy(alpha = 0.3f),
        radius = 28f,
        center = bubbleCenter,
    )
    drawCircle(
        color = bubbleColor,
        radius = 28f,
        center = bubbleCenter,
        style = Stroke(width = 3f),
    )
    drawCircle(
        color = bubbleColor,
        radius = 8f,
        center = bubbleCenter,
    )
}

private fun DrawScope.drawLinearLevel(
    roll: Float,
    bubbleColor: Color,
    trackColor: Color,
) {
    val centerY = size.height / 2
    val trackWidth = size.width
    val trackHeight = 20f

    // Track background
    drawRoundRect(
        color = trackColor,
        topLeft = Offset(0f, centerY - trackHeight / 2),
        size = androidx.compose.ui.geometry.Size(trackWidth, trackHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2),
    )

    // Center tick marks
    val tickCount = 9
    for (i in 0 until tickCount) {
        val x = trackWidth * i / (tickCount - 1)
        val tickHeight = if (i == tickCount / 2) 30f else 14f
        drawLine(
            color = trackColor,
            start = Offset(x, centerY - tickHeight / 2),
            end = Offset(x, centerY + tickHeight / 2),
            strokeWidth = if (i == tickCount / 2) 2f else 1f,
            cap = StrokeCap.Round,
        )
    }

    // Bubble on linear level
    val maxTilt = 15f
    val bubbleX = (size.width / 2) + (roll / maxTilt).coerceIn(-1f, 1f) * (size.width / 2 - 20f)
    drawCircle(
        color = bubbleColor,
        radius = 14f,
        center = Offset(bubbleX, centerY),
    )
}
