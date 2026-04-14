package com.toolbox.measurement.networkinfo

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.toolbox.core.permission.PermissionGate
import com.toolbox.core.ui.components.ShimmerBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.URL

@Composable
fun NetworkInfoScreen() {
    PermissionGate(
        permission = Manifest.permission.ACCESS_FINE_LOCATION,
        rationale = "Network Info needs location permission to read Wi-Fi details like SSID and BSSID on Android 8+.",
    ) {
        NetworkInfoContent()
    }
}

@Composable
private fun NetworkInfoContent() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var connectionType by remember { mutableStateOf("Checking...") }
    var wifiInfo by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var ipInfo by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var mobileInfo by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var publicIp by remember { mutableStateOf("Fetching...") }
    var publicIpError by remember { mutableStateOf(false) }

    fun refreshNetworkInfo() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val caps = network?.let { cm.getNetworkCapabilities(it) }
        val linkProps = network?.let { cm.getLinkProperties(it) }

        // Connection type
        connectionType = when {
            caps == null -> "Not connected"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "Other"
        }

        // Wi-Fi info
        if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val info = wm.connectionInfo
            val ssid = info.ssid?.removePrefix("\"")?.removeSuffix("\"") ?: "Unknown"
            val bssid = info.bssid ?: "Unknown"
            val rssi = info.rssi
            val freq = info.frequency
            val linkSpeed = info.linkSpeed
            val channel = frequencyToChannel(freq)

            wifiInfo = listOf(
                "SSID" to ssid,
                "BSSID" to bssid,
                "Signal Strength" to "$rssi dBm",
                "Frequency" to "$freq MHz",
                "Channel" to "$channel",
                "Link Speed" to "$linkSpeed Mbps",
            )
        } else {
            wifiInfo = emptyList()
        }

        // IP addresses
        val ips = mutableListOf<Pair<String, String>>()
        linkProps?.linkAddresses?.forEach { la ->
            when (val addr = la.address) {
                is Inet4Address -> ips.add("IPv4" to addr.hostAddress.orEmpty())
                is Inet6Address -> ips.add("IPv6" to addr.hostAddress.orEmpty())
            }
        }
        ipInfo = ips

        // Mobile info
        if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val carrier = tm.networkOperatorName.takeIf { it.isNotEmpty() } ?: "Unknown"
            val networkTypeStr = try {
                @Suppress("DEPRECATION")
                when (tm.dataNetworkType) {
                    TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
                    TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
                    TelephonyManager.NETWORK_TYPE_HSDPA,
                    TelephonyManager.NETWORK_TYPE_HSUPA,
                    TelephonyManager.NETWORK_TYPE_HSPA,
                    TelephonyManager.NETWORK_TYPE_HSPAP -> "3G HSPA"
                    TelephonyManager.NETWORK_TYPE_UMTS -> "3G UMTS"
                    TelephonyManager.NETWORK_TYPE_EDGE -> "2G EDGE"
                    TelephonyManager.NETWORK_TYPE_GPRS -> "2G GPRS"
                    else -> "Unknown"
                }
            } catch (_: SecurityException) {
                "Permission required"
            }
            mobileInfo = listOf(
                "Carrier" to carrier,
                "Network Type" to networkTypeStr,
            )
        } else {
            mobileInfo = emptyList()
        }
    }

    LaunchedEffect(Unit) {
        refreshNetworkInfo()
    }

    // Fetch public IP
    LaunchedEffect(Unit) {
        publicIp = fetchPublicIp()
        publicIpError = publicIp == "Unavailable"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Connection status card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
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
                        "Connection",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    if (connectionType == "Checking...") {
                        ShimmerBox(
                            height = 28.dp,
                            modifier = Modifier.width(140.dp).padding(top = 4.dp),
                        )
                    } else {
                        Text(
                            connectionType,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                IconButton(onClick = {
                    refreshNetworkInfo()
                    publicIp = "Fetching..."
                    publicIpError = false
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        }

        // Wi-Fi details
        if (wifiInfo.isNotEmpty()) {
            InfoSection("Wi-Fi Details", wifiInfo, clipboardManager)
        }

        // IP addresses
        val allIps = buildList {
            addAll(ipInfo)
            add("Public IP" to publicIp)
        }
        InfoSection("IP Addresses", allIps, clipboardManager)

        if (publicIpError) {
            OutlinedButton(
                onClick = {
                    publicIp = "Fetching..."
                    publicIpError = false
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Retry Public IP")
            }
            // Re-fetch on retry
            LaunchedEffect(publicIpError) {
                if (!publicIpError) {
                    publicIp = fetchPublicIp()
                    publicIpError = publicIp == "Unavailable"
                }
            }
        }

        // Mobile info
        if (mobileInfo.isNotEmpty()) {
            InfoSection("Mobile Network", mobileInfo, clipboardManager)
        }

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
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(0.35f),
                    )
                    if (value == "Fetching..." || value == "Checking...") {
                        Box(modifier = Modifier.weight(0.55f), contentAlignment = Alignment.CenterEnd) {
                            ShimmerBox(
                                height = 14.dp,
                                modifier = Modifier.width(100.dp),
                            )
                        }
                    } else {
                        Text(
                            value,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(0.55f),
                            textAlign = TextAlign.End,
                        )
                    }
                    IconButton(
                        onClick = { clipboardManager.setText(AnnotatedString(value)) },
                        modifier = Modifier.weight(0.1f),
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            modifier = Modifier.padding(0.dp),
                        )
                    }
                }
                if (index < items.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

private fun frequencyToChannel(freqMhz: Int): Int = when {
    freqMhz in 2412..2484 -> (freqMhz - 2412) / 5 + 1
    freqMhz in 5170..5825 -> (freqMhz - 5170) / 5 + 34
    freqMhz in 5955..7115 -> (freqMhz - 5955) / 5 + 1  // 6 GHz
    else -> 0
}

private suspend fun fetchPublicIp(): String {
    return try {
        withContext(Dispatchers.IO) {
            val connection = URL("https://api.ipify.org").openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.getInputStream().bufferedReader().readText().trim()
        }
    } catch (_: Exception) {
        "Unavailable"
    }
}
