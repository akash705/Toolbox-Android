package com.toolbox.measurement.wifisignal

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toolbox.core.permission.PermissionGate
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private data class WifiReading(
    val rssi: Int,
    val ssid: String,
    val label: String,
    val timestamp: String,
    val linkSpeed: Int,
    val frequency: Int,
)

@Composable
fun WifiSignalScreen() {
    PermissionGate(
        permission = Manifest.permission.ACCESS_FINE_LOCATION,
        rationale = "WiFi Signal needs location permission to read WiFi information on Android 8+.",
    ) {
        val context = LocalContext.current
        val wifiManager = remember {
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        }

        var rssi by remember { mutableIntStateOf(-100) }
        var ssid by remember { mutableStateOf("--") }
        var linkSpeed by remember { mutableIntStateOf(0) }
        var frequency by remember { mutableIntStateOf(0) }
        var roomLabel by rememberSaveable { mutableStateOf("") }
        val readings = remember { mutableStateListOf<WifiReading>() }

        // Poll WiFi info
        LaunchedEffect(Unit) {
            while (true) {
                try {
                    @Suppress("DEPRECATION")
                    val info = wifiManager.connectionInfo
                    rssi = info.rssi
                    @Suppress("DEPRECATION")
                    ssid = info.ssid?.removePrefix("\"")?.removeSuffix("\"") ?: "--"
                    linkSpeed = info.linkSpeed
                    frequency = info.frequency
                } catch (_: Exception) {}
                delay(1000)
            }
        }

        val signalPercent = WifiManager.calculateSignalLevel(rssi, 100)
        val qualityLabel = when {
            rssi > -50 -> "Excellent"
            rssi > -60 -> "Good"
            rssi > -70 -> "Fair"
            rssi > -80 -> "Weak"
            else -> "Poor"
        }
        val qualityColor = when {
            rssi > -50 -> Color(0xFF2E7D32)
            rssi > -60 -> Color(0xFF66BB6A)
            rssi > -70 -> Color(0xFFFFA726)
            rssi > -80 -> Color(0xFFEF5350)
            else -> Color(0xFFB71C1C)
        }
        val band = if (frequency > 4000) "5 GHz" else "2.4 GHz"

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Signal strength card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Default.Wifi,
                        contentDescription = null,
                        tint = qualityColor,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = ssid,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$rssi dBm",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = qualityColor,
                    )
                    Text(
                        text = qualityLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = qualityColor,
                        fontWeight = FontWeight.SemiBold,
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { signalPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = qualityColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round,
                    )
                }
            }

            // Details card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    StatRow(Icons.Default.SignalWifi4Bar, "Signal Quality", "$signalPercent%")
                    Spacer(modifier = Modifier.height(8.dp))
                    StatRow(Icons.Default.Speed, "Link Speed", "$linkSpeed Mbps")
                    Spacer(modifier = Modifier.height(8.dp))
                    StatRow(Icons.Default.NetworkWifi, "Frequency", "$frequency MHz ($band)")
                }
            }

            // Log reading card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Log Reading",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = roomLabel,
                        onValueChange = { roomLabel = it },
                        label = { Text("Room / Location label") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                            readings.add(
                                WifiReading(rssi, ssid, roomLabel.ifBlank { "Unnamed" }, timestamp, linkSpeed, frequency)
                            )
                            roomLabel = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log Current Reading")
                    }
                }
            }

            // Logged readings
            if (readings.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Logged Readings (${readings.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Row {
                                IconButton(onClick = {
                                    val csv = buildString {
                                        appendLine("Time,Location,SSID,RSSI (dBm),Link Speed (Mbps),Frequency (MHz)")
                                        readings.forEach { r ->
                                            appendLine("${r.timestamp},${r.label},${r.ssid},${r.rssi},${r.linkSpeed},${r.frequency}")
                                        }
                                    }
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, csv)
                                        putExtra(Intent.EXTRA_SUBJECT, "WiFi Signal Readings")
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Readings"))
                                }) {
                                    Icon(Icons.Default.Share, contentDescription = "Export", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { readings.clear() }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Clear", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        readings.forEachIndexed { index, reading ->
                            if (index > 0) Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        text = reading.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = reading.timestamp,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    text = "${reading.rssi} dBm",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when {
                                        reading.rssi > -50 -> Color(0xFF2E7D32)
                                        reading.rssi > -60 -> Color(0xFF66BB6A)
                                        reading.rssi > -70 -> Color(0xFFFFA726)
                                        else -> Color(0xFFEF5350)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}
