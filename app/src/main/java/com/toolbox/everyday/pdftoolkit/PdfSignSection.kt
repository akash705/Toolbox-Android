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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
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

private enum class ApplyScope { ThisPage, AllPages, Specific }

@OptIn(ExperimentalMaterial3Api::class)
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

    var signature by remember { mutableStateOf<Bitmap?>(null) }
    var offset by remember { mutableStateOf(Offset(80f, 80f)) }
    var scaleF by remember { mutableFloatStateOf(1f) }

    var applyScope by remember { mutableStateOf(ApplyScope.ThisPage) }
    var rangeText by remember { mutableStateOf("") }

    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
        pageBitmap = withContext(Dispatchers.IO) { PdfOps.renderPage(context, u, page, 1240) }
    }

    if (uri == null) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { pickPdf.launch(arrayOf("application/pdf")) }, modifier = Modifier.fillMaxWidth()) {
                Text("Pick a PDF to sign")
            }
            Text(
                "Pick a PDF to open it full-screen. Add your signature (draw / type / import), drag it onto the page, then apply to this page, all pages, or specific pages.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    // Full-screen editor
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Page nav
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { pickPdf.launch(arrayOf("application/pdf")) }) { Text("Change") }
            Spacer(Modifier.weight(1f))
            if (pageCount > 1) {
                OutlinedButton(onClick = { if (page > 0) page-- }, enabled = page > 0) { Text("‹") }
                Text("Page ${page + 1} / $pageCount")
                OutlinedButton(onClick = { if (page < pageCount - 1) page++ }, enabled = page < pageCount - 1) { Text("›") }
            }
        }

        // Big page area
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f).onGloballyPositioned { boxSize = it.size },
            contentAlignment = Alignment.Center,
        ) {
            val pb = pageBitmap
            if (pb == null) {
                CircularProgressIndicator()
            } else {
                Image(
                    bitmap = pb.asImageBitmap(),
                    contentDescription = "Page ${page + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
                val sig = signature
                if (sig != null && boxSize.width > 0) {
                    val rect = drawnRect(pb, boxSize)
                    val baseW = rect.w * 0.32f
                    val sigRatio = sig.height.toFloat() / sig.width.toFloat()
                    val wDp = with(density) { (baseW * scaleF).toDp() }
                    val hDp = with(density) { (baseW * sigRatio * scaleF).toDp() }
                    Image(
                        bitmap = sig.asImageBitmap(),
                        contentDescription = "Signature",
                        modifier = Modifier
                            .offset { IntOffset(offset.x.toInt(), offset.y.toInt()) }
                            .size(wDp, hDp)
                            .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                            .pointerInput(sig) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    offset += pan
                                    scaleF = (scaleF * zoom).coerceIn(0.2f, 6f)
                                }
                            },
                    )
                }
            }
        }

        // Controls
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { showSheet = true }, modifier = Modifier.weight(1f)) {
                Text(if (signature == null) "Add signature" else "Edit signature")
            }
            if (signature != null) Text("Drag · pinch", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Text("Apply to", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(applyScope == ApplyScope.ThisPage, onClick = { applyScope = ApplyScope.ThisPage }, label = { Text("This page") })
            FilterChip(applyScope == ApplyScope.AllPages, onClick = { applyScope = ApplyScope.AllPages }, label = { Text("All pages") })
            FilterChip(applyScope == ApplyScope.Specific, onClick = { applyScope = ApplyScope.Specific }, label = { Text("Specific") })
        }
        if (applyScope == ApplyScope.Specific) {
            OutlinedTextField(
                value = rangeText, onValueChange = { rangeText = it },
                label = { Text("Pages (e.g. 1-3,5)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
        }

        Button(
            onClick = {
                val u = uri ?: return@Button
                val sig = signature ?: return@Button
                val pb = pageBitmap ?: return@Button
                if (boxSize.width == 0) return@Button
                val rect = drawnRect(pb, boxSize)
                val baseW = rect.w * 0.32f
                val sigRatio = sig.height.toFloat() / sig.width.toFloat()
                val nl = ((offset.x - rect.x) / rect.w).coerceIn(0f, 1f)
                val nt = ((offset.y - rect.y) / rect.h).coerceIn(0f, 1f)
                val nw = (baseW * scaleF) / rect.w
                val nh = (baseW * sigRatio * scaleF) / rect.h
                val pages: Set<Int> = when (applyScope) {
                    ApplyScope.ThisPage -> setOf(page)
                    ApplyScope.AllPages -> (0 until pageCount).toSet()
                    ApplyScope.Specific -> PdfOps.parseRange(rangeText, pageCount).toSet()
                }
                if (pages.isEmpty()) return@Button
                working = true
                scope.launch {
                    try {
                        val outDir = File(context.cacheDir, "pdf_out").apply { mkdirs() }
                        val out = File(outDir, "signed_${System.currentTimeMillis()}.pdf")
                        withContext(Dispatchers.IO) { PdfOps.signPages(context, u, pages, sig, nl, nt, nw, nh, out) }
                        resultFile = out
                    } finally { working = false }
                }
            },
            enabled = signature != null && !working &&
                (applyScope != ApplyScope.Specific || rangeText.isNotBlank()),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Apply signature & share") }

        if (working) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Applying…")
            }
        }
        resultFile?.let { f ->
            Button(onClick = { sharePdfFile(context, f) }, modifier = Modifier.fillMaxWidth()) { Text("Share signed PDF") }
        }
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }, sheetState = sheetState) {
            SignatureCreator(
                onDrawn = { signature = it },
                onTyped = { signature = it },
                onImport = { pickImage.launch(arrayOf("image/*")) },
                onDone = { showSheet = false },
            )
        }
    }
}

