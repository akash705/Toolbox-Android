package com.toolbox.measurement.deviceinfo

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.view.WindowManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import java.io.File

@Composable
fun DeviceInfoScreen() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Battery state
    var batteryLevel by remember { mutableStateOf("--") }
    var batteryStatus by remember { mutableStateOf("--") }
    var batteryTemp by remember { mutableStateOf("--") }
    var batteryHealth by remember { mutableStateOf("--") }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                intent ?: return
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    batteryLevel = "${(level * 100 / scale)}%"
                }
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                batteryStatus = when (status) {
                    BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                    BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                    BatteryManager.BATTERY_STATUS_FULL -> "Full"
                    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
                    else -> "Unknown"
                }
                val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
                if (temp >= 0) batteryTemp = "${"%.1f".format(temp / 10.0)}°C"
                val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
                batteryHealth = when (health) {
                    BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                    BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                    BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                    BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                    else -> "Unknown"
                }
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { context.unregisterReceiver(receiver) }
    }

    // Static device info
    val generalInfo = remember {
        listOf(
            "Model" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "Device" to Build.DEVICE,
            "Android Version" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            "Build" to Build.DISPLAY,
            "Kernel" to System.getProperty("os.version").orEmpty(),
        )
    }

    val displayInfo = remember {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val metrics = context.resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        val refreshRate = display.refreshRate
        val modes = display.supportedModes
        val maxRefreshRate = modes.maxOfOrNull { it.refreshRate } ?: refreshRate

        val hdrCapabilities = if (Build.VERSION.SDK_INT >= 24) {
            val hdr = display.hdrCapabilities
            if (hdr != null && hdr.supportedHdrTypes.isNotEmpty()) {
                hdr.supportedHdrTypes.joinToString(", ") { type ->
                    when (type) {
                        1 -> "Dolby Vision"
                        2 -> "HDR10"
                        3 -> "HLG"
                        4 -> "HDR10+"
                        else -> "Type $type"
                    }
                }
            } else "Not supported"
        } else "N/A (API < 24)"

        listOf(
            "Resolution" to "${width} × ${height} px",
            "Density" to "${density} dpi (${metrics.density}x)",
            "Refresh Rate" to "${"%.0f".format(refreshRate)} Hz (max ${"%.0f".format(maxRefreshRate)} Hz)",
            "HDR Support" to hdrCapabilities,
        )
    }

    val storageInfo = remember {
        val internalStat = StatFs(Environment.getDataDirectory().path)
        val internalTotal = internalStat.totalBytes
        val internalFree = internalStat.availableBytes

        val externalDir = Environment.getExternalStorageDirectory()
        val externalStat = StatFs(externalDir.path)
        val externalTotal = externalStat.totalBytes
        val externalFree = externalStat.availableBytes

        listOf(
            "Internal Storage" to "${formatBytes(internalTotal - internalFree)} / ${formatBytes(internalTotal)}",
            "Internal Free" to formatBytes(internalFree),
            "External Storage" to "${formatBytes(externalTotal - externalFree)} / ${formatBytes(externalTotal)}",
            "External Free" to formatBytes(externalFree),
        )
    }

    val memoryInfo = remember {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        listOf(
            "Total RAM" to formatBytes(mi.totalMem),
            "Available RAM" to formatBytes(mi.availMem),
            "CPU Architecture" to Build.SUPPORTED_ABIS.joinToString(", "),
            "CPU Cores" to "${Runtime.getRuntime().availableProcessors()}",
        )
    }

    val batteryCapacity = remember {
        try {
            val file = File("/sys/class/power_supply/battery/charge_full_design")
            if (file.exists()) {
                val microAh = file.readText().trim().toLongOrNull()
                if (microAh != null) "${microAh / 1000} mAh" else "N/A"
            } else "N/A"
        } catch (_: Exception) {
            "N/A"
        }
    }

    val sensorList = remember {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sm.getSensorList(Sensor.TYPE_ALL).map { sensor ->
            sensor.name to "${sensor.vendor} (v${sensor.version})"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InfoSection("General", generalInfo, clipboardManager)
        InfoSection("Display", displayInfo, clipboardManager)
        InfoSection("Storage", storageInfo, clipboardManager)
        InfoSection("Memory & CPU", memoryInfo, clipboardManager)
        InfoSection(
            "Battery",
            listOf(
                "Level" to batteryLevel,
                "Status" to batteryStatus,
                "Health" to batteryHealth,
                "Temperature" to batteryTemp,
                "Capacity" to batteryCapacity,
            ),
            clipboardManager,
        )
        InfoSection("Sensors (${sensorList.size})", sensorList, clipboardManager)

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun InfoSection(
    title: String,
    items: List<Pair<String, String>>,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            items.forEachIndexed { index, (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { clipboardManager.setText(AnnotatedString(value)) }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(0.4f),
                    )
                    Text(
                        value,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(0.6f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    )
                }
                if (index < items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val gb = bytes / (1024.0 * 1024 * 1024)
    return if (gb >= 1.0) "${"%.1f".format(gb)} GB"
    else "${"%.0f".format(bytes / (1024.0 * 1024))} MB"
}
