package com.toolbox.measurement.bluetoothfinder

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp as lerpF
import com.toolbox.core.permission.PermissionGate
import com.toolbox.core.ui.LocalAccent
import com.toolbox.core.ui.PlayfulButton
import com.toolbox.core.ui.PlayfulEntrance
import com.toolbox.core.ui.ToolIntro
import com.toolbox.core.ui.playfulBorder
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private data class BleDevice(
    val address: String,
    val name: String?,
    val rssiEma: Float,
    val lastRaw: Int,
    val lastSeen: Long,
)

/** The permissions BLE scanning needs, which differ by platform version. */
private fun bleScanPermissions(): List<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        listOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

/** Maps a smoothed RSSI onto a 0f (very far) .. 1f (right here) "warmth" value. */
private fun warmth(ema: Float): Float = ((ema + 100f) / 60f).coerceIn(0f, 1f)

private data class Band(val label: String, val color: Color)

private fun bandFor(ema: Float): Band = when {
    ema > -55 -> Band("Right here!", Color(0xFFFF1744))
    ema > -65 -> Band("Very close", Color(0xFFFF7043))
    ema > -75 -> Band("Getting closer", Color(0xFFFFA000))
    ema > -85 -> Band("Nearby", Color(0xFF42A5F5))
    else -> Band("Far away", Color(0xFF5C6BC0))
}

@Composable
fun BluetoothFinderScreen() {
    PermissionGate(
        permissions = bleScanPermissions(),
        rationale = "Bluetooth Finder needs Bluetooth (and, on older phones, location) to scan for " +
            "nearby devices and guide you to them by signal strength.",
    ) {
        BluetoothFinderContent()
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun BluetoothFinderContent() {
    val accent = LocalAccent.current
    val context = LocalContext.current
    val adapter = remember {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }

    if (adapter == null) {
        InfoState(
            icon = Icons.Default.BluetoothDisabled,
            title = "No Bluetooth here",
            message = "This device doesn't have Bluetooth Low Energy, so the finder can't scan.",
        )
        return
    }

    var bluetoothOn by remember { mutableStateOf(adapter.isEnabled) }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    bluetoothOn = adapter.isEnabled
                }
            }
        }
        context.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    val enableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { bluetoothOn = adapter.isEnabled }

    if (!bluetoothOn) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Default.BluetoothDisabled,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Bluetooth is off",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Turn it on so we can hunt for your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            PlayfulButton(
                text = "Turn on Bluetooth",
                icon = Icons.Default.Bluetooth,
                onClick = {
                    runCatching { enableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) }
                },
            )
        }
        return
    }

    // --- Live scanning ---
    val devices = remember { mutableStateMapOf<String, BleDevice>() }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val callback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val addr = result.device?.address ?: return
                val advertised = result.scanRecord?.deviceName
                val bonded = try {
                    result.device.name
                } catch (_: SecurityException) {
                    null
                }
                val raw = result.rssi
                val now = System.currentTimeMillis()
                mainHandler.post {
                    val prev = devices[addr]
                    val ema = if (prev == null) raw.toFloat() else prev.rssiEma * 0.65f + raw * 0.35f
                    devices[addr] = BleDevice(
                        address = addr,
                        name = bonded ?: advertised ?: prev?.name,
                        rssiEma = ema,
                        lastRaw = raw,
                        lastSeen = now,
                    )
                }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { onScanResult(0, it) }
            }
        }
    }

    DisposableEffect(bluetoothOn) {
        val scanner = adapter.bluetoothLeScanner
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        runCatching { scanner?.startScan(null, settings, callback) }
        onDispose { runCatching { scanner?.stopScan(callback) } }
    }

    // Drop devices we haven't heard from in a while.
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            val cutoff = System.currentTimeMillis() - 10_000
            devices.keys.filter { (devices[it]?.lastSeen ?: 0) < cutoff }.forEach { devices.remove(it) }
        }
    }

    val sorted by remember { derivedStateOf { devices.values.sortedByDescending { it.rssiEma } } }
    val current = selected

    if (current != null) {
        FindMode(device = devices[current], onBack = { selected = null })
    } else {
        ScanList(devices = sorted, onSelect = { selected = it })
    }
}

@Composable
private fun ScanList(devices: List<BleDevice>, onSelect: (String) -> Unit) {
    PlayfulEntrance {
        Column(Modifier.fillMaxSize()) {
            ToolIntro(
                icon = Icons.Default.BluetoothSearching,
                title = "Bluetooth Finder",
                subtitle = "Tap a device, then walk around — it gets warmer as you get closer.",
            )
            RadarHero(deviceCount = devices.size)
            if (devices.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Sniffing the airwaves… make sure your device is on and out of its case.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
                ) {
                    items(devices, key = { it.address }) { d -> DeviceCard(d, onSelect) }
                }
            }
        }
    }
}

@Composable
private fun RadarHero(deviceCount: Int) {
    val accent = LocalAccent.current
    val transition = rememberInfiniteTransition(label = "radar")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep",
    )
    Box(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(160.dp)) {
            val r = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            for (k in 1..3) {
                drawCircle(
                    color = accent.copy(alpha = 0.18f),
                    radius = r * k / 3f,
                    center = center,
                    style = Stroke(width = 2.5f),
                )
            }
            rotate(sweep, center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0f to Color.Transparent,
                        0.25f to accent.copy(alpha = 0.55f),
                        0.30f to Color.Transparent,
                        1f to Color.Transparent,
                        center = center,
                    ),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = true,
                    topLeft = Offset(center.x - r, center.y - r),
                    size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                )
            }
        }
        Icon(
            Icons.Default.BluetoothSearching,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(44.dp),
        )
    }
}

