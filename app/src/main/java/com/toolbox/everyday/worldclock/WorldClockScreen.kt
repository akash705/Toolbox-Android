package com.toolbox.everyday.worldclock

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val CITIES = listOf(
    "UTC" to "UTC",
    "London" to "Europe/London",
    "Paris" to "Europe/Paris",
    "Berlin" to "Europe/Berlin",
    "Moscow" to "Europe/Moscow",
    "New York" to "America/New_York",
    "Chicago" to "America/Chicago",
    "Los Angeles" to "America/Los_Angeles",
    "Toronto" to "America/Toronto",
    "São Paulo" to "America/Sao_Paulo",
    "Dubai" to "Asia/Dubai",
    "Mumbai" to "Asia/Kolkata",
    "Karachi" to "Asia/Karachi",
    "Dhaka" to "Asia/Dhaka",
    "Bangkok" to "Asia/Bangkok",
    "Singapore" to "Asia/Singapore",
    "Hong Kong" to "Asia/Hong_Kong",
    "Shanghai" to "Asia/Shanghai",
    "Tokyo" to "Asia/Tokyo",
    "Seoul" to "Asia/Seoul",
    "Sydney" to "Australia/Sydney",
    "Auckland" to "Pacific/Auckland",
    "Honolulu" to "Pacific/Honolulu",
    "Cairo" to "Africa/Cairo",
    "Johannesburg" to "Africa/Johannesburg",
    "Lagos" to "Africa/Lagos",
)

@Composable
fun WorldClockScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("world_clock", Context.MODE_PRIVATE) }
    var zones by remember { mutableStateOf(loadZones(prefs)) }
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    var menuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            now = ZonedDateTime.now()
            delay(1000)
        }
    }

    fun save(newZones: List<String>) {
        zones = newZones
        prefs.edit().putString("zones", newZones.joinToString("|")).apply()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Box {
            Button(onClick = { menuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add city")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                CITIES.filter { it.second !in zones }.forEach { (city, zoneId) ->
                    DropdownMenuItem(
                        text = { Text(city) },
                        onClick = { save(zones + zoneId); menuOpen = false },
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        if (zones.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No cities added yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(zones, key = { it }) { zoneId ->
                    ClockRow(zoneId = zoneId, now = now, onRemove = { save(zones - zoneId) })
                }
            }
        }
    }
}

@Composable
private fun ClockRow(zoneId: String, now: ZonedDateTime, onRemove: () -> Unit) {
    val zone = remember(zoneId) { runCatching { ZoneId.of(zoneId) }.getOrDefault(ZoneId.systemDefault()) }
    val zdt = now.withZoneSameInstant(zone)
    val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm:ss") }
    val dateFmt = remember { DateTimeFormatter.ofPattern("EEE, d MMM") }
    val cityName = CITIES.firstOrNull { it.second == zoneId }?.first ?: zoneId

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(cityName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(zdt.format(dateFmt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(zdt.format(timeFmt), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = onRemove) { Icon(Icons.Default.Close, contentDescription = "Remove") }
        }
    }
}

private fun loadZones(prefs: android.content.SharedPreferences): List<String> {
    val stored = prefs.getString("zones", null)
    return if (stored == null) {
        listOf(ZoneId.systemDefault().id, "UTC", "America/New_York")
    } else {
        stored.split("|").filter { it.isNotBlank() }
    }
}
