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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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

    val bubbleColor = Color(0xFFFF9800)
    val ringColor = MaterialTheme.colorScheme.outlineVariant
    val levelGreen = Color(0xFF4CAF50)
    val isLevel = totalDeviation < 1f
    val circleFill = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.35f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Circular level — large, matching Stitch
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(320.dp),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircularLevel(
                    pitch = animatedPitch,
                    roll = animatedRoll,
                    bubbleColor = if (isLevel) levelGreen else bubbleColor,
                    ringColor = ringColor,
                    fillColor = circleFill,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Total deviation
        Text(
            text = "%.1f°".format(totalDeviation),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = if (isLevel) levelGreen else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Total Deviation",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Linear level bar — pill/capsule indicator
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 20.dp),
        ) {
            drawLinearLevel(
                roll = animatedRoll,
                bubbleColor = if (isLevel) levelGreen else bubbleColor,
                trackColor = ringColor,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Pitch and Roll values in card containers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ValueCard(
                label = "PITCH",
                value = "%.1f°".format(pitch),
                modifier = Modifier.weight(1f),
            )
            ValueCard(
                label = "ROLL",
                value = "%.1f°".format(roll),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Lock button — filled with light blue tint, matching Stitch
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
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isLocked) "Unlock Reading" else "Lock Reading",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ValueCard(label: String, value: String, modifier: Modifier = Modifier) {
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
                .padding(vertical = 16.dp),
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
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun DrawScope.drawCircularLevel(
    pitch: Float,
    roll: Float,
    bubbleColor: Color,
    ringColor: Color,
    fillColor: Color,
) {
    val center = Offset(size.width / 2, size.height / 2)
    val maxRadius = size.minDimension / 2 - 8f

    // Filled background circle
    drawCircle(
        color = fillColor,
        radius = maxRadius,
        center = center,
    )

    // Draw concentric rings
    val ringCount = 4
    for (i in 1..ringCount) {
        val ringRadius = maxRadius * i / ringCount
        drawCircle(
            color = ringColor,
            radius = ringRadius,
            center = center,
            style = Stroke(width = if (i == ringCount) 2f else 1.2f),
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

    // Bubble position
    val maxTilt = 15f
    val bubbleOffsetX = (roll / maxTilt).coerceIn(-1f, 1f) * maxRadius
    val bubbleOffsetY = (-pitch / maxTilt).coerceIn(-1f, 1f) * maxRadius
    val bubbleCenter = Offset(center.x + bubbleOffsetX, center.y + bubbleOffsetY)

    // Bubble — large solid filled circle matching Stitch
    val bubbleRadius = 36f
    drawCircle(
        color = bubbleColor,
        radius = bubbleRadius,
        center = bubbleCenter,
    )
    // Small white center dot on bubble
    drawCircle(
        color = Color.White.copy(alpha = 0.8f),
        radius = 5f,
        center = bubbleCenter,
    )
}

private fun DrawScope.drawLinearLevel(
    roll: Float,
    bubbleColor: Color,
    trackColor: Color,
) {
    val centerY = size.height / 2
    val trackHeight = 24f

    // Track background
    drawRoundRect(
        color = trackColor,
        topLeft = Offset(0f, centerY - trackHeight / 2),
        size = Size(size.width, trackHeight),
        cornerRadius = CornerRadius(trackHeight / 2),
    )

    // Tick marks
    val tickCount = 9
    for (i in 0 until tickCount) {
        val x = size.width * i / (tickCount - 1)
        val tickH = if (i == tickCount / 2) 36f else 16f
        drawLine(
            color = trackColor,
            start = Offset(x, centerY - tickH / 2),
            end = Offset(x, centerY + tickH / 2),
            strokeWidth = if (i == tickCount / 2) 2.5f else 1.2f,
        )
    }

    // Pill/capsule bubble indicator — larger and more prominent
    val maxTilt = 15f
    val bubbleX = (size.width / 2) + (roll / maxTilt).coerceIn(-1f, 1f) * (size.width / 2 - 48f)
    val pillWidth = 72f
    val pillHeight = 32f
    drawRoundRect(
        color = bubbleColor,
        topLeft = Offset(bubbleX - pillWidth / 2, centerY - pillHeight / 2),
        size = Size(pillWidth, pillHeight),
        cornerRadius = CornerRadius(pillHeight / 2),
    )
}
