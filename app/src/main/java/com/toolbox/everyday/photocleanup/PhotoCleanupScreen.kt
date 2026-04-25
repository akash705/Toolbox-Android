package com.toolbox.everyday.photocleanup

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toolbox.everyday.photocleanup.ExifReader.FieldPresence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private const val MAX_BATCH = 20

@Composable
fun PhotoCleanupScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var tab by remember { mutableIntStateOf(0) }
    val selected = remember { mutableStateListOf<Uri>() }
    val results = remember { mutableStateListOf<ImageProcessor.ProcessResult>() }
    var processing by remember { mutableStateOf(false) }
    var progressDone by remember { mutableIntStateOf(0) }

    // Compress tab state
    var quality by remember { mutableFloatStateOf(80f) }
    var resizeEnabled by remember { mutableStateOf(false) }
    var maxLongEdge by remember { mutableFloatStateOf(2048f) }

    // Strip Metadata tab state
    var keepOrientation by remember { mutableStateOf(true) }
    var presence by remember { mutableStateOf<FieldPresence?>(null) }

    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = MAX_BATCH),
    ) { uris ->
        if (uris.isNotEmpty()) {
            selected.clear()
            selected.addAll(uris.take(MAX_BATCH))
            results.clear()
        }
    }

    // Refresh EXIF presence summary when selection or tab changes (Strip Metadata only)
    LaunchedEffect(selected.size, tab) {
        presence = if (tab == 1 && selected.isNotEmpty()) {
            withContext(Dispatchers.IO) { ExifReader.read(context, selected.first()) }
        } else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TabRow(selectedTabIndex = tab) {
            Tab(
                selected = tab == 0,
                onClick = { tab = 0; results.clear() },
                text = { Text("Compress") },
                icon = { Icon(Icons.Default.Compress, contentDescription = null) },
            )
            Tab(
                selected = tab == 1,
                onClick = { tab = 1; results.clear() },
                text = { Text("Strip Metadata") },
                icon = { Icon(Icons.Default.PrivacyTip, contentDescription = null) },
            )
        }

        SelectionCard(
            selectedCount = selected.size,
            onPick = {
                pickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onClear = { selected.clear(); results.clear() },
        )

        if (tab == 0) {
            CompressControls(
                quality = quality,
                onQuality = { quality = it },
                resizeEnabled = resizeEnabled,
                onResizeEnabled = { resizeEnabled = it },
                maxLongEdge = maxLongEdge,
                onMaxLongEdge = { maxLongEdge = it },
            )
        } else {
            StripMetadataControls(
                presence = presence,
                selectionCount = selected.size,
                keepOrientation = keepOrientation,
                onKeepOrientation = { keepOrientation = it },
            )
        }

        Button(
            onClick = {
                processing = true
                progressDone = 0
                results.clear()
                scope.launch {
                    val output = mutableListOf<ImageProcessor.ProcessResult>()
                    withContext(Dispatchers.Default) {
                        for ((index, uri) in selected.withIndex()) {
                            val r = runCatching {
                                if (tab == 0) {
                                    ImageProcessor.compress(
                                        context = context,
                                        sourceUri = uri,
                                        quality = quality.roundToInt(),
                                        maxLongEdgePx = if (resizeEnabled) maxLongEdge.roundToInt() else null,
                                    )
                                } else {
                                    ImageProcessor.stripMetadata(
                                        context = context,
                                        sourceUri = uri,
                                        keepOrientation = keepOrientation,
                                    )
                                }
                            }.getOrNull()
                            if (r != null) output.add(r)
                            progressDone = index + 1
                        }
                    }
                    results.addAll(output)
                    processing = false
                }
            },
            enabled = !processing && selected.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (processing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(8.dp))
                Text("Processing ${progressDone}/${selected.size}…")
            } else {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (tab == 0) "Compress ${selected.size.takeIf { it > 0 } ?: ""}".trim()
                    else "Strip metadata ${selected.size.takeIf { it > 0 } ?: ""}".trim(),
                )
            }
        }

        if (processing && selected.isNotEmpty()) {
            LinearProgressIndicator(
                progress = { progressDone.toFloat() / selected.size },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (results.isNotEmpty()) {
            ResultsCard(results = results, mode = tab)
        }
    }
}

