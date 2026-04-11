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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(0.3f))

        // Filled semicircle gauge — matching Stitch
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawFilledGauge(
                    value = animatedDb,
                    maxValue = 140f,
                    textMeasurer = textMeasurer,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // dB value
        Row(
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = "${currentDb.toInt()}",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = " dB",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Level badge chip
        Card(
            colors = CardDefaults.cardColors(
                containerColor = level.color.copy(alpha = 0.1f),
            ),
            shape = RoundedCornerShape(20.dp),
        ) {
            Text(
                text = level.label.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = level.color,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // MIN / AVG / MAX stat cards — equal width, AVG highlighted
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatCard(
                label = "MIN",
                value = if (minDb == Float.MAX_VALUE) "--" else "${minDb.toInt()}",
                unit = "dB",
                isHighlighted = false,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "AVG",
                value = if (sampleCount == 0f) "--" else "${avgDb.toInt()}",
                unit = "dB",
                isHighlighted = true,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "MAX",
                value = if (maxDb == 0f) "--" else "${maxDb.toInt()}",
                unit = "dB",
                isHighlighted = false,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

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
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
        ) {
            Text(
                text = "Reset",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Readings are approximate and not calibrated\nfor professional use.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.weight(0.5f))
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    unit: String,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isHighlighted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = FontWeight.SemiBold,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isHighlighted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Text(
                    text = " $unit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
    }
}

private data class NoiseLevel(val label: String, val color: Color)

private fun getNoiseLevel(db: Float): NoiseLevel = when {
    db < 30 -> NoiseLevel("Quiet", Color(0xFF4CAF50))
    db < 60 -> NoiseLevel("Moderate", Color(0xFF4CAF50))
    db < 80 -> NoiseLevel("Loud", Color(0xFFFFC107))
    db < 100 -> NoiseLevel("Very Loud", Color(0xFFFF9800))
    else -> NoiseLevel("Dangerous", Color(0xFFF44336))
}

private fun DrawScope.drawFilledGauge(
    value: Float,
    maxValue: Float,
    textMeasurer: TextMeasurer,
) {
    val gaugeHeight = size.height
    val gaugeWidth = size.width
    val centerX = gaugeWidth / 2
    val centerY = gaugeHeight // pivot at bottom center
    val radius = gaugeWidth / 2 - 20f

    val startAngle = 180f // left
    val totalSweep = 180f // semicircle

    val arcTopLeft = Offset(centerX - radius, centerY - radius)
    val arcSize = Size(radius * 2, radius * 2)

    // Filled color segments — green dominant, then yellow, orange, red
    val segments = listOf(
        Color(0xFF4CAF50) to 0.43f,   // 0-60 dB green
        Color(0xFF8BC34A) to 0.14f,   // 60-80 dB light green
        Color(0xFFFFC107) to 0.14f,   // 80-100 dB yellow
        Color(0xFFFF9800) to 0.14f,   // 100-120 dB orange
        Color(0xFFF44336) to 0.15f,   // 120-140 dB red
    )

    var drawnSweep = 0f
    for ((color, fraction) in segments) {
        val segmentSweep = totalSweep * fraction
        drawArc(
            color = color,
            startAngle = startAngle + drawnSweep,
            sweepAngle = segmentSweep,
            useCenter = true,
            topLeft = arcTopLeft,
            size = arcSize,
        )
        drawnSweep += segmentSweep
    }

    // Inner white mask to create the "thick arc" look
    val innerRadius = radius * 0.35f
    drawCircle(
        color = Color.White,
        radius = innerRadius,
        center = Offset(centerX, centerY),
    )

    // "0" label at bottom left
    val zeroStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, color = Color.White.copy(alpha = 0.8f))
    val zeroMeasured = textMeasurer.measure("0", zeroStyle)
    drawText(
        textLayoutResult = zeroMeasured,
        topLeft = Offset(centerX - radius + 16f, centerY - 24f),
    )

    // "140" label at bottom right
    val maxStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, color = Color.White.copy(alpha = 0.8f))
    val maxMeasured = textMeasurer.measure("140", maxStyle)
    drawText(
        textLayoutResult = maxMeasured,
        topLeft = Offset(centerX + radius - maxMeasured.size.width - 16f, centerY - 24f),
    )

    // Needle — dark blue line with dot at tip and hollow circle at pivot
    val progress = (value / maxValue).coerceIn(0f, 1f)
    val needleAngle = startAngle + totalSweep * progress
    val needleRad = Math.toRadians(needleAngle.toDouble())
    val needleTipRadius = radius * 0.85f
    val needleTipX = centerX + needleTipRadius * cos(needleRad).toFloat()
    val needleTipY = centerY + needleTipRadius * sin(needleRad).toFloat()

    val needleColor = Color(0xFF1A3A5C)

    // Needle line
    drawLine(
        color = needleColor,
        start = Offset(centerX, centerY),
        end = Offset(needleTipX, needleTipY),
        strokeWidth = 3f,
        cap = StrokeCap.Round,
    )

    // Filled dot at needle tip
    drawCircle(
        color = needleColor,
        radius = 7f,
        center = Offset(needleTipX, needleTipY),
    )

    // Hollow circle at pivot
    drawCircle(
        color = needleColor,
        radius = 8f,
        center = Offset(centerX, centerY),
        style = Stroke(width = 3f),
    )
    drawCircle(
        color = Color.White,
        radius = 5f,
        center = Offset(centerX, centerY),
    )
}
