package com.toolbox.everyday.colorpicker

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.Camera
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toolbox.core.camera.CameraPreview
import com.toolbox.core.permission.PermissionGate
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun ColorPickerScreen() {
    PermissionGate(
        permission = android.Manifest.permission.CAMERA,
        rationale = "The color picker needs camera access to detect colors.",
    ) {
        ColorPickerContent()
    }
}

@Composable
private fun ColorPickerContent() {
    var centerColor by remember { mutableIntStateOf(0xFFFFFFFF.toInt()) }
    val colorHistory = remember { mutableStateListOf<Int>() }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torchOn by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val analyzer = remember {
        CenterColorAnalyzer { color ->
            centerColor = color
        }
    }

    val r = (centerColor shr 16) and 0xFF
    val g = (centerColor shr 8) and 0xFF
    val b = centerColor and 0xFF
    val hex = "#%02X%02X%02X".format(r, g, b)
    val composeColor = Color(r / 255f, g / 255f, b / 255f)
    val hsl = rgbToHsl(r, g, b)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        // Camera preview with crosshair
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(16.dp)),
        ) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                imageAnalyzer = analyzer,
                onCameraBound = { cam -> camera = cam },
            )

            // Crosshair overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2
                val cy = size.height / 2
                val crosshairSize = 30f
                val ringRadius = 24f

                // Outer ring with picked color
                drawCircle(
                    color = composeColor,
                    radius = ringRadius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 4f),
                )

                // Crosshair lines
                drawLine(Color.White, Offset(cx - crosshairSize, cy), Offset(cx - 8, cy), strokeWidth = 2f)
                drawLine(Color.White, Offset(cx + 8, cy), Offset(cx + crosshairSize, cy), strokeWidth = 2f)
                drawLine(Color.White, Offset(cx, cy - crosshairSize), Offset(cx, cy - 8), strokeWidth = 2f)
                drawLine(Color.White, Offset(cx, cy + 8), Offset(cx, cy + crosshairSize), strokeWidth = 2f)
            }

            // Torch toggle
            IconButton(
                onClick = {
                    torchOn = !torchOn
                    camera?.cameraControl?.enableTorch(torchOn)
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            ) {
                Icon(
                    imageVector = if (torchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = if (torchOn) "Flash on" else "Flash off",
                    tint = Color.White,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Color info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Color swatch
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(composeColor)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = hex,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text = "RGB($r, $g, $b)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text = "HSL(${hsl.first}°, ${hsl.second}%, ${hsl.third}%)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                IconButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Color", hex))
                }) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy hex",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recent colors header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Recent Colors",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(
                onClick = {
                    if (colorHistory.isEmpty() || colorHistory.last() != centerColor) {
                        colorHistory.add(centerColor)
                        if (colorHistory.size > 20) colorHistory.removeAt(0)
                    }
                },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Save color",
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (colorHistory.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(colorHistory.reversed()) { color ->
                    val cR = (color shr 16) and 0xFF
                    val cG = (color shr 8) and 0xFF
                    val cB = color and 0xFF
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(cR / 255f, cG / 255f, cB / 255f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val h = "#%02X%02X%02X".format(cR, cG, cB)
                                clipboard.setPrimaryClip(ClipData.newPlainText("Color", h))
                            },
                    )
                }
            }
        } else {
            Text(
                text = "Tap + to save the current color",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun rgbToHsl(r: Int, g: Int, b: Int): Triple<Int, Int, Int> {
    val rf = r / 255f
    val gf = g / 255f
    val bf = b / 255f
    val cMax = max(rf, max(gf, bf))
    val cMin = min(rf, min(gf, bf))
    val delta = cMax - cMin

    val l = (cMax + cMin) / 2f

    if (delta == 0f) return Triple(0, 0, (l * 100).toInt())

    val s = if (l < 0.5f) delta / (cMax + cMin) else delta / (2f - cMax - cMin)

    val h = when (cMax) {
        rf -> 60f * (((gf - bf) / delta) % 6f)
        gf -> 60f * (((bf - rf) / delta) + 2f)
        else -> 60f * (((rf - gf) / delta) + 4f)
    }

    return Triple(
        ((h + 360) % 360).toInt(),
        (s * 100).toInt(),
        (l * 100).toInt(),
    )
}

private class CenterColorAnalyzer(
    private val onColorDetected: (Int) -> Unit,
) : ImageAnalysis.Analyzer {
    private var frameCount = 0

    override fun analyze(image: ImageProxy) {
        frameCount++
        if (frameCount % 5 != 0) {
            image.close()
            return
        }

        try {
            val bitmap = image.toBitmap()
            val cx = bitmap.width / 2
            val cy = bitmap.height / 2

            var rSum = 0L
            var gSum = 0L
            var bSum = 0L
            var count = 0
            val sampleSize = 5

            for (dx in -sampleSize..sampleSize) {
                for (dy in -sampleSize..sampleSize) {
                    val x = (cx + dx).coerceIn(0, bitmap.width - 1)
                    val y = (cy + dy).coerceIn(0, bitmap.height - 1)
                    val pixel = bitmap.getPixel(x, y)
                    rSum += (pixel shr 16) and 0xFF
                    gSum += (pixel shr 8) and 0xFF
                    bSum += pixel and 0xFF
                    count++
                }
            }

            val r = (rSum / count).toInt()
            val g = (gSum / count).toInt()
            val b = (bSum / count).toInt()
            val color = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            onColorDetected(color)
        } catch (_: Exception) {
        } finally {
            image.close()
        }
    }
}
