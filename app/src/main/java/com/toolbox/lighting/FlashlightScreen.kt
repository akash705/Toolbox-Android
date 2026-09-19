package com.toolbox.lighting

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private enum class FlashMode(val label: String) {
    Steady("Steady"),
    SOS("SOS"),
    Strobe("Strobe"),
}

// Safety + hardware ceiling: setTorchMode cannot reliably toggle faster than this, and staying
// below the 3-30 Hz photosensitive-seizure band keeps the strobe safer.
private const val STROBE_MAX_HZ = 10f

@Composable
fun FlashlightScreen() {
    val context = LocalContext.current
    var isOn by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(FlashMode.Steady) }
    var brightness by remember { mutableFloatStateOf(0.85f) }
    var strobeHz by remember { mutableFloatStateOf(5f) }
    var torchError by remember { mutableStateOf<String?>(null) }

    val prefs = remember { context.getSharedPreferences("flashlight", Context.MODE_PRIVATE) }
    var strobeAcknowledged by remember { mutableStateOf(prefs.getBoolean("strobe_ack", false)) }
    var showStrobeCaution by remember { mutableStateOf(false) }

    val cameraManager = remember { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    val cameraId = remember {
        cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }

    // Toggles the LED, distinguishing "no flash hardware" from "flash present but busy"
    // (held by the camera/another app), so R22 can show the right message + screen fallback.
    fun safeTorch(on: Boolean) {
        if (cameraId == null) return
        try {
            cameraManager.setTorchMode(cameraId, on)
            torchError = null
        } catch (_: android.hardware.camera2.CameraAccessException) {
            torchError = "The flashlight is in use by another app. Close it, or use Screen Flash instead."
        } catch (_: Exception) {
            torchError = "Couldn't control the flashlight right now."
        }
    }

    // Control the torch
    DisposableEffect(isOn, mode) {
        onDispose {
            try {
                if (cameraId != null) {
                    cameraManager.setTorchMode(cameraId, false)
                }
            } catch (_: Exception) {}
        }
    }

    // Torch control with mode patterns. SOS/Strobe run inside this LaunchedEffect, whose scope
    // survives the Activity's onStop — so they intentionally continue with the screen off. The
    // torch is released on nav-away/exit by the DisposableEffect above (R23).
    LaunchedEffect(isOn, mode, strobeHz, strobeAcknowledged) {
        if (cameraId == null) return@LaunchedEffect
        if (!isOn) {
            safeTorch(false)
            return@LaunchedEffect
        }

        when (mode) {
            FlashMode.Steady -> safeTorch(true)
            FlashMode.SOS -> {
                val dot = 200L
                val dash = 600L
                val gap = 200L
                val letterGap = 600L
                val wordGap = 1400L
                while (true) {
                    // S: ...
                    repeat(3) { safeTorch(true); delay(dot); safeTorch(false); delay(gap) }
                    delay(letterGap)
                    // O: ---
                    repeat(3) { safeTorch(true); delay(dash); safeTorch(false); delay(gap) }
                    delay(letterGap)
                    // S: ...
                    repeat(3) { safeTorch(true); delay(dot); safeTorch(false); delay(gap) }
                    delay(wordGap)
                }
            }
            FlashMode.Strobe -> {
                // Gate strobe behind the one-time photosensitivity acknowledgement (R21).
                if (!strobeAcknowledged) return@LaunchedEffect
                val halfPeriod = (1000f / (2f * strobeHz.coerceIn(1f, STROBE_MAX_HZ))).toLong()
                while (true) {
                    safeTorch(true); delay(halfPeriod)
                    safeTorch(false); delay(halfPeriod)
                }
            }
        }
    }

    val buttonScale by animateFloatAsState(
        targetValue = if (isOn) 1.05f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "scale",
    )
    val bgColor by animateColorAsState(
        targetValue = if (isOn) Color(0xFFFFF9C4) else Color.Transparent,
        label = "bg",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Power button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(180.dp)
                    .scale(buttonScale)
                    .clip(CircleShape)
                    .background(
                        if (isOn) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        isOn = !isOn
                    },
            ) {
                Icon(
                    Icons.Default.PowerSettingsNew,
                    contentDescription = if (isOn) "Turn off" else "Turn on",
                    modifier = Modifier.size(72.dp),
                    tint = if (isOn) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Status text
            Text(
                text = if (isOn) "FLASHLIGHT IS ON" else "FLASHLIGHT IS OFF",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = if (isOn) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Mode chips — inline, no card wrapper
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FlashMode.entries.forEach { flashMode ->
                    FilterChip(
                        selected = mode == flashMode,
                        onClick = {
                            mode = flashMode
                            if (flashMode == FlashMode.Strobe && !strobeAcknowledged) {
                                showStrobeCaution = true
                            }
                        },
                        label = { Text(flashMode.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Brightness slider card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Brightness",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${(brightness * 100).toInt()}%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = brightness,
                        onValueChange = { brightness = it },
                        valueRange = 0.1f..1f,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Strobe rate slider + safety note (only in Strobe mode).
            if (mode == FlashMode.Strobe) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Strobe rate", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("${strobeHz.toInt()} Hz", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = strobeHz,
                            onValueChange = { strobeHz = it },
                            valueRange = 1f..STROBE_MAX_HZ,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "Caution: flashing light may affect people with photosensitive epilepsy.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            torchError?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer text
            Text(
                text = if (cameraId == null) "No flashlight available on this device"
                else "Available on supported devices only",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (showStrobeCaution) {
            AlertDialog(
                onDismissRequest = {
                    showStrobeCaution = false
                    if (!strobeAcknowledged) mode = FlashMode.Steady
                },
                title = { Text("Photosensitivity warning") },
                text = {
                    Text(
                        "Strobe mode flashes the light rapidly. This may trigger seizures in people " +
                            "with photosensitive epilepsy. Point it away from faces and stop if anyone " +
                            "feels unwell.",
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        strobeAcknowledged = true
                        prefs.edit().putBoolean("strobe_ack", true).apply()
                        showStrobeCaution = false
                    }) { Text("I understand") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showStrobeCaution = false
                        mode = FlashMode.Steady
                    }) { Text("Cancel") }
                },
            )
        }
    }
}
