package com.toolbox.everyday.pdftoolkit

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.toolbox.core.pdf.PdfOps
import com.toolbox.core.pdf.SignatureRender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PdfSignSection() {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var uri by remember { mutableStateOf<Uri?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    var page by remember { mutableIntStateOf(0) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    var mode by remember { mutableIntStateOf(0) } // 0 draw, 1 type, 2 import
    var signature by remember { mutableStateOf<Bitmap?>(null) }

    // Placement (in page-preview px coordinates)
    var offset by remember { mutableStateOf(Offset(60f, 60f)) }
    var scale by remember { mutableFloatStateOf(1f) }

    var working by remember { mutableStateOf(false) }
    var resultFile by remember { mutableStateOf<File?>(null) }

    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        uri = it; page = 0; signature = null; resultFile = null
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { imgUri ->
        imgUri?.let { signature = SignatureRender.fromUri(context, it) }
    }

    androidx.compose.runtime.LaunchedEffect(uri, page) {
        val u = uri ?: return@LaunchedEffect
        pageCount = withContext(Dispatchers.IO) { PdfOps.pageCount(context, u) }
        pageBitmap = withContext(Dispatchers.IO) { PdfOps.renderPage(context, u, page, 1080) }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { pickPdf.launch(arrayOf("application/pdf")) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (uri == null) "Pick a PDF to sign" else "PDF selected")
        }

        if (uri != null) {
            if (pageCount > 1) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { if (page > 0) page-- }, enabled = page > 0) { Text("Prev") }
                    Text("Page ${page + 1} / $pageCount")
                    OutlinedButton(onClick = { if (page < pageCount - 1) page++ }, enabled = page < pageCount - 1) { Text("Next") }
                }
            }

            // Page preview with draggable/resizable signature overlay.
            pageBitmap?.let { pb ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { boxSize = it.size },
                ) {
                    Image(bitmap = pb.asImageBitmap(), contentDescription = "Page preview", modifier = Modifier.fillMaxWidth())
                    signature?.let { sig ->
                        if (boxSize.width > 0) {
                            val baseW = boxSize.width * 0.3f
                            val ratio = sig.height.toFloat() / sig.width.toFloat()
                            val baseH = baseW * ratio
                            val wDp = with(density) { (baseW * scale).toDp() }
                            val hDp = with(density) { (baseH * scale).toDp() }
                            Image(
                                bitmap = sig.asImageBitmap(),
                                contentDescription = "Signature",
                                modifier = Modifier
                                    .offset { IntOffset(offset.x.toInt(), offset.y.toInt()) }
                                    .size(wDp, hDp)
                                    .pointerInput(sig) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            offset += pan
                                            scale = (scale * zoom).coerceIn(0.2f, 5f)
                                        }
                                    },
                            )
                        }
                    }
                }
                if (signature != null) {
                    Text("Drag to move · pinch to resize", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Signature builder
            Text("Signature", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(mode == 0, onClick = { mode = 0 }, label = { Text("Draw") })
                FilterChip(mode == 1, onClick = { mode = 1 }, label = { Text("Type") })
                FilterChip(mode == 2, onClick = { mode = 2 }, label = { Text("Import") })
            }

            when (mode) {
                0 -> DrawPad(onDone = { signature = it })
                1 -> TypeSignature(onDone = { signature = it })
                else -> OutlinedButton(onClick = { pickImage.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Import signature image")
                }
            }

            Button(
                onClick = {
                    val u = uri ?: return@Button
                    val sig = signature ?: return@Button
                    if (boxSize.width == 0) return@Button
                    val baseW = boxSize.width * 0.3f
                    val ratio = sig.height.toFloat() / sig.width.toFloat()
                    val dispW = baseW * scale
                    val dispH = baseW * ratio * scale
                    val nl = offset.x / boxSize.width
                    val nt = offset.y / boxSize.height
                    val nw = dispW / boxSize.width
                    val nh = dispH / boxSize.height
                    working = true
                    scope.launch {
                        try {
                            val outDir = File(context.cacheDir, "pdf_out").apply { mkdirs() }
                            val out = File(outDir, "signed_${System.currentTimeMillis()}.pdf")
                            withContext(Dispatchers.IO) {
                                PdfOps.signPage(context, u, page, sig, nl, nt, nw, nh, out)
                            }
                            resultFile = out
                        } finally {
                            working = false
                        }
                    }
                },
                enabled = signature != null && !working,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Apply signature & share") }

            if (working) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp)); Spacer(Modifier.size(8.dp)); Text("Applying…")
                }
            }
            resultFile?.let { f ->
                Button(onClick = { sharePdfFile(context, f) }, modifier = Modifier.fillMaxWidth()) { Text("Share signed PDF") }
            }
        }
    }
}

@Composable
private fun DrawPad(onDone: (Bitmap) -> Unit) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var current by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var padSize by remember { mutableStateOf(IntSize.Zero) }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .onSizeChanged { padSize = it }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { current = listOf(it) },
                        onDrag = { change, _ -> current = current + change.position },
                        onDragEnd = { strokes.add(current); current = emptyList() },
                    )
                },
        ) {
            Canvas(Modifier.fillMaxWidth().height(160.dp)) {
                val ink = Color.Black
                (strokes + listOf(current)).forEach { stroke ->
                    for (i in 1 until stroke.size) {
                        drawLine(ink, stroke[i - 1], stroke[i], strokeWidth = 5f)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { strokes.clear(); current = emptyList() }) { Text("Clear") }
            Button(onClick = {
                if (strokes.isNotEmpty() && padSize.width > 0) {
                    onDone(SignatureRender.strokesToBitmap(strokes.toList(), padSize.width, padSize.height))
                }
            }) { Text("Use drawing") }
        }
    }
}

@Composable
private fun TypeSignature(onDone: (Bitmap) -> Unit) {
    var text by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Type your name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(onClick = { if (text.isNotBlank()) onDone(SignatureRender.textToBitmap(text.trim())) }, enabled = text.isNotBlank()) {
            Text("Use text")
        }
    }
}

private fun sharePdfFile(context: Context, file: File) {
    val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, fileUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share signed PDF"))
}
