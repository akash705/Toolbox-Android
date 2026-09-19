package com.toolbox.everyday.docscanner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.core.content.FileProvider
import com.toolbox.core.pdf.PdfOps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun DocumentScannerScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var working by remember { mutableStateOf(false) }
    var resultFile by remember { mutableStateOf<File?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) pendingCameraUri?.let { pages = pages + it }
        pendingCameraUri = null
        resultFile = null
    }
    val pickImages = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
        pages = pages + it; resultFile = null
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera(context, { pendingCameraUri = it }, takePicture)
    }

    fun onAddCamera() {
        val granted = context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (granted) launchCamera(context, { pendingCameraUri = it }, takePicture)
        else cameraPermission.launch(Manifest.permission.CAMERA)
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onAddCamera() }, modifier = Modifier.weight(1f)) { Text("Camera") }
            OutlinedButton(onClick = { pickImages.launch(arrayOf("image/*")) }, modifier = Modifier.weight(1f)) { Text("Gallery") }
        }

        if (pages.isNotEmpty()) {
            Text("${pages.size} page(s)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(pages) { index, _ ->
                    Box(
                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${index + 1}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
            OutlinedButton(onClick = { pages = emptyList(); resultFile = null }, modifier = Modifier.fillMaxWidth()) { Text("Clear") }
        }

        Button(
            onClick = {
                working = true
                scope.launch {
                    try {
                        val outDir = File(context.cacheDir, "pdf_out").apply { mkdirs() }
                        val out = File(outDir, "scan_${System.currentTimeMillis()}.pdf")
                        withContext(Dispatchers.IO) { PdfOps.imagesToPdf(context, pages, out) }
                        resultFile = out
                    } finally { working = false }
                }
            },
            enabled = pages.isNotEmpty() && !working,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Export as PDF") }

        if (working) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.width(20.dp).height(20.dp)); Spacer(Modifier.width(8.dp)); Text("Building PDF…")
            }
        }
        resultFile?.let { f ->
            Button(onClick = { sharePdf(context, f) }, modifier = Modifier.fillMaxWidth()) { Text("Share PDF") }
        }
    }
}

private fun launchCamera(
    context: Context,
    setUri: (Uri) -> Unit,
    launcher: androidx.activity.result.ActivityResultLauncher<Uri>,
) {
    val dir = File(context.cacheDir, "scans").apply { mkdirs() }
    val file = File(dir, "scan_${System.currentTimeMillis()}.jpg")
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    setUri(uri)
    launcher.launch(uri)
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