@Composable
private fun DeviceCard(device: BleDevice, onSelect: (String) -> Unit) {
    val band = bandFor(device.rssiEma)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onSelect(device.address) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(band.color.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Bluetooth, contentDescription = null, tint = band.color)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = device.name ?: "Unknown device",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${device.address}  ·  ${device.lastRaw} dBm",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SignalBars(warmth(device.rssiEma), band.color)
        }
    }
}

@Composable
private fun SignalBars(warmth: Float, color: Color) {
    val lit = (warmth * 4f).toInt().coerceIn(0, 4)
    Row(verticalAlignment = Alignment.Bottom) {
        for (i in 1..4) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .width(6.dp)
                    .height((8 + i * 6).dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (i <= lit) color else color.copy(alpha = 0.18f)),
            )
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun FindMode(device: BleDevice?, onBack: () -> Unit) {
    val context = LocalContext.current
    val ema = device?.rssiEma ?: -100f
    val band = bandFor(ema)
    val t = warmth(ema)
    val tState = rememberUpdatedState(t)
    val emaState = rememberUpdatedState(ema)
    val present = device != null && ema > -100f

    val cold = Color(0xFF2979FF)
    val warm = Color(0xFFFFA000)
    val hot = Color(0xFFFF1744)
    val targetColor = if (t < 0.5f) lerp(cold, warm, t * 2f) else lerp(warm, hot, (t - 0.5f) * 2f)
    val morph by animateColorAsState(targetColor, tween(500), label = "hotcold")

    // Pulse rings that beat faster the closer you are.
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = (now - last) / 1_000_000_000f
                    val period = lerpF(1.7f, 0.45f, tState.value)
                    phase = (phase + dt / period) % 1f
                }
                last = now
            }
        }
    }

    // A little character that hops faster as it warms up.
    var bounce by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = (now - last) / 1_000_000_000f
                    val period = lerpF(1.1f, 0.4f, tState.value)
                    bounce = (bounce + dt / period) % 1f
                }
                last = now
            }
        }
    }
    val hopY = -abs(sin(bounce * PI.toFloat())) * lerpF(6f, 26f, t)

    // Warmer / colder trend.
    var trend by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            val before = emaState.value
            delay(1000)
            val diff = emaState.value - before
            trend = when {
                diff > 1.5f -> 1
                diff < -1.5f -> -1
                else -> 0
            }
        }
    }

    // Haptic ticks that quicken near the device.
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            val tv = tState.value
            if (emaState.value > -100f && tv > 0.08f) {
                val amplitude = (40 + tv * 180).toInt().coerceIn(1, 255)
                runCatching { vibrator.vibrate(VibrationEffect.createOneShot(28, amplitude)) }
            }
            delay(lerpF(950f, 130f, tv).toLong())
        }
    }

    val found = present && ema > -55f

    PlayfulEntrance {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = device?.name ?: "Searching…",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }

                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier.size(280.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val maxR = size.minDimension / 2f
                        val center = Offset(size.width / 2f, size.height / 2f)
                        for (k in 0..3) {
                            val p = (phase + k * 0.25f) % 1f
                            drawCircle(
                                color = morph.copy(alpha = (1f - p) * 0.5f),
                                radius = maxR * (0.25f + p * 0.75f),
                                center = center,
                                style = Stroke(width = 8f, cap = StrokeCap.Round),
                            )
                        }
                        drawCircle(morph.copy(alpha = 0.16f), radius = maxR * 0.34f, center = center)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (found) "🎉" else "🎧",
                            fontSize = 46.sp,
                            modifier = Modifier.graphicsLayer { translationY = hopY },
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (present) "${ema.toInt()}" else "—",
                            fontSize = 52.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = morph,
                        )
                        Text("dBm", style = MaterialTheme.typography.labelLarge, color = morph)
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = if (present) band.label else "Lost the signal — keep moving",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = morph,
                )

                Spacer(Modifier.height(8.dp))

                val (trendText, trendColor) = when {
                    !present -> "Searching…" to MaterialTheme.colorScheme.onSurfaceVariant
                    trend > 0 -> "🔥 Warmer — you're getting closer!" to hot
                    trend < 0 -> "❄️ Colder — try another direction" to cold
                    else -> "Hold steady…" to MaterialTheme.colorScheme.onSurfaceVariant
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(trendColor.copy(alpha = 0.14f))
                        .playfulBorder(trendColor, 50)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                ) {
                    Text(trendText, color = trendColor, fontWeight = FontWeight.Bold)
                }
            }

            AnimatedVisibility(
                visible = found,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp),
            ) {
                Text("Found it!", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = hot)
            }
            ConfettiBurst(active = found)
        }
    }
}

@Composable
private fun ConfettiBurst(active: Boolean) {
    val progress by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(1100),
        label = "confetti",
    )
    if (progress <= 0f) return
    val emojis = listOf("🎉", "✨", "🎊", "⭐", "💫", "🥳")
    val particles = remember {
        List(18) { i -> Triple((i * 41 % 360).toFloat(), 130f + (i * 19 % 110), emojis[i % emojis.size]) }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        particles.forEach { (angle, dist, emoji) ->
            val rad = Math.toRadians(angle.toDouble())
            Text(
                text = emoji,
                fontSize = 22.sp,
                modifier = Modifier.graphicsLayer {
                    translationX = cos(rad).toFloat() * dist * progress
                    translationY = sin(rad).toFloat() * dist * progress
                    alpha = 1f - progress
                    val s = 0.6f + progress
                    scaleX = s
                    scaleY = s
                },
            )
        }
    }
}

@Composable
private fun InfoState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = LocalAccent.current, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
