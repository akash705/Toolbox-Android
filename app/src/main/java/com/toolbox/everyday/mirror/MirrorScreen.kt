package com.toolbox.everyday.mirror

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.toolbox.core.camera.CameraPreview
import com.toolbox.core.permission.PermissionGate
import java.io.File

private enum class ToneMode { Normal, Warm, Cool }
private enum class ActiveControl { Zoom, Light }

@Composable
fun MirrorScreen() {
    PermissionGate(
        permission = android.Manifest.permission.CAMERA,
        rationale = "The mirror needs camera access to show your reflection.",
    ) {
        MirrorContent()
    }
}

@Composable
private fun MirrorContent() {
    var isTrueMirror by remember { mutableStateOf(false) }
    var flashOn by remember { mutableStateOf(false) }
    var zoomLevel by remember { mutableFloatStateOf(1f) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var toneMode by remember { mutableStateOf(ToneMode.Normal) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var activeControl by remember { mutableStateOf<ActiveControl?>(null) }
    val context = LocalContext.current

    LaunchedEffect(zoomLevel, camera) {
        camera?.cameraControl?.setZoomRatio(zoomLevel)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // Camera preview area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            CameraPreview(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Default front camera is already mirrored.
                        // "True Mirror" flips it back so you see what others see.
                        if (isTrueMirror) scaleX = -1f
                    },
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
                onPreviewView = { pv -> previewView = pv },
                onCameraBound = { cam -> camera = cam },
            )

            // Brightness / flash overlay
            val overlayAlpha = when {
                flashOn -> 0.35f
                brightness > 0f -> brightness * 0.5f
                else -> 0f
            }
            if (overlayAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = overlayAlpha)),
                )
            }

            // Tone overlay
            when (toneMode) {
                ToneMode.Warm -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFF8C00).copy(alpha = 0.15f)),
                )
                ToneMode.Cool -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF4488FF).copy(alpha = 0.15f)),
                )
                ToneMode.Normal -> {}
            }

            // Mode badge — top right
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 12.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (isTrueMirror) "TRUE" else "MIRROR",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // Expandable slider for ZOOM or LIGHT
        AnimatedVisibility(
            visible = activeControl != null,
            enter = expandVertically(expandFrom = Alignment.Top),
            exit = shrinkVertically(shrinkTowards = Alignment.Top),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (activeControl) {
                    ActiveControl.Zoom -> {
                        Icon(
                            Icons.Default.ZoomOut,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Slider(
                            value = zoomLevel,
                            onValueChange = { zoomLevel = it },
                            valueRange = 1f..5f,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                        )
                        Icon(
                            Icons.Default.ZoomIn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    ActiveControl.Light -> {
                        Icon(
                            Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Slider(
                            value = brightness,
                            onValueChange = { brightness = it },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                        )
                        Icon(
                            Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    null -> {}
                }
            }
        }

        // Controls panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            // Row 1: mode chips + flash toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = !isTrueMirror,
                    onClick = { isTrueMirror = false },
                    label = { Text("Mirror") },
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = isTrueMirror,
                    onClick = { isTrueMirror = true },
                    label = { Text("True") },
                )
                Spacer(modifier = Modifier.weight(1f))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(end = 4.dp),
                ) {
                    Text(
                        text = "FLASH",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (flashOn) "On" else "Off",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (flashOn) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = { flashOn = !flashOn },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (flashOn) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHighest,
                            RoundedCornerShape(8.dp),
                        ),
                ) {
                    Icon(
                        imageVector = if (flashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = if (flashOn) "Flash On" else "Flash Off",
                        tint = if (flashOn) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 2: ZOOM, LIGHT, TONE, CAPTURE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                MirrorControlButton(
                    icon = Icons.Default.ZoomIn,
                    label = "ZOOM",
                    active = activeControl == ActiveControl.Zoom,
                    onClick = {
                        activeControl = if (activeControl == ActiveControl.Zoom) null else ActiveControl.Zoom
                    },
                )
                MirrorControlButton(
                    icon = Icons.Default.WbSunny,
                    label = "LIGHT",
                    active = activeControl == ActiveControl.Light,
                    onClick = {
                        activeControl = if (activeControl == ActiveControl.Light) null else ActiveControl.Light
                    },
                )
                MirrorControlButton(
                    icon = Icons.Default.Contrast,
                    label = "TONE",
                    active = toneMode != ToneMode.Normal,
                    onClick = {
                        toneMode = when (toneMode) {
                            ToneMode.Normal -> ToneMode.Warm
                            ToneMode.Warm -> ToneMode.Cool
                            ToneMode.Cool -> ToneMode.Normal
                        }
                    },
                )
                MirrorControlButton(
                    icon = Icons.Default.CameraAlt,
                    label = "CAPTURE",
                    active = false,
                    onClick = {
                        val bmp = previewView?.bitmap
                        if (bmp == null) {
                            Toast.makeText(context, "Capture unavailable", Toast.LENGTH_SHORT).show()
                            return@MirrorControlButton
                        }
                        saveMirrorCapture(context, bmp)
                    },
                )
            }
        }
    }
}

@Composable
private fun MirrorControlButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(52.dp)
                .background(
                    if (active) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                    RoundedCornerShape(12.dp),
                ),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

private fun saveMirrorCapture(context: Context, bitmap: Bitmap) {
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "Mirror_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Mirror")
            }
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues,
            ) ?: throw Exception("Failed to create media entry")
            context.contentResolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            Toast.makeText(context, "Saved to gallery", Toast.LENGTH_SHORT).show()
        } else {
            // API 26–28: save to app-specific storage and share so user can save manually
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: context.filesDir
            val file = File(dir, "Mirror_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Save capture"))
        }
    }.onFailure {
        Toast.makeText(context, "Capture failed", Toast.LENGTH_SHORT).show()
    }
}
