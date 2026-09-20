package com.toolbox.everyday.duplicatephotos

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface ScanState {
    data object NeedsPermission : ScanState
    data object Idle : ScanState
    data class Scanning(val done: Int, val total: Int) : ScanState
    data class Results(val groups: List<DuplicateGroup>) : ScanState
}

@Composable
fun DuplicatePhotosScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE

    var state by remember { mutableStateOf<ScanState>(if (hasPermission(context, permission)) ScanState.Idle else ScanState.NeedsPermission) }
    var selected by remember { mutableStateOf<Set<Uri>>(emptySet()) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        state = if (granted) ScanState.Idle else ScanState.NeedsPermission
    }

    fun startScan() {
        state = ScanState.Scanning(0, 0)
        scope.launch {
            val groups = withContext(Dispatchers.IO) {
                DuplicateFinder.scan(context) { done, total ->
                    state = ScanState.Scanning(done, total)
                }
            }
            // Pre-select every copy except the newest (first) in each group.
            selected = groups.flatMap { it.photos.drop(1) }.map { it.uri }.toSet()
            state = ScanState.Results(groups)
        }
    }

    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { res ->
        if (res.resultCode == android.app.Activity.RESULT_OK) removeDeleted(context, selected) { state = it; selected = emptySet() }
    }

    fun deleteSelected() {
        val uris = selected.toList()
        if (uris.isEmpty()) return
        if (Build.VERSION.SDK_INT >= 30) {
            val pi = MediaStore.createDeleteRequest(context.contentResolver, uris)
            deleteLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
        } else {
            scope.launch {
                withContext(Dispatchers.IO) { uris.forEach { runCatching { context.contentResolver.delete(it, null, null) } } }
                removeDeleted(context, selected.toSet()) { state = it; selected = emptySet() }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (val s = state) {
            ScanState.NeedsPermission -> {
                Text("Find exact duplicate photos and free up space. Grant photo access to scan.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { permLauncher.launch(permission) }, modifier = Modifier.fillMaxWidth()) { Text("Grant photo access") }
            }
            ScanState.Idle -> {
                Text("Scans your photo library for images with identical content. Nothing is uploaded — analysis is entirely on-device.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { startScan() }, modifier = Modifier.fillMaxWidth()) { Text("Scan for duplicates") }
            }
            is ScanState.Scanning -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(22.dp)); Spacer(Modifier.width(12.dp))
                    Text(if (s.total == 0) "Reading library…" else "Comparing ${s.done} / ${s.total}…")
                }
            }
            is ScanState.Results -> ResultsView(
                groups = s.groups,
                selected = selected,
                onToggle = { uri -> selected = if (uri in selected) selected - uri else selected + uri },
                onDelete = { deleteSelected() },
                onRescan = { startScan() },
            )
        }
    }
}

@Composable
private fun ColumnScope.ResultsView(
    groups: List<DuplicateGroup>,
    selected: Set<Uri>,
    onToggle: (Uri) -> Unit,
    onDelete: () -> Unit,
    onRescan: () -> Unit,
) {
    if (groups.isEmpty()) {
        Text("No duplicate photos found. 🎉", style = MaterialTheme.typography.titleMedium)
        Button(onClick = onRescan, modifier = Modifier.fillMaxWidth()) { Text("Scan again") }
        return
    }
    val reclaim = groups.flatMap { it.photos }.filter { it.uri in selected }.sumOf { it.sizeBytes }
    Text("${groups.size} duplicate set(s) — ${formatSize(reclaim)} selected to free", fontWeight = FontWeight.SemiBold)
    Button(onClick = onDelete, enabled = selected.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
        Text("Delete ${selected.size} selected")
    }
    LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(groups, key = { it.photos.first().uri.toString() }) { group ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)) {
                Column(Modifier.padding(8.dp)) {
                    Text("${group.photos.size} copies · ${formatSize(group.photos.first().sizeBytes)} each", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    group.photos.forEachIndexed { index, photo ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Thumbnail(photo.uri)
                            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(photo.displayName, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                Text(if (index == 0) "Newest — kept by default" else "Duplicate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Checkbox(checked = photo.uri in selected, onCheckedChange = { onToggle(photo.uri) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Thumbnail(uri: Uri) {
    val context = LocalContext.current
    val bmp by produceState<Bitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) { decodeThumb(context, uri) }
    }
    Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
        bmp?.let { Image(it.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(56.dp)) }
    }
}

private fun decodeThumb(context: Context, uri: Uri): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 160) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
}.getOrNull()

private fun removeDeleted(context: Context, deleted: Set<Uri>, update: (ScanState) -> Unit) {
    // The deleted uris are gone; rescanning is the simplest correct way to refresh.
    update(ScanState.Idle)
}

private fun hasPermission(context: Context, perm: String): Boolean =
    context.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.0f KB".format(bytes / 1_000.0)
    else -> "$bytes B"
}
