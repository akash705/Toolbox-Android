package com.toolbox.everyday.pdftoolkit

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.toolbox.core.pdf.PdfOps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private sealed interface Status {
    data object Idle : Status
    data object Working : Status
    data class Done(val file: File) : Status
    data class Error(val message: String) : Status
}

@Composable
fun PdfToolkitScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf<Status>(Status.Idle) }

    // Merge inputs
    var mergeUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    // Rotate/Extract input
    var singleUri by remember { mutableStateOf<Uri?>(null) }
    var rotateDegrees by remember { mutableIntStateOf(90) }
    var range by remember { mutableStateOf("") }

    val outDir = remember { File(context.cacheDir, "pdf_out").apply { mkdirs() } }

    val pickMultiple = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
        mergeUris = it; status = Status.Idle
    }
    val pickSingle = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        singleUri = it; status = Status.Idle
    }

    fun run(block: (File) -> Unit) {
        status = Status.Working
        scope.launch {
            try {
                val out = File(outDir, "toolbox_${System.currentTimeMillis()}.pdf")
                withContext(Dispatchers.IO) { block(out) }
                status = Status.Done(out)
            } catch (e: Exception) {
                status = Status.Error(e.message ?: "Could not process the PDF")
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(tab == 0, onClick = { tab = 0; status = Status.Idle }, label = { Text("Merge") })
            FilterChip(tab == 1, onClick = { tab = 1; status = Status.Idle }, label = { Text("Rotate") })
            FilterChip(tab == 2, onClick = { tab = 2; status = Status.Idle }, label = { Text("Extract") })
            FilterChip(tab == 4, onClick = { tab = 4; status = Status.Idle }, label = { Text("Reorder") })
            FilterChip(tab == 3, onClick = { tab = 3; status = Status.Idle }, label = { Text("Sign") })
        }

        // The Sign and Reorder tabs must NOT be inside a verticalScroll — their gesture/draw pad
        // and thumbnail list would fight the scroll. Each manages its own layout.
        if (tab == 3) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) { PdfSignSection() }
            return@Column
        }
        if (tab == 4) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) { PdfReorderSection() }
            return@Column
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        when (tab) {
            0 -> {
                OutlinedButton(onClick = { pickMultiple.launch(arrayOf("application/pdf")) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (mergeUris.isEmpty()) "Pick PDFs to merge" else "${mergeUris.size} PDFs selected")
                }
                Button(
                    onClick = { run { out -> PdfOps.merge(context, mergeUris, out) } },
                    enabled = mergeUris.size >= 2 && status != Status.Working,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Merge") }
            }
            1 -> {
                OutlinedButton(onClick = { pickSingle.launch(arrayOf("application/pdf")) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (singleUri == null) "Pick a PDF" else "PDF selected")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(90, 180, 270).forEach { d ->
                        FilterChip(rotateDegrees == d, onClick = { rotateDegrees = d }, label = { Text("$d°") })
                    }
                }
                Button(
                    onClick = { singleUri?.let { u -> run { out -> PdfOps.rotate(context, u, rotateDegrees, out) } } },
                    enabled = singleUri != null && status != Status.Working,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Rotate") }
            }
            else -> {
                OutlinedButton(onClick = { pickSingle.launch(arrayOf("application/pdf")) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (singleUri == null) "Pick a PDF" else "PDF selected")
                }
                OutlinedTextField(
                    value = range, onValueChange = { range = it },
                    label = { Text("Pages (e.g. 1-3,5)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        singleUri?.let { u ->
                            run { out ->
                                val count = PdfOps.pageCount(context, u)
                                val indices = PdfOps.parseRange(range, count)
                                require(indices.isNotEmpty()) { "No valid pages in that range" }
                                PdfOps.extract(context, u, indices, out)
                            }
                        }
                    },
                    enabled = singleUri != null && range.isNotBlank() && status != Status.Working,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Extract") }
            }
        }

        Spacer(Modifier.height(8.dp))
        when (val s = status) {
            Status.Working -> Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp))
                Spacer(Modifier.width(8.dp)); Text("Processing…")
            }
            is Status.Done -> {
                Text("Done!", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Button(onClick = { sharePdf(context, s.file) }, modifier = Modifier.fillMaxWidth()) { Text("Share result") }
            }
            is Status.Error -> Text(s.message, color = MaterialTheme.colorScheme.error)
            Status.Idle -> {}
        }

        Text(
            "Output is rasterized (pages re-rendered as images), so text is not selectable. Fully offline.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        }
    }
}

private fun sharePdf(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share PDF"))
}
