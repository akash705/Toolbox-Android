package com.toolbox.measurement.soundmeter

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toolbox.core.permission.PermissionGate
import com.toolbox.core.sensor.rememberAudioLevel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SoundMeterScreen() {
    PermissionGate(
        permission = android.Manifest.permission.RECORD_AUDIO,
        rationale = "The sound meter needs microphone access to measure ambient noise levels.",
    ) {
        SoundMeterContent()
    }
}

@Composable
private fun SoundMeterContent() {
    val dbState = rememberAudioLevel()
    val currentDb = dbState.value

    var minDb by remember { mutableFloatStateOf(Float.MAX_VALUE) }
    var maxDb by remember { mutableFloatStateOf(0f) }
    var sumDb by remember { mutableFloatStateOf(0f) }
    var sampleCount by remember { mutableFloatStateOf(0f) }

    // Update stats
    if (currentDb > 0f) {
        if (currentDb < minDb) minDb = currentDb
        if (currentDb > maxDb) maxDb = currentDb
        sumDb += currentDb
        sampleCount++
    }

    val avgDb = if (sampleCount > 0) sumDb / sampleCount else 0f

    val animatedDb by animateFloatAsState(
        targetValue = currentDb,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "db",
    )

    val level = getNoiseLevel(currentDb)

    // Gauge colors
    val gaugeGreen = Color(0xFF4CAF50)
    val gaugeYellow = Color(0xFFFFC107)
    val gaugeOrange = Color(0xFFFF9800)
    val gaugeRed = Color(0xFFF44336)
    val gaugeBackground = MaterialTheme.colorScheme.surfaceContainerHighest

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Gauge
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(260.dp),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawGauge(
                    value = animatedDb,
                    maxValue = 120f,
                    green = gaugeGreen,
                    yellow = gaugeYellow,
                    orange = gaugeOrange,
                    red = gaugeRed,
                    background = gaugeBackground,
                )
            }

            // dB value in center
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 32.dp),
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${currentDb.toInt()}",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = " dB",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                Text(
                    text = level.label.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = level.color,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // MIN / AVG / MAX stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatCard(
                label = "MIN",
                value = if (minDb == Float.MAX_VALUE) "-- dB" else "${minDb.toInt()} dB",
            )
            StatCard(
                label = "AVG",
                value = if (sampleCount == 0f) "-- dB" else "${avgDb.toInt()} dB",
            )
            StatCard(
                label = "MAX",
                value = if (maxDb == 0f) "-- dB" else "${maxDb.toInt()} dB",
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Audio wave indicator
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 24.dp),
        ) {
            drawAudioWave(
                level = animatedDb / 120f,
                color = level.color,
                backgroundColor = gaugeBackground,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Reset button
        OutlinedButton(
            onClick = {
                minDb = Float.MAX_VALUE
                maxDb = 0f
                sumDb = 0f
                sampleCount = 0f
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
        ) {
            Text(
                text = "Reset",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Readings are approximate and not calibrated\nfor professional use.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
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

private data class NoiseLevel(val label: String, val color: Color)

private fun getNoiseLevel(db: Float): NoiseLevel = when {
    db < 30 -> NoiseLevel("Quiet", Color(0xFF4CAF50))
    db < 60 -> NoiseLevel("Moderate", Color(0xFF8BC34A))
    db < 80 -> NoiseLevel("Loud", Color(0xFFFFC107))
    db < 100 -> NoiseLevel("Very Loud", Color(0xFFFF9800))
    else -> NoiseLevel("Dangerous", Color(0xFFF44336))
}

private fun DrawScope.drawGauge(
    value: Float,
    maxValue: Float,
    green: Color,
    yellow: Color,
    orange: Color,
    red: Color,
    background: Color,
) {
    val strokeWidth = 24f
    val padding = strokeWidth / 2 + 16f
    val arcRect = Size(size.width - padding * 2, size.height - padding * 2)
    val topLeft = Offset(padding, padding)

    // Background arc (semicircle from 150° to 240° sweep)
    val startAngle = 150f
    val totalSweep = 240f

    drawArc(
        color = background,
        startAngle = startAngle,
        sweepAngle = totalSweep,
        useCenter = false,
        topLeft = topLeft,
        size = arcRect,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
    )

    // Colored segments
    val segments = listOf(
        green to 0.25f,   // 0-30 dB
        Color(0xFF8BC34A) to 0.25f,  // 30-60 dB
        yellow to 0.167f,  // 60-80 dB
        orange to 0.167f,  // 80-100 dB
        red to 0.167f,     // 100-120 dB
    )

    val progress = (value / maxValue).coerceIn(0f, 1f)
    var drawnSweep = 0f

    for ((color, fraction) in segments) {
        val segmentSweep = totalSweep * fraction
        val remainingProgress = progress - drawnSweep / totalSweep
        if (remainingProgress <= 0) break

        val drawSweep = (segmentSweep).coerceAtMost(remainingProgress * totalSweep)
        drawArc(
            color = color,
            startAngle = startAngle + drawnSweep,
            sweepAngle = drawSweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcRect,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        drawnSweep += segmentSweep
    }

    // Needle indicator
    val needleAngle = startAngle + totalSweep * progress
    val needleRad = Math.toRadians(needleAngle.toDouble())
    val needleRadius = (arcRect.width / 2) - strokeWidth
    val centerX = topLeft.x + arcRect.width / 2
    val centerY = topLeft.y + arcRect.height / 2

    val needleEndX = centerX + needleRadius * cos(needleRad).toFloat()
    val needleEndY = centerY + needleRadius * sin(needleRad).toFloat()

    drawCircle(
        color = Color(0xFF424242),
        radius = 8f,
        center = Offset(centerX, centerY),
    )
    drawLine(
        color = Color(0xFF424242),
        start = Offset(centerX, centerY),
        end = Offset(needleEndX, needleEndY),
        strokeWidth = 3f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawAudioWave(
    level: Float,
    color: Color,
    backgroundColor: Color,
) {
    val centerY = size.height / 2
    val barCount = 20
    val barWidth = size.width / (barCount * 2f)
    val maxHeight = size.height * 0.8f

    for (i in 0 until barCount) {
        val x = size.width / 2 + (i - barCount / 2) * barWidth * 2
        val distFromCenter = kotlin.math.abs(i - barCount / 2f) / (barCount / 2f)
        val heightFactor = (1f - distFromCenter * 0.6f) * level
        val barHeight = (maxHeight * heightFactor).coerceAtLeast(4f)

        drawRoundRect(
            color = if (heightFactor > 0.05f) color.copy(alpha = 0.3f + 0.7f * heightFactor) else backgroundColor,
            topLeft = Offset(x - barWidth / 2, centerY - barHeight / 2),
            size = Size(barWidth, barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2),
        )
    }
}
