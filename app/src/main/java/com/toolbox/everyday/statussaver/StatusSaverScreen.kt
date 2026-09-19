package com.toolbox.everyday.statussaver

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.toolbox.core.media.MediaStoreWriter

private const val STATUSES_INITIAL =
    "content://com.android.externalstorage.documents/document/primary%3AAndroid%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia%2F.Statuses"

private data class StatusItem(val uri: Uri, val name: String, val mime: String, val modified: Long)

@Composable
fun StatusSaverScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("status_saver", Context.MODE_PRIVATE) }
    var treeUri by remember { mutableStateOf(prefs.getString("tree", null)?.let(Uri::parse)) }
    var items by remember { mutableStateOf<List<StatusItem>>(emptyList()) }

    fun refresh() {
        treeUri?.let { items = loadStatuses(context, it) }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            prefs.edit().putString("tree", uri.toString()).apply()
            treeUri = uri
            refresh()
        }
    }

    androidx.compose.runtime.LaunchedEffect(treeUri) { refresh() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedButton(
            onClick = {
                val initial = Uri.parse(STATUSES_INITIAL)
                picker.launch(initial)
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (treeUri == null) "Grant access to WhatsApp statuses" else "Change folder") }

        Text(
            "Open a status in WhatsApp first, then grant access to the “.Statuses” folder when asked. Statuses vanish after 24 hours.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
        )

        if (treeUri != null && items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No statuses found. View some in WhatsApp, then reopen this tool.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items, key = { it.uri.toString() }) { item ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)) {
                        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (item.mime.startsWith("video")) "🎬 Video" else "🖼 Image", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            if (item.mime.startsWith("image")) {
                                IconButton(onClick = {
                                    val msg = saveImage(context, item.uri)
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }) { Icon(Icons.Default.Save, contentDescription = "Save") }
                            }
                            IconButton(onClick = { share(context, item) }) { Icon(Icons.Default.Share, contentDescription = "Share") }
                        }
                    }
                }
            }
        }
    }
}

private fun loadStatuses(context: Context, treeUri: Uri): List<StatusItem> {
    val out = mutableListOf<StatusItem>()
    val childrenUri = runCatching {
        DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
    }.getOrNull() ?: return emptyList()
    val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
    )
    context.contentResolver.query(childrenUri, projection, null, null, null)?.use { c ->
        while (c.moveToNext()) {
            val docId = c.getString(0)
            val name = c.getString(1) ?: docId
            val mime = c.getString(2) ?: ""
            val modified = c.getLong(3)
            if (mime.startsWith("image") || mime.startsWith("video")) {
                val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                out.add(StatusItem(docUri, name, mime, modified))
            }
        }
    }
    return out.sortedByDescending { it.modified }
}

private fun saveImage(context: Context, uri: Uri): String = try {
    val bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        ?: return "Couldn't read image"
    val r = MediaStoreWriter.saveJpeg(context, bmp, "Toolbox", "status", 95)
    "Saved to ${r.displayPath}"
} catch (e: Exception) {
    "Couldn't save: ${e.message}"
}

private fun share(context: Context, item: StatusItem) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = item.mime
        putExtra(Intent.EXTRA_STREAM, item.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share status"))
}
