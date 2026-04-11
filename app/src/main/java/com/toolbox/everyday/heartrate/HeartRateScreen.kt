package com.toolbox.everyday.heartrate

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.toolbox.core.permission.PermissionGate
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

private val HeartPink = Color(0xFFE57373)
private val HeartPinkLight = Color(0xFFF8BBD0)

@Composable
fun HeartRateScreen() {
    PermissionGate(
        permission = android.Manifest.permission.CAMERA,
        rationale = "The heart rate monitor needs camera access to detect your pulse through your fingertip.",
    ) {
        HeartRateContent()
    }
}

@Composable
private fun HeartRateContent() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isMeasuring by remember { mutableStateOf(false) }
    var bpm by remember { mutableIntStateOf(0) }
    var fingerDetected by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableIntStateOf(15) }
    var measurementProgress by remember { mutableFloatStateOf(0f) }
    val waveformData = remember { mutableStateListOf<Float>() }
    val recentReadings = remember { mutableStateListOf<Pair<Int, Long>>() } // bpm, timestamp

    var camera by remember { mutableStateOf<Camera?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val analyzer = remember {
        HeartRateAnalyzer(
            onRedIntensity = { intensity ->
                if (waveformData.size > 100) waveformData.removeAt(0)
                waveformData.add(intensity)
            },
            onBpmResult = { result -> bpm = result },
            onFingerDetected = { detected -> fingerDetected = detected },
        )
    }

    // Countdown timer
    LaunchedEffect(isMeasuring) {
        if (isMeasuring) {
            remainingSeconds = 15
            measurementProgress = 0f
            bpm = 0
            waveformData.clear()
            analyzer.startMeasuring()

            for (i in 15 downTo 0) {
                remainingSeconds = i
                measurementProgress = (15 - i) / 15f
                delay(1000)
            }

            // Measurement complete
            isMeasuring = false
            if (bpm > 0) {
                recentReadings.add(0, bpm to System.currentTimeMillis())
                if (recentReadings.size > 3) recentReadings.removeAt(recentReadings.size - 1)
            }
            analyzer.reset()
        }
    }

    // Camera setup
    DisposableEffect(isMeasuring) {
        if (isMeasuring) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(cameraExecutor, analyzer) }

                try {
                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        imageAnalysis,
                    )
                    camera?.cameraControl?.enableTorch(true)
                } catch (_: Exception) {}
            }, ContextCompat.getMainExecutor(context))
        } else {
            camera?.cameraControl?.enableTorch(false)
        }

        onDispose {
            camera?.cameraControl?.enableTorch(false)
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
            } catch (_: Exception) {}
        }
    }

    val pulseLabel = when {
        !isMeasuring && bpm > 0 -> when {
            bpm < 60 -> "LOW PULSE"
            bpm in 60..100 -> "STEADY PULSE"
            else -> "HIGH PULSE"
        }
        isMeasuring && fingerDetected -> "MEASURING..."
        isMeasuring && !fingerDetected -> "PLACE FINGER"
        else -> "READY"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Heart icon
        Icon(
            imageVector = if (isMeasuring && fingerDetected) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = HeartPink,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // BPM display
        Row(
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = if (bpm > 0) "$bpm" else "--",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "BPM",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        Text(
            text = pulseLabel,
            style = MaterialTheme.typography.labelMedium,
            color = HeartPink,
            letterSpacing = 1.5.sp,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Pulse waveform
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            if (waveformData.isNotEmpty()) {
                val waveColor = HeartPink
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                ) {
                    val data = waveformData.toList()
                    if (data.size < 2) return@Canvas

                    val min = data.min()
                    val max = data.max()
                    val range = (max - min).coerceAtLeast(1f)

                    val path = Path()
                    val stepX = size.width / (data.size - 1).coerceAtLeast(1)

                    data.forEachIndexed { index, value ->
                        val x = index * stepX
                        val y = size.height - ((value - min) / range) * size.height * 0.8f - size.height * 0.1f
                        if (index == 0) path.moveTo(x, y)
                        else path.lineTo(x, y)
                    }

                    drawPath(
                        path = path,
                        color = waveColor,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            } else {
                // Placeholder ECG line
                val lineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                ) {
                    val midY = size.height / 2
                    // Flat line with a small blip
                    drawLine(lineColor, Offset(0f, midY), Offset(size.width * 0.35f, midY), 2f)
                    drawLine(lineColor, Offset(size.width * 0.35f, midY), Offset(size.width * 0.38f, midY - 20), 2f)
                    drawLine(lineColor, Offset(size.width * 0.38f, midY - 20), Offset(size.width * 0.40f, midY + 30), 2f)
                    drawLine(lineColor, Offset(size.width * 0.40f, midY + 30), Offset(size.width * 0.43f, midY - 15), 2f)
                    drawLine(lineColor, Offset(size.width * 0.43f, midY - 15), Offset(size.width * 0.46f, midY), 2f)
                    drawLine(lineColor, Offset(size.width * 0.46f, midY), Offset(size.width, midY), 2f)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress bar
        if (isMeasuring) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Measuring...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "${remainingSeconds}s remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { measurementProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeCap = StrokeCap.Round,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Instruction card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Place your fingertip over the rear camera lens. Keep the flash covered and hold steady.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Start/Stop button
        Button(
            onClick = {
                if (isMeasuring) {
                    isMeasuring = false
                    analyzer.reset()
                    camera?.cameraControl?.enableTorch(false)
                } else {
                    isMeasuring = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isMeasuring) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
            ),
        ) {
            Icon(
                imageVector = if (isMeasuring) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isMeasuring) "Stop Measurement" else "Start Measurement",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Readings
        if (recentReadings.isNotEmpty()) {
            Text(
                text = "Recent Readings",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            )

            recentReadings.forEach { (readingBpm, timestamp) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = HeartPink,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$readingBpm BPM",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = formatTimestamp(timestamp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Disclaimer
        Text(
            text = "FOR REFERENCE ONLY. NOT A MEDICAL DEVICE.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            letterSpacing = 0.5.sp,
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000} min ago"
        diff < 86_400_000 -> "${diff / 3_600_000} hours ago"
        else -> "${diff / 86_400_000} days ago"
    }
}
