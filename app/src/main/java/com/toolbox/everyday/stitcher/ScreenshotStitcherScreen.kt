package com.toolbox.everyday.stitcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.toolbox.core.media.MediaStoreWriter
import com.toolbox.core.sharing.ImageSharer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Toast

@Composable
fun ScreenshotStitcherScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var result by remember { mutableStateOf<Bitmap?>(null) }
    var working by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
        uris = it; result = null
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OutlinedButton(onClick = { picker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (uris.isEmpty()) "Pick screenshots (in order)" else "${uris.size} images selected")
        }
        Button(
            onClick = {
                working = true
                scope.launch {
                    result = withContext(Dispatchers.IO) { stitch(context, uris) }
                    working = false
                }
            },
            enabled = uris.size >= 2 && !working,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Stitch") }

        if (working) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.width(20.dp).height(20.dp)); Spacer(Modifier.width(8.dp)); Text("Stitching…")
            }
        }

        result?.let { bmp ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val msg = try {
                        val r = MediaStoreWriter.saveJpeg(context, bmp, "Toolbox", "stitch", 90)
                        "Saved to ${r.displayPath}"
                    } catch (e: Exception) { "Couldn't save: ${e.message}" }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }, modifier = Modifier.weight(1f)) { Text("Save") }
                OutlinedButton(onClick = { ImageSharer.shareBitmap(context, bmp, "stitch", "Share image") }, modifier = Modifier.weight(1f)) { Text("Share") }
            }
            Text("Preview", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Image(bitmap = bmp.asImageBitmap(), contentDescription = "Stitched image", modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Decode all images, scale each to a common width, and stack them vertically into one bitmap. */
private fun stitch(context: Context, uris: List<Uri>): Bitmap? {
    if (uris.isEmpty()) return null
    val bitmaps = uris.mapNotNull { uri ->
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
    }
    if (bitmaps.isEmpty()) return null
    val targetWidth = bitmaps.maxOf { it.width }.coerceAtMost(2000)
    val scaled = bitmaps.map { b ->
        if (b.width == targetWidth) b
        else Bitmap.createScaledBitmap(b, targetWidth, (b.height * targetWidth.toFloat() / b.width).toInt().coerceAtLeast(1), true)
    }
    val totalHeight = scaled.sumOf { it.height }
    val out = Bitmap.createBitmap(targetWidth, totalHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    var y = 0f
    for (b in scaled) {
        canvas.drawBitmap(b, 0f, y, null)
        y += b.height
    }
    return out
}
