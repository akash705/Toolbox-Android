package com.toolbox.everyday.pdftoolkit

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.toolbox.core.pdf.PdfOps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val REORDER_MAX_PAGES = 60

@Composable
fun PdfReorderSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val outDir = remember { File(context.cacheDir, "pdf_out").apply { mkdirs() } }

    var uri by remember { mutableStateOf<Uri?>(null) }
    var thumbs by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var order by remember { mutableStateOf<List<Int>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<File?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { picked ->
        if (picked == null) return@rememberLauncherForActivityResult
        uri = picked
        result = null; error = null
        loading = true
        scope.launch {
            val loaded = withContext(Dispatchers.IO) {
                val count = PdfOps.pageCount(context, picked)
                if (count == 0 || count > REORDER_MAX_PAGES) return@withContext null
                (0 until count).mapNotNull { PdfOps.renderPage(context, picked, it, 160) }
            }
            if (loaded == null) {
                error = "Reorder supports PDFs of 1–$REORDER_MAX_PAGES pages."
                thumbs = emptyList(); order = emptyList()
            } else {
                thumbs = loaded
                order = loaded.indices.toList()
            }
            loading = false
        }
    }

    fun move(pos: Int, delta: Int) {
        val target = pos + delta
        if (target < 0 || target >= order.size) return
        order = order.toMutableList().also { it[pos] = it[target].also { _ -> it[target] = it[pos] } }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { pick.launch(arrayOf("application/pdf")) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (uri == null) "Pick a PDF to reorder" else "PDF selected — ${order.size} pages")
        }

        if (loading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Loading pages…")
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        if (order.isNotEmpty()) {
            LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(order, key = { _, src -> src }) { pos, src ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)) {
                        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(60.dp).height(80.dp), contentAlignment = Alignment.Center) {
                                Image(thumbs[src].asImageBitmap(), contentDescription = "Page ${src + 1}")
                            }
                            Text("Page ${src + 1}", Modifier.weight(1f).padding(start = 12.dp), fontWeight = FontWeight.Medium)
                            IconButton(onClick = { move(pos, -1) }, enabled = pos > 0) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
                            }
                            IconButton(onClick = { move(pos, 1) }, enabled = pos < order.size - 1) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val u = uri ?: return@Button
                    working = true; result = null; error = null
                    scope.launch {
                        try {
                            val out = File(outDir, "reordered_${System.currentTimeMillis()}.pdf")
                            withContext(Dispatchers.IO) { PdfOps.reorder(context, u, order, out) }
                            result = out
                        } catch (e: Exception) {
                            error = e.message ?: "Could not save the PDF"
                        }
                        working = false
                    }
                },
                enabled = !working,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (working) "Saving…" else "Save reordered PDF") }
        }

        result?.let { file ->
            Text("Done!", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Button(onClick = { shareReordered(context, file) }, modifier = Modifier.fillMaxWidth()) { Text("Share result") }
        }
    }
}

private fun shareReordered(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share PDF"))
}
