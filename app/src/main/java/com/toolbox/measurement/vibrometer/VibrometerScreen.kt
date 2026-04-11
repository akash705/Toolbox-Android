package com.toolbox.measurement.vibrometer

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val WAVEFORM_SAMPLES = 200

@Composable
fun VibrometerScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    val sensorManager = remember { context.getSystemService(SensorManager::class.java) }
    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    val waveform = remember { FloatArray(WAVEFORM_SAMPLES) }
    var writeIndex by remember { mutableIntStateOf(0) }
    var currentMagnitude by remember { mutableFloatStateOf(0f) }
    var peakMagnitude by remember { mutableFloatStateOf(0f) }
    var rmsMagnitude by remember { mutableFloatStateOf(0f) }
    var zeroCrossings by remember { mutableIntStateOf(0) }
    var sampleCount by remember { mutableIntStateOf(0) }
    var sumSquared by remember { mutableFloatStateOf(0f) }
    var isRunning by remember { mutableStateOf(true) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val waveColor = MaterialTheme.colorScheme.primary

    DisposableEffect(lifecycleOwner, isRunning) {
        if (accelerometer == null || !isRunning) return@DisposableEffect onDispose {}

        val listener = object : SensorEventListener {
            private var lastValue = 0f

            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val mag = sqrt(x * x + y * y + z * z) - 9.81f
                val absMag = abs(mag).coerceAtLeast(0f)

                currentMagnitude = absMag
                if (absMag > peakMagnitude) peakMagnitude = absMag

                sumSquared += absMag * absMag
                sampleCount++
                rmsMagnitude = sqrt(sumSquared / sampleCount)

                if ((lastValue >= 0 && mag < 0) || (lastValue < 0 && mag >= 0)) {
                    zeroCrossings++
                }
                lastValue = mag

                waveform[writeIndex % WAVEFORM_SAMPLES] = mag
                writeIndex++
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME ->
                    sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_FASTEST)
                Lifecycle.Event.ON_PAUSE ->
                    sensorManager.unregisterListener(listener)
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            sensorManager.unregisterListener(listener)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Estimate frequency from zero crossings
    val estimatedFreq = if (sampleCount > 50) {
        (zeroCrossings.toFloat() / 2f) / (sampleCount.toFloat() / 200f)
    } else 0f

    val animatedFreq by animateFloatAsState(
        targetValue = estimatedFreq,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
        label = "freq",
    )

    // Map frequency to gauge sweep (0-360 degrees, max ~100 Hz)
    val gaugeSweep = (animatedFreq / 100f * 360f).coerceIn(0f, 360f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Circular gauge with Hz in center
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(260.dp),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawVibrometerGauge(
                    sweepAngle = gaugeSweep,
                    primaryColor = primaryColor,
                    trackColor = trackColor,
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "%.1f".format(animatedFreq),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                )
                Text(
                    text = "hertz",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Waveform chart card
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
                    text = "WAVEFORM",
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
                    drawWaveform(
                        waveform = waveform,
                        writeIndex = writeIndex,
                        totalSamples = WAVEFORM_SAMPLES,
                        waveColor = waveColor,
                        gridColor = trackColor,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Peak and RMS cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MeasurementCard(
                label = "Peak",
                value = "%.2f g".format(peakMagnitude),
                modifier = Modifier.weight(1f),
                isPrimary = true,
            )
            MeasurementCard(
                label = "RMS",
                value = "%.2f g".format(rmsMagnitude),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isRunning = !isRunning
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isRunning) "Stop" else "Start")
            }

            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    peakMagnitude = 0f
                    rmsMagnitude = 0f
                    sumSquared = 0f
                    sampleCount = 0
                    zeroCrossings = 0
                    writeIndex = 0
                    waveform.fill(0f)
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

private fun DrawScope.drawVibrometerGauge(
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

    // Needle at current position
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

private fun DrawScope.drawWaveform(
    waveform: FloatArray,
    writeIndex: Int,
    totalSamples: Int,
    waveColor: Color,
    gridColor: Color,
) {
    val w = size.width
    val h = size.height
    val midY = h / 2

    // Grid lines
    for (i in 0..4) {
        val y = h * i / 4f
        drawLine(gridColor.copy(alpha = 0.3f), Offset(0f, y), Offset(w, y), strokeWidth = 0.5f)
    }
    drawLine(gridColor.copy(alpha = 0.5f), Offset(0f, midY), Offset(w, midY), strokeWidth = 1f)

    // Waveform path
    val samplesAvailable = minOf(writeIndex, totalSamples)
    if (samplesAvailable > 1) {
        val path = Path()
        val startIdx = if (writeIndex >= totalSamples) writeIndex else 0
        val maxAmp = 5f

        for (i in 0 until samplesAvailable) {
            val idx = (startIdx + i) % totalSamples
            val x = w * i / (totalSamples - 1).toFloat()
            val y = midY - (waveform[idx] / maxAmp) * (h / 2f)
            if (i == 0) path.moveTo(x, y.coerceIn(0f, h))
            else path.lineTo(x, y.coerceIn(0f, h))
        }

        drawPath(path, waveColor, style = Stroke(width = 2f))
    }
}
