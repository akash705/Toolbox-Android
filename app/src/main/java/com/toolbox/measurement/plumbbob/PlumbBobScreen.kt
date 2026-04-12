package com.toolbox.measurement.plumbbob

import android.Manifest
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.core.camera.CameraPreview
import com.toolbox.core.permission.PermissionGate
import kotlin.math.abs
import kotlin.math.atan2

@Composable
fun PlumbBobScreen() {
    PermissionGate(
        permission = Manifest.permission.CAMERA,
        rationale = "Plumb Bob needs camera access to show the vertical reference line over the camera view.",
    ) {
        var tiltAngle by remember { mutableFloatStateOf(0f) }
        val context = LocalContext.current

        val sensorManager = remember {
            context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        }

        val listener = remember {
            object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        val x = event.values[0]
                        val y = event.values[1]
                        // Angle from vertical in portrait mode
                        tiltAngle = Math.toDegrees(atan2(x.toDouble(), y.toDouble())).toFloat()
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
        }

        DisposableEffect(Unit) {
            val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            accelerometer?.let {
                sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
            }
            onDispose {
                sensorManager.unregisterListener(listener)
            }
        }

        val deviationColor = when {
            abs(tiltAngle) <= 0.5f -> Color(0xFF66BB6A) // green
            abs(tiltAngle) <= 2f -> Color(0xFFFFA726)    // yellow
            else -> Color(0xFFEF5350)                     // red
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // Camera preview
            CameraPreview(modifier = Modifier.fillMaxSize())

            // Plumb line overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2
                val angleRad = Math.toRadians(tiltAngle.toDouble())
                val lineLength = size.height * 0.8f

                // Offset at top and bottom based on tilt
                val topX = centerX - (lineLength / 2 * kotlin.math.sin(angleRad)).toFloat()
                val topY = size.height * 0.1f
                val bottomX = centerX + (lineLength / 2 * kotlin.math.sin(angleRad)).toFloat()
                val bottomY = size.height * 0.9f

                // True vertical reference line (faint)
                drawLine(
                    color = Color.White.copy(alpha = 0.2f),
                    start = Offset(centerX, topY),
                    end = Offset(centerX, bottomY),
                    strokeWidth = 1f,
                )

                // Plumb line
                drawLine(
                    color = deviationColor,
                    start = Offset(topX, topY),
                    end = Offset(bottomX, bottomY),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round,
                )

                // Center crosshair
                val crossSize = 20f
                drawLine(
                    color = deviationColor,
                    start = Offset(centerX - crossSize, size.height / 2),
                    end = Offset(centerX + crossSize, size.height / 2),
                    strokeWidth = 2f,
                )
                drawLine(
                    color = deviationColor,
                    start = Offset(centerX, size.height / 2 - crossSize),
                    end = Offset(centerX, size.height / 2 + crossSize),
                    strokeWidth = 2f,
                )

                // Plumb bob weight at bottom
                drawCircle(
                    color = deviationColor,
                    radius = 10f,
                    center = Offset(bottomX, bottomY),
                )
            }

            // Deviation readout
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .background(
                        Color.Black.copy(alpha = 0.6f),
                        RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "%.1f°".format(abs(tiltAngle)),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = deviationColor,
                )
                Text(
                    text = when {
                        abs(tiltAngle) <= 0.5f -> "Vertical"
                        tiltAngle > 0 -> "Tilted right"
                        else -> "Tilted left"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                )
            }
        }
    }
}
