package com.toolbox.measurement.gyroscope

import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.core.sensor.rememberGyroscopeData
import kotlin.math.abs
import kotlin.math.sqrt

private val AxisBlue = Color(0xFF517BA0)
private val AxisDark = Color(0xFF2D3748)
private val AxisRed = Color(0xFFB84444)

@Composable
fun GyroscopeScreen() {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(SensorManager::class.java) }
    val hasSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null }

    if (!hasSensor) {
        SensorNotAvailable()
        return
    }

    GyroscopeContent()
}

@Composable
private fun GyroscopeContent() {
    val gyroData by rememberGyroscopeData()

    // Convert rad/s to °/s for display
    val xDeg = Math.toDegrees(gyroData.x.toDouble()).toFloat()
    val yDeg = Math.toDegrees(gyroData.y.toDouble()).toFloat()
    val zDeg = Math.toDegrees(gyroData.z.toDouble()).toFloat()

    // Calibration offset
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var offsetZ by remember { mutableStateOf(0f) }

    val calibratedX = xDeg - offsetX
    val calibratedY = yDeg - offsetY
    val calibratedZ = zDeg - offsetZ

    val totalRotation = sqrt(
        calibratedX * calibratedX +
        calibratedY * calibratedY +
        calibratedZ * calibratedZ,
    )

    // Track min/max
    var minX by remember { mutableStateOf(0f) }
    var maxX by remember { mutableStateOf(0f) }
    var minY by remember { mutableStateOf(0f) }
    var maxY by remember { mutableStateOf(0f) }
    var minZ by remember { mutableStateOf(0f) }
    var maxZ by remember { mutableStateOf(0f) }

    if (calibratedX < minX) minX = calibratedX
    if (calibratedX > maxX) maxX = calibratedX
    if (calibratedY < minY) minY = calibratedY
    if (calibratedY > maxY) maxY = calibratedY
    if (calibratedZ < minZ) minZ = calibratedZ
    if (calibratedZ > maxZ) maxZ = calibratedZ

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 3D Phone visualization area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                // Phone silhouette that responds to gyro data
                Box(
                    modifier = Modifier
                        .graphicsLayer(
                            rotationX = (calibratedX * 2).coerceIn(-30f, 30f),
                            rotationY = (calibratedY * 2).coerceIn(-30f, 30f),
                            rotationZ = (calibratedZ * 2).coerceIn(-30f, 30f),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    // Phone body
                    Box(
                        modifier = Modifier
                            .size(width = 84.dp, height = 140.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(18.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        // Phone screen area
                        Box(
                            modifier = Modifier
                                .size(width = 68.dp, height = 116.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.ScreenRotation,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Axis circle indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AxisCircle("X", AxisBlue)
            AxisCircle("Y", AxisDark)
            AxisCircle("Z", AxisRed)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Axis rows with progress bars
        AxisRow(
            label = "X",
            axisName = "Pitch",
            value = calibratedX,
            color = AxisBlue,
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        )

        AxisRow(
            label = "Y",
            axisName = "Roll",
            value = calibratedY,
            color = AxisDark,
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        )

        AxisRow(
            label = "Z",
            axisName = "Yaw",
            value = calibratedZ,
            color = AxisRed,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Total Rotation Rate card
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
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Total Rotation Rate",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            text = "%.2f".format(totalRotation),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "\u00b0/s",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Sensors,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Min/Max card
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
                    .padding(16.dp),
            ) {
                // Minimums
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MINIMUMS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    MinMaxRow("X", minX)
                    MinMaxRow("Y", minY)
                    MinMaxRow("Z", minZ)
                }

                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(80.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )

                // Maximums
                Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(
                        text = "MAXIMUMS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    MinMaxRow("X", maxX)
                    MinMaxRow("Y", maxY)
                    MinMaxRow("Z", maxZ)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Calibrate button
        Button(
            onClick = {
                offsetX = xDeg
                offsetY = yDeg
                offsetZ = zDeg
                minX = 0f; maxX = 0f
                minY = 0f; maxY = 0f
                minZ = 0f; maxZ = 0f
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Calibrate",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Footer
        Text(
            text = "Rotation rate measured by device gyroscope",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun AxisCircle(label: String, color: Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .border(
                width = 1.5.dp,
                color = color.copy(alpha = 0.4f),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun AxisRow(
    label: String,
    axisName: String,
    value: Float,
    color: Color,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = color,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = axisName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "%.2f".format(value),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "\u00b0/s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        // Progress bar showing magnitude relative to max range (~10 °/s)
        val progress = (abs(value) / 10f).coerceIn(0f, 1f)
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round,
        )
    }
}

@Composable
private fun MinMaxRow(axis: String, value: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = axis,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "%.2f".format(value),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SensorNotAvailable() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.ScreenRotation,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Gyroscope Sensor Not Available",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "This device does not have a gyroscope sensor.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
