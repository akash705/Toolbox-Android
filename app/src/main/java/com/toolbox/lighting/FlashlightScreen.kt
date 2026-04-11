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
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.delay

private enum class FlashMode(val label: String) {
    Steady("Steady"),
    SOS("SOS"),
    Strobe("Strobe"),
}

@Composable
fun FlashlightScreen() {
    val context = LocalContext.current
    var isOn by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(FlashMode.Steady) }

    val cameraManager = remember { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    val cameraId = remember {
        cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }

    // Control the torch
    DisposableEffect(isOn, mode) {
        onDispose {
            // Turn off when leaving
            try {
                if (cameraId != null) {
                    cameraManager.setTorchMode(cameraId, false)
                }
            } catch (_: Exception) {}
        }
    }

    // Steady mode
    LaunchedEffect(isOn, mode) {
        if (cameraId == null) return@LaunchedEffect
        if (!isOn) {
            try { cameraManager.setTorchMode(cameraId, false) } catch (_: Exception) {}
            return@LaunchedEffect
        }

        when (mode) {
            FlashMode.Steady -> {
                try { cameraManager.setTorchMode(cameraId, true) } catch (_: Exception) {}
            }
            FlashMode.SOS -> {
                // SOS pattern: ... --- ...
                val dot = 200L
                val dash = 600L
                val gap = 200L
                val letterGap = 600L
                val wordGap = 1400L
                while (true) {
                    // S: ...
                    repeat(3) {
                        cameraManager.setTorchMode(cameraId, true); delay(dot)
                        cameraManager.setTorchMode(cameraId, false); delay(gap)
                    }
                    delay(letterGap)
                    // O: ---
                    repeat(3) {
                        cameraManager.setTorchMode(cameraId, true); delay(dash)
                        cameraManager.setTorchMode(cameraId, false); delay(gap)
                    }
                    delay(letterGap)
                    // S: ...
                    repeat(3) {
                        cameraManager.setTorchMode(cameraId, true); delay(dot)
                        cameraManager.setTorchMode(cameraId, false); delay(gap)
                    }
                    delay(wordGap)
                }
            }
            FlashMode.Strobe -> {
                while (true) {
                    cameraManager.setTorchMode(cameraId, true); delay(100)
                    cameraManager.setTorchMode(cameraId, false); delay(100)
                }
            }
        }
    }

    val buttonScale by animateFloatAsState(
        targetValue = if (isOn) 1.1f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "scale",
    )
    val bgColor by animateColorAsState(
        targetValue = if (isOn) Color(0xFFFFF9C4) else Color.Transparent,
        label = "bg",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(0.3f))

        // Status text
        Text(
            text = if (isOn) "ON" else "OFF",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = if (isOn) Color(0xFFFF9800) else MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Big toggle button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(160.dp)
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
                if (isOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                contentDescription = if (isOn) "Turn off" else "Turn on",
                modifier = Modifier.size(64.dp),
                tint = if (isOn) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Mode selector
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
                Text(
                    text = "MODE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FlashMode.entries.forEach { flashMode ->
                        FilterChip(
                            selected = mode == flashMode,
                            onClick = { mode = flashMode },
                            label = { Text(flashMode.label) },
                        )
                    }
                }
            }
        }

        if (cameraId == null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No flashlight available on this device",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(modifier = Modifier.weight(0.5f))
    }
}
