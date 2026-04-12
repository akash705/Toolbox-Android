package com.toolbox.lighting.screenflash

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private data class FlashColor(val name: String, val color: Color)

private val flashColors = listOf(
    FlashColor("White", Color.White),
    FlashColor("Red", Color(0xFFFF1744)),
    FlashColor("Blue", Color(0xFF2979FF)),
    FlashColor("Green", Color(0xFF00E676)),
    FlashColor("Yellow", Color(0xFFFFEA00)),
    FlashColor("Orange", Color(0xFFFF9100)),
    FlashColor("Purple", Color(0xFFD500F9)),
)

private enum class FlashMode { Solid, Strobe, SOS }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScreenFlashScreen() {
    var isActive by rememberSaveable { mutableStateOf(false) }
    var selectedColorIndex by rememberSaveable { mutableStateOf(0) }
    var mode by rememberSaveable { mutableStateOf(FlashMode.Solid.name) }
    var strobeFreq by rememberSaveable { mutableFloatStateOf(5f) }
    var showWarning by rememberSaveable { mutableStateOf(false) }
    var pendingMode by remember { mutableStateOf<String?>(null) }
    var flashOn by remember { mutableStateOf(true) }

    val flashMode = FlashMode.valueOf(mode)
    val selectedColor = flashColors[selectedColorIndex].color
    val context = LocalContext.current
    val activity = context as? Activity

    // Brightness control
    DisposableEffect(isActive) {
        if (isActive && activity != null) {
            val lp = activity.window.attributes
            lp.screenBrightness = 1.0f
            activity.window.attributes = lp
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            if (activity != null) {
                val lp = activity.window.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                activity.window.attributes = lp
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    // Strobe / SOS animation
    LaunchedEffect(isActive, mode, strobeFreq) {
        if (!isActive) {
            flashOn = true
            return@LaunchedEffect
        }
        when (flashMode) {
            FlashMode.Solid -> flashOn = true
            FlashMode.Strobe -> {
                val intervalMs = (1000f / strobeFreq / 2).toLong()
                while (true) {
                    flashOn = true
                    delay(intervalMs)
                    flashOn = false
                    delay(intervalMs)
                }
            }
            FlashMode.SOS -> {
                val dot = 200L
                val dash = 600L
                val gap = 200L
                val letterGap = 600L
                val wordGap = 1400L
                while (true) {
                    // S: ...
                    repeat(3) { flashOn = true; delay(dot); flashOn = false; delay(gap) }
                    delay(letterGap)
                    // O: ---
                    repeat(3) { flashOn = true; delay(dash); flashOn = false; delay(gap) }
                    delay(letterGap)
                    // S: ...
                    repeat(3) { flashOn = true; delay(dot); flashOn = false; delay(gap) }
                    delay(wordGap)
                }
            }
        }
    }

    // Seizure warning dialog
    if (showWarning) {
        AlertDialog(
            onDismissRequest = { showWarning = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFA726)) },
            title = { Text("Seizure Warning") },
            text = { Text("Strobe/flashing lights may cause seizures in people with photosensitive epilepsy. Use with caution.") },
            confirmButton = {
                TextButton(onClick = {
                    showWarning = false
                    pendingMode?.let { mode = it }
                    pendingMode = null
                }) { Text("I Understand") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showWarning = false
                    pendingMode = null
                }) { Text("Cancel") }
            },
        )
    }

    if (isActive) {
        // Full-screen flash
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (flashOn) selectedColor else Color.Black)
                .clickable { isActive = false },
            contentAlignment = Alignment.Center,
        ) {
            if (!flashOn) {
                Text(
                    "Tap to stop",
                    color = Color.White.copy(alpha = 0.3f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    } else {
        // Settings UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Preview + start
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(selectedColor)
                    .clickable { isActive = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.FlashOn,
                    contentDescription = "Start",
                    tint = if (selectedColor == Color.White) Color.Black else Color.White,
                    modifier = Modifier.size(48.dp),
                )
            }

            Text(
                "Tap to activate",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Color picker card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Color",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        flashColors.forEachIndexed { index, fc ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(fc.color)
                                    .then(
                                        if (index == selectedColorIndex) {
                                            Modifier
                                                .clip(CircleShape)
                                                .background(fc.color)
                                                .padding(3.dp)
                                                .clip(CircleShape)
                                                .background(fc.color)
                                        } else Modifier
                                    )
                                    .clickable { selectedColorIndex = index },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (index == selectedColorIndex) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(fc.color),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Mode card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FlashMode.entries.forEach { fm ->
                            FilterChip(
                                selected = fm.name == mode,
                                onClick = {
                                    if (fm == FlashMode.Strobe || fm == FlashMode.SOS) {
                                        pendingMode = fm.name
                                        showWarning = true
                                    } else {
                                        mode = fm.name
                                    }
                                },
                                label = { Text(fm.name) },
                            )
                        }
                    }

                    if (flashMode == FlashMode.Strobe) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Frequency", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${strobeFreq.roundToInt()} Hz",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Slider(
                            value = strobeFreq,
                            onValueChange = { strobeFreq = it },
                            valueRange = 1f..20f,
                            steps = 18,
                        )
                    }
                }
            }
        }
    }
}