@Composable
private fun SelectionCard(
    selectedCount: Int,
    onPick: () -> Unit,
    onClear: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Photo,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (selectedCount == 0) "No photos selected"
                        else "$selectedCount of $MAX_BATCH selected",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Pick from gallery — uses Android's secure photo picker (no permissions needed)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPick, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (selectedCount == 0) "Pick photos" else "Change selection")
                }
                if (selectedCount > 0) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Check, contentDescription = "Clear")
                    }
                }
            }
        }
    }
}

@Composable
private fun CompressControls(
    quality: Float,
    onQuality: (Float) -> Unit,
    resizeEnabled: Boolean,
    onResizeEnabled: (Boolean) -> Unit,
    maxLongEdge: Float,
    onMaxLongEdge: (Float) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            LabeledValueRow("Quality", "${quality.roundToInt()}")
            Slider(
                value = quality,
                onValueChange = onQuality,
                valueRange = 30f..100f,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Resize long edge", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Cap the longer side of each image",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = resizeEnabled, onCheckedChange = onResizeEnabled)
            }
            if (resizeEnabled) {
                Spacer(Modifier.height(8.dp))
                LabeledValueRow("Max long edge", "${maxLongEdge.roundToInt()} px")
                Slider(
                    value = maxLongEdge,
                    onValueChange = onMaxLongEdge,
                    valueRange = 480f..4096f,
                )
            }
        }
    }
}

@Composable
private fun StripMetadataControls(
    presence: FieldPresence?,
    selectionCount: Int,
    keepOrientation: Boolean,
    onKeepOrientation: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Metadata in first photo",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Showing presence only — actual values (location, timestamps, device serial) are never displayed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            if (selectionCount == 0 || presence == null) {
                Text(
                    "Pick a photo to see what will be stripped.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                PresenceRow("Location (GPS)", presence.gps)
                PresenceRow("Device & camera info", presence.device)
                PresenceRow("Timestamps", presence.timestamps)
                PresenceRow("Embedded thumbnail", presence.thumbnail)
                PresenceRow("Orientation tag", presence.orientation)
                Spacer(Modifier.height(8.dp))
                Text(
                    "${presence.totalTagCount} known EXIF tag(s) detected.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Keep orientation", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (keepOrientation) "Photos stay right-side-up via the orientation tag"
                        else "Rotation is baked into pixels; tag is removed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = keepOrientation, onCheckedChange = onKeepOrientation)
            }
        }
    }
}

@Composable
private fun PresenceRow(label: String, present: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(
                    if (present) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                ),
        )
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            if (present) "Will be removed" else "Not present",
            style = MaterialTheme.typography.bodySmall,
            color = if (present) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (present) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun LabeledValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ResultsCard(
    results: SnapshotStateList<ImageProcessor.ProcessResult>,
    mode: Int,
) {
    val context = LocalContext.current
    val totalSaved = results.sumOf { (it.originalBytes - it.outputBytes).coerceAtLeast(0) }
    val totalOriginal = results.sumOf { it.originalBytes.coerceAtLeast(0) }
    val savedPct = if (totalOriginal > 0) (totalSaved * 100 / totalOriginal).toInt() else 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                if (mode == 0) "Compressed ${results.size}" else "Cleaned ${results.size}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (totalOriginal > 0) "Saved ${formatBytes(totalSaved)} ($savedPct%) — files in Pictures/Toolbox"
                else "Files saved to Pictures/Toolbox",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(12.dp))

            results.forEachIndexed { index, r ->
                if (index > 0) Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(enabled = r.outputUri != null) {
                            r.outputUri?.let { shareImage(context, it) }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            r.outputPath.substringAfterLast('/'),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            maxLines = 1,
                        )
                        Text(
                            sizeChange(r.originalBytes, r.outputBytes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

private fun shareImage(context: android.content.Context, uri: Uri) {
    // Use the MediaStore content URI directly when available; safer than re-saving.
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share image"))
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.size - 1) {
        value /= 1024
        unit++
    }
    return "%.1f %s".format(value, units[unit])
}

private fun sizeChange(originalBytes: Long, outputBytes: Long): String {
    if (originalBytes <= 0) return formatBytes(outputBytes)
    val pct = ((originalBytes - outputBytes) * 100 / originalBytes).toInt()
    val arrow = if (pct >= 0) "↓" else "↑"
    return "${formatBytes(originalBytes)} → ${formatBytes(outputBytes)}  $arrow${kotlin.math.abs(pct)}%"
}