private data class DrawnRect(val x: Float, val y: Float, val w: Float, val h: Float)

/** Where the Fit-scaled page actually draws inside the box (for correct placement math). */
private fun drawnRect(pb: Bitmap, box: IntSize): DrawnRect {
    val imgRatio = pb.width.toFloat() / pb.height
    val boxW = box.width.toFloat(); val boxH = box.height.toFloat()
    val w: Float; val h: Float
    if (boxW / boxH > imgRatio) { h = boxH; w = boxH * imgRatio } else { w = boxW; h = boxW / imgRatio }
    return DrawnRect((boxW - w) / 2f, (boxH - h) / 2f, w, h)
}

@Composable
private fun SignatureCreator(
    onDrawn: (Bitmap) -> Unit,
    onTyped: (Bitmap) -> Unit,
    onImport: () -> Unit,
    onDone: () -> Unit,
) {
    var mode by remember { mutableIntStateOf(0) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Create signature", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(mode == 0, onClick = { mode = 0 }, label = { Text("Draw") })
            FilterChip(mode == 1, onClick = { mode = 1 }, label = { Text("Type") })
            FilterChip(mode == 2, onClick = { mode = 2 }, label = { Text("Import") })
        }
        when (mode) {
            0 -> DrawPad(onChange = onDrawn)
            1 -> TypeSignature(onChange = onTyped)
            else -> OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) { Text("Import signature image") }
        }
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}

@Composable
private fun DrawPad(onChange: (Bitmap) -> Unit) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var current by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var padSize by remember { mutableStateOf(IntSize.Zero) }

    fun commit() {
        if (strokes.isNotEmpty() && padSize.width > 0) {
            onChange(SignatureRender.strokesToBitmap(strokes.toList(), padSize.width, padSize.height))
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .onSizeChanged { padSize = it }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { current = listOf(it) },
                        onDrag = { change, _ -> change.consume(); current = current + change.position },
                        onDragEnd = { strokes.add(current); current = emptyList(); commit() },
                    )
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                (strokes + listOf(current)).forEach { stroke ->
                    for (i in 1 until stroke.size) drawLine(Color.Black, stroke[i - 1], stroke[i], strokeWidth = 5f)
                }
            }
        }
        OutlinedButton(onClick = { strokes.clear(); current = emptyList() }) { Text("Clear") }
    }
}

@Composable
private fun TypeSignature(onChange: (Bitmap) -> Unit) {
    var text by remember { mutableStateOf("") }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it; if (it.isNotBlank()) onChange(SignatureRender.textToBitmap(it.trim())) },
        label = { Text("Type your name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
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
