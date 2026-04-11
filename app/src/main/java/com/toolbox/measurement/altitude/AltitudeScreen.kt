package com.toolbox.measurement.altitude

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Landscape
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
import com.toolbox.core.permission.PermissionGate

private val AscentGreen = Color(0xFF4CAF50)
private val DescentRed = Color(0xFFF44336)
private val ChartBackground = Color(0xFF1E1E2E)
private val ChartLine = Color(0xFF4CAF50)
private val GpsGreen = Color(0xFF2E7D32)
private val GpsAmber = Color(0xFFF9A825)

@Composable
fun AltitudeScreen() {
    PermissionGate(
        permission = Manifest.permission.ACCESS_FINE_LOCATION,
        rationale = "Altitude measurement requires location access to determine elevation via GPS.",
    ) {
        AltitudeContent()
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun AltitudeContent() {
    val context = LocalContext.current

    var currentAltitude by remember { mutableDoubleStateOf(0.0) }
    var minAltitude by remember { mutableDoubleStateOf(Double.MAX_VALUE) }
    var maxAltitude by remember { mutableDoubleStateOf(Double.MIN_VALUE) }
    var gpsAccuracy by remember { mutableFloatStateOf(-1f) }
    var gpsReady by remember { mutableStateOf(false) }
    var isTracking by remember { mutableStateOf(false) }
    var totalAscent by remember { mutableDoubleStateOf(0.0) }
    var totalDescent by remember { mutableDoubleStateOf(0.0) }
    var lastAltitude by remember { mutableDoubleStateOf(Double.NaN) }
    var selectedUnit by remember { mutableIntStateOf(0) } // 0=meters, 1=feet
    val altitudeHistory = remember { mutableStateListOf<Float>() }

    // GPS location listener
    DisposableEffect(Unit) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                gpsReady = true
                gpsAccuracy = location.accuracy

                if (location.hasAltitude()) {
                    currentAltitude = location.altitude
                    if (currentAltitude < minAltitude) minAltitude = currentAltitude
                    if (currentAltitude > maxAltitude) maxAltitude = currentAltitude

                    if (isTracking) {
                        if (!lastAltitude.isNaN()) {
                            val diff = currentAltitude - lastAltitude
                            if (diff > 1.0) totalAscent += diff
                            else if (diff < -1.0) totalDescent += -diff
                        }
                        lastAltitude = currentAltitude
                    }

                    if (altitudeHistory.size > 100) altitudeHistory.removeAt(0)
                    altitudeHistory.add(currentAltitude.toFloat())
                }
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
                2000L, // 2 seconds
                0f,
                listener,
            )
        } catch (_: Exception) {}

        onDispose {
            locationManager.removeUpdates(listener)
        }
    }

    val unitLabel = if (selectedUnit == 0) "m" else "ft"
    fun convertAlt(meters: Double): String {
        val value = if (selectedUnit == 1) meters * 3.28084 else meters
        return if (value.isNaN() || value == Double.MAX_VALUE || value == Double.MIN_VALUE) "--"
        else "${value.toInt()}"
    }
    fun convertAltValue(meters: Double): String {
        val value = if (selectedUnit == 1) meters * 3.28084 else meters
        return "${value.toInt()}"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Mountain icon
        Icon(
            Icons.Default.Landscape,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Altitude display
        Text(
            text = convertAltValue(currentAltitude),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = if (selectedUnit == 0) "meters" else "feet",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Unit chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilterChip(
                selected = selectedUnit == 0,
                onClick = { selectedUnit = 0 },
                label = { Text("Meters", fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
            FilterChip(
                selected = selectedUnit == 1,
                onClick = { selectedUnit = 1 },
                label = { Text("Feet", fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Altitude chart
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            colors = CardDefaults.cardColors(containerColor = ChartBackground),
            shape = RoundedCornerShape(16.dp),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                val data = altitudeHistory.toList()
                if (data.size < 2) {
                    // Placeholder flat line
                    val midY = size.height / 2
                    drawLine(
                        Color.White.copy(alpha = 0.2f),
                        Offset(0f, midY),
                        Offset(size.width, midY),
                        strokeWidth = 1f,
                    )
                    return@Canvas
                }

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
                    color = ChartLine,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stats grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AltStatCard(
                label = "CURRENT",
                value = "${convertAltValue(currentAltitude)} $unitLabel",
                modifier = Modifier.weight(1f),
            )
            AltStatCard(
                label = "MIN",
                value = "${convertAlt(minAltitude)} $unitLabel",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AltStatCard(
                label = "MAX",
                value = "${convertAlt(maxAltitude)} $unitLabel",
                modifier = Modifier.weight(1f),
            )
            AltStatCard(
                label = "GPS ACCURACY",
                value = if (gpsReady && gpsAccuracy > 0) "±${gpsAccuracy.toInt()} m" else "-- m",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Elevation tracking card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                // Ascent
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = AscentGreen,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Total Ascent",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "${convertAltValue(totalAscent)} $unitLabel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AscentGreen,
                        )
                    }
                }

                // Descent
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ArrowDownward,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = DescentRed,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Total Descent",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "${convertAltValue(totalDescent)} $unitLabel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DescentRed,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Start/Stop tracking button
        Button(
            onClick = {
                if (isTracking) {
                    isTracking = false
                } else {
                    isTracking = true
                    lastAltitude = currentAltitude
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isTracking) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
            ),
        ) {
            Icon(
                imageVector = if (isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isTracking) "Stop Tracking" else "Start Tracking",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Reset button
        OutlinedButton(
            onClick = {
                minAltitude = Double.MAX_VALUE
                maxAltitude = Double.MIN_VALUE
                totalAscent = 0.0
                totalDescent = 0.0
                lastAltitude = Double.NaN
                isTracking = false
                altitudeHistory.clear()
            },
            modifier = Modifier
                .fillMaxWidth()
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

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Altitude readings depend on GPS signal quality",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun AltStatCard(
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
