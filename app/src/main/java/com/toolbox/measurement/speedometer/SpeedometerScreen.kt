package com.toolbox.measurement.speedometer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
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
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.core.permission.PermissionGate
import kotlin.math.cos
import kotlin.math.sin
import com.toolbox.core.sharing.ShareButton

private val GaugeGreen = Color(0xFF4CAF50)
private val GaugeYellow = Color(0xFFFFC107)
private val GaugeOrange = Color(0xFFFF9800)
private val GaugeRed = Color(0xFFF44336)
private val GpsGreen = Color(0xFF2E7D32)
private val GpsAmber = Color(0xFFF9A825)

@Composable
fun SpeedometerScreen() {
    PermissionGate(
        permission = Manifest.permission.ACCESS_FINE_LOCATION,
        rationale = "Speed measurement requires location access to determine your velocity via GPS.",
    ) {
        SpeedometerContent()
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun SpeedometerContent() {
    val context = LocalContext.current

    var currentSpeed by remember { mutableFloatStateOf(0f) } // m/s
    var maxSpeed by remember { mutableFloatStateOf(0f) }
    var totalSpeed by remember { mutableDoubleStateOf(0.0) }
    var speedSamples by remember { mutableIntStateOf(0) }
    var distance by remember { mutableDoubleStateOf(0.0) } // meters
    var lastLocation by remember { mutableStateOf<Location?>(null) }
    var gpsAccuracy by remember { mutableFloatStateOf(-1f) }
    var gpsReady by remember { mutableStateOf(false) }
    var isTripActive by remember { mutableStateOf(false) }
    var tripStartTime by remember { mutableLongStateOf(0L) }
    var selectedUnit by remember { mutableIntStateOf(0) } // 0=km/h, 1=mph, 2=m/s, 3=knots

    // GPS location listener
    DisposableEffect(Unit) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                gpsReady = true
                gpsAccuracy = location.accuracy

                if (location.hasSpeed()) {
                    currentSpeed = location.speed // m/s
                    if (currentSpeed > maxSpeed) maxSpeed = currentSpeed
                    totalSpeed += currentSpeed
                    speedSamples++
                }

                if (isTripActive && lastLocation != null) {
                    distance += location.distanceTo(lastLocation!!)
                }
                lastLocation = location
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {
                gpsReady = false
            }
        }

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L, // 1 second
                0f,    // 0 meters
                listener,
            )
        } catch (_: Exception) {}

        onDispose {
            locationManager.removeUpdates(listener)
        }
    }

    val avgSpeed = if (speedSamples > 0) (totalSpeed / speedSamples).toFloat() else 0f

    // Unit conversion
    val unitLabels = listOf("km/h", "mph", "m/s", "knots")
    fun convertSpeed(mps: Float): Float = when (selectedUnit) {
        0 -> mps * 3.6f        // km/h
        1 -> mps * 2.23694f    // mph
        2 -> mps               // m/s
        3 -> mps * 1.94384f    // knots
        else -> mps * 3.6f
    }
    fun formatSpeed(mps: Float): String {
        val converted = convertSpeed(mps)
        return if (converted < 10f) String.format("%.1f", converted)
        else "${converted.toInt()}"
    }
    fun formatDistance(meters: Double): String {
        return when (selectedUnit) {
            1 -> String.format("%.1f mi", meters / 1609.34) // miles
            else -> {
                if (meters >= 1000) String.format("%.1f km", meters / 1000.0)
                else String.format("%.0f m", meters)
            }
        }
    }

    val displaySpeed = convertSpeed(currentSpeed)
    val maxGaugeSpeed = when (selectedUnit) {
        0 -> 200f  // km/h
        1 -> 120f  // mph
        2 -> 55f   // m/s
        3 -> 100f  // knots
        else -> 200f
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Speedometer gauge with speed value overlaid inside
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawSpeedGauge(
                    speed = displaySpeed,
                    maxSpeed = maxGaugeSpeed,
                )
            }
            // Speed value overlaid inside the gauge
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                Text(
                    text = if (displaySpeed < 10f) String.format("%.1f", displaySpeed)
                    else "${displaySpeed.toInt()}",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = unitLabels[selectedUnit].uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // GPS status badge
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (gpsReady) GpsGreen.copy(alpha = 0.1f) else GpsAmber.copy(alpha = 0.1f),
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                text = if (gpsReady) "● GPS READY" else "● ACQUIRING GPS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (gpsReady) GpsGreen else GpsAmber,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Unit selector chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            unitLabels.forEachIndexed { index, label ->
                FilterChip(
                    selected = selectedUnit == index,
                    onClick = { selectedUnit = index },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stats grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SpeedStatCard(
                label = "CURRENT SPEED",
                value = "${formatSpeed(currentSpeed)} ${unitLabels[selectedUnit]}",
                modifier = Modifier.weight(1f),
            )
            SpeedStatCard(
                label = "MAX SPEED",
                value = "${formatSpeed(maxSpeed)} ${unitLabels[selectedUnit]}",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SpeedStatCard(
                label = "AVERAGE",
                value = "${formatSpeed(avgSpeed)} ${unitLabels[selectedUnit]}",
                modifier = Modifier.weight(1f),
            )
            SpeedStatCard(
                label = "DISTANCE",
                value = formatDistance(distance),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // GPS Accuracy card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.GpsFixed,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (gpsReady) GpsGreen else GpsAmber,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "GPS ACCURACY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp,
                    )
                    Text(
                        text = if (gpsReady && gpsAccuracy > 0) "±${gpsAccuracy.toInt()}m"
                        else "Waiting for signal...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (gpsReady) MaterialTheme.colorScheme.onSurface else GpsAmber,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Trip buttons
        Button(
            onClick = {
                if (isTripActive) {
                    isTripActive = false
                } else {
                    isTripActive = true
                    tripStartTime = System.currentTimeMillis()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isTripActive) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
            ),
        ) {
            Icon(
                imageVector = if (isTripActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isTripActive) "Stop Trip" else "Start Trip",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = {
                    currentSpeed = 0f
                    maxSpeed = 0f
                    totalSpeed = 0.0
                    speedSamples = 0
                    distance = 0.0
                    lastLocation = null
                    isTripActive = false
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Reset",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            ShareButton(
                toolName = "Speedometer",
                value = if (displaySpeed < 10f) String.format("%.1f", displaySpeed) else "${displaySpeed.toInt()}",
                unit = unitLabels[selectedUnit],
                modifier = Modifier.height(48.dp),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Speed readings depend on GPS accuracy and device\npositioning. Actual speed may vary.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SpeedStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 12.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun DrawScope.drawSpeedGauge(
    speed: Float,
    maxSpeed: Float,
) {
    val centerX = size.width / 2
    val centerY = size.height
    val radius = (size.width / 2 - 32f).coerceAtMost(size.height - 24f)

    val arcLeft = centerX - radius
    val arcTop = centerY - radius
    val arcSize = Size(radius * 2, radius * 2)

    val startAngle = 180f
    val totalSweep = 180f
    val strokeWidth = 30f

    // Background track
    drawArc(
        color = Color(0xFFE0E0E0),
        startAngle = startAngle,
        sweepAngle = totalSweep,
        useCenter = false,
        topLeft = Offset(arcLeft, arcTop),
        size = arcSize,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
    )

    // Colored segments with round caps for smooth, curvy look
    val segments = listOf(
        GaugeGreen to 0.25f,
        Color(0xFF8BC34A) to 0.15f,
        GaugeYellow to 0.15f,
        GaugeOrange to 0.20f,
        GaugeRed to 0.25f,
    )

    var drawnSweep = 0f
    for ((color, fraction) in segments) {
        val segmentSweep = totalSweep * fraction
        drawArc(
            color = color,
            startAngle = startAngle + drawnSweep,
            sweepAngle = segmentSweep,
            useCenter = false,
            topLeft = Offset(arcLeft, arcTop),
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        drawnSweep += segmentSweep
    }

    // Needle
    val progress = (speed / maxSpeed).coerceIn(0f, 1f)
    val needleAngle = startAngle + totalSweep * progress
    val needleRad = Math.toRadians(needleAngle.toDouble())
    val needleLength = radius - 40f

    val needleTipX = centerX + needleLength * cos(needleRad).toFloat()
    val needleTipY = centerY + needleLength * sin(needleRad).toFloat()

    val needleColor = Color(0xFF37474F)

    drawLine(
        color = needleColor,
        start = Offset(centerX, centerY),
        end = Offset(needleTipX, needleTipY),
        strokeWidth = 3f,
        cap = StrokeCap.Round,
    )

    // Needle pivot circle
    drawCircle(
        color = needleColor,
        radius = 8f,
        center = Offset(centerX, centerY),
    )
    drawCircle(
        color = Color.White,
        radius = 4f,
        center = Offset(centerX, centerY),
    )

    // No endpoint labels - matches design
}
