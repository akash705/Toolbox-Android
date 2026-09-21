package com.toolbox.everyday.storageanalyzer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface UiState {
    data object NeedsFolder : UiState
    data object Idle : UiState
    data class Scanning(val files: Int) : UiState
    data class Done(val report: StorageReport) : UiState
}

@Composable
fun StorageAnalyzerScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("storage_analyzer", Context.MODE_PRIVATE) }

    var treeUri by remember { mutableStateOf(loadPersistedTree(context, prefs)) }
    var state by remember { mutableStateOf<UiState>(if (treeUri != null) UiState.Idle else UiState.NeedsFolder) }
    var tab by remember { mutableIntStateOf(0) }
    var confirmDelete by remember { mutableStateOf<FileEntry?>(null) }

    fun startScan() {
        val uri = treeUri ?: return
        state = UiState.Scanning(0)
        scope.launch {
            val report = withContext(Dispatchers.IO) {
                StorageScanner.scan(context, uri) { files -> state = UiState.Scanning(files) }
            }
            state = UiState.Done(report)
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            prefs.edit().putString("tree", uri.toString()).apply()
            treeUri = uri
            startScan()
        }
    }

    // Auto-scan once when returning with an already-granted folder.
    LaunchedEffect(Unit) { if (treeUri != null && state is UiState.Idle) startScan() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Pick a folder (e.g. Internal storage) and see what's using space inside it — largest files, biggest folders, and a breakdown by type. Nothing is uploaded; deletions are confirmed one by one.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = { picker.launch(null) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (treeUri == null) "Choose a folder to analyze" else "Change folder / rescan")
        }

        when (val s = state) {
            UiState.NeedsFolder -> {}
            UiState.Idle -> {}
            is UiState.Scanning -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(22.dp)); Spacer(Modifier.width(12.dp))
                Text("Scanning… ${s.files} files")
            }
            is UiState.Done -> ReportView(
                report = s.report,
                tab = tab,
                onTab = { tab = it },
                onDeleteRequest = { confirmDelete = it },
            )
        }
    }

    confirmDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete file?") },
            text = { Text("${entry.name}\n${formatSize(entry.bytes)}\n\nThis permanently deletes the file from your device.") },
            confirmButton = {
                TextButton(onClick = {
                    val target = entry
                    confirmDelete = null
                    val ok = runCatching {
                        DocumentsContract.deleteDocument(context.contentResolver, target.uri)
                    }.getOrDefault(false)
                    if (ok && state is UiState.Done) {
                        val r = (state as UiState.Done).report
                        state = UiState.Done(
                            r.copy(
                                largestFiles = r.largestFiles.filterNot { it.uri == target.uri },
                                totalBytes = (r.totalBytes - target.bytes).coerceAtLeast(0),
                                fileCount = (r.fileCount - 1).coerceAtLeast(0),
                            ),
                        )
                    } else if (!ok) {
                        android.widget.Toast.makeText(context, "Couldn't delete this file", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ColumnScope.ReportView(
    report: StorageReport,
    tab: Int,
    onTab: (Int) -> Unit,
    onDeleteRequest: (FileEntry) -> Unit,
) {
    if (report.fileCount == 0) {
        Text("No files found in that folder.", style = MaterialTheme.typography.titleMedium)
        return
    }
    Text("${formatSize(report.totalBytes)} across ${report.fileCount} files", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

    report.categories.forEach { cat ->
        val fraction = if (report.totalBytes > 0) cat.bytes.toFloat() / report.totalBytes else 0f
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth()) {
                Text(cat.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text(formatSize(cat.bytes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.primary))
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(tab == 0, onClick = { onTab(0) }, label = { Text("Largest files") })
        FilterChip(tab == 1, onClick = { onTab(1) }, label = { Text("Largest folders") })
    }

    LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (tab == 0) {
            items(report.largestFiles, key = { it.uri.toString() }) { f ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)) {
                    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(f.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                            Text(formatSize(f.bytes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onDeleteRequest(f) }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        } else {
            items(report.largestFolders, key = { it.path }) { d ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(d.name.ifEmpty { "(folder)" }, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                            Text(d.path, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                        Text(formatSize(d.bytes), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

private fun loadPersistedTree(context: Context, prefs: android.content.SharedPreferences): Uri? {
    val saved = prefs.getString("tree", null)?.let(Uri::parse) ?: return null
    val stillGranted = context.contentResolver.persistedUriPermissions.any { it.uri == saved && it.isReadPermission }
    if (!stillGranted) { prefs.edit().remove("tree").apply(); return null }
    return saved
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.2f GB".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.0f KB".format(bytes / 1_000.0)
    else -> "$bytes B"
}
