package com.toolbox.everyday.nfc

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun NfcToolkitScreen() {
    val context = LocalContext.current
    val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }

    if (nfcAdapter == null) {
        // No NFC hardware
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Default.Nfc,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "NFC Not Available",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "This device does not have NFC hardware.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    if (!nfcAdapter.isEnabled) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Default.Nfc,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "NFC is Disabled",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Enable NFC in settings to use this tool.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
            }) {
                Text("Open NFC Settings")
            }
        }
        return
    }

    NfcContent(nfcAdapter)
}

@Composable
private fun NfcContent(nfcAdapter: NfcAdapter) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var lastTag by remember { mutableStateOf<Tag?>(null) }
    var tagInfo by remember { mutableStateOf<TagInfo?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    // Enable NFC reader mode
    DisposableEffect(Unit) {
        val activity = context as Activity
        val callback = NfcAdapter.ReaderCallback { tag ->
            lastTag = tag
            tagInfo = parseTag(tag)
            statusMessage = null
        }
        nfcAdapter.enableReaderMode(
            activity,
            callback,
            NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V,
            Bundle(),
        )
        onDispose {
            nfcAdapter.disableReaderMode(activity)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Read") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Write") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Tools") })
        }

        // Status message
        statusMessage?.let { msg ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isError) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                        contentDescription = null,
                    )
                    Text(msg, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        when (selectedTab) {
            0 -> ReadTab(tagInfo)
            1 -> WriteTab(lastTag) { msg, err ->
                statusMessage = msg
                isError = err
                // Re-read tag after write
                if (!err) lastTag?.let { tagInfo = parseTag(it) }
            }
            2 -> ToolsTab(lastTag, tagInfo) { msg, err ->
                statusMessage = msg
                isError = err
                if (!err) lastTag?.let { tagInfo = parseTag(it) }
            }
        }
    }
}

@Composable
private fun ReadTab(tagInfo: TagInfo?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (tagInfo == null) {
            WaitingForTag()
            return@Column
        }

        // Tag details card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Tag Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow("UID", tagInfo.uid)
                InfoRow("Technologies", tagInfo.techList.joinToString(", "))
                InfoRow("NDEF", if (tagInfo.isNdef) "Yes" else "No")
                if (tagInfo.isNdef) {
                    InfoRow("Writable", if (tagInfo.isWritable) "Yes" else "Read-only")
                    InfoRow("Memory", "${tagInfo.usedSize} / ${tagInfo.maxSize} bytes")
                }
            }
        }

        // NDEF Records
        if (tagInfo.ndefRecords.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Content", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    tagInfo.ndefRecords.forEachIndexed { index, record ->
                        when (record) {
                            is ParsedRecord.TextRecord -> {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Column {
                                        Text("Text (${record.language})", style = MaterialTheme.typography.labelSmall)
                                        Text(record.text, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                            is ParsedRecord.UrlRecord -> {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Column {
                                        Text("URL", style = MaterialTheme.typography.labelSmall)
                                        Text(record.url, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                            is ParsedRecord.RawRecord -> {
                                Column {
                                    Text("Raw Record (TNF: ${record.tnf}, Type: ${record.type})", style = MaterialTheme.typography.labelSmall)
                                    Text(record.payload, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                        if (index < tagInfo.ndefRecords.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }
        }

        // Hex dump
        if (tagInfo.rawHex.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Hex Dump", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(tagInfo.rawHex, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun WriteTab(tag: Tag?, onResult: (String, Boolean) -> Unit) {
    var writeType by rememberSaveable { mutableStateOf("text") }
    var writeContent by rememberSaveable { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Write type selector
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = writeType == "text", onClick = { writeType = "text" }, label = { Text("Text") })
            FilterChip(selected = writeType == "url", onClick = { writeType = "url" }, label = { Text("URL") })
        }

        OutlinedTextField(
            value = writeContent,
            onValueChange = { writeContent = it },
            label = { Text(if (writeType == "text") "Text to write" else "URL to write") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )

        OutlinedButton(
            onClick = {
                if (tag == null) {
                    onResult("Tap an NFC tag first", true)
                } else if (writeContent.isBlank()) {
                    onResult("Enter content to write", true)
                } else {
                    showConfirmDialog = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Write to Tag")
        }

        if (tag == null) {
            WaitingForTag()
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Write to Tag?") },
            text = { Text("This will overwrite all data on the tag. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    tag?.let { t ->
                        val result = if (writeType == "text") writeTextToTag(t, writeContent)
                        else writeUrlToTag(t, writeContent)
                        result.fold(
                            onSuccess = { onResult("Written successfully!", false) },
                            onFailure = { onResult("Write failed: ${it.message}", true) },
                        )
                    }
                }) { Text("Write") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ToolsTab(tag: Tag?, tagInfo: TagInfo?, onResult: (String, Boolean) -> Unit) {
    var showFormatDialog by remember { mutableStateOf(false) }
    var showEraseDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (tag == null) {
            WaitingForTag()
            return@Column
        }

        OutlinedButton(
            onClick = { showFormatDialog = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Format Tag to NDEF")
        }

        OutlinedButton(
            onClick = { showEraseDialog = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Erase Tag Content")
        }

        // Tag info summary
        tagInfo?.let { info ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tag Summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow("UID", info.uid)
                    InfoRow("Technologies", info.techList.joinToString(", "))
                    InfoRow("NDEF", if (info.isNdef) "Yes" else "No")
                    InfoRow("Writable", if (info.isWritable) "Yes" else "Read-only")
                }
            }
        }
    }

    if (showFormatDialog) {
        AlertDialog(
            onDismissRequest = { showFormatDialog = false },
            title = { Text("Format Tag?") },
            text = { Text("This will format the tag to NDEF and erase any existing data. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showFormatDialog = false
                    tag?.let { t ->
                        formatTag(t).fold(
                            onSuccess = { onResult("Tag formatted successfully!", false) },
                            onFailure = { onResult("Format failed: ${it.message}", true) },
                        )
                    }
                }) { Text("Format") }
            },
            dismissButton = {
                TextButton(onClick = { showFormatDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showEraseDialog) {
        AlertDialog(
            onDismissRequest = { showEraseDialog = false },
            title = { Text("Erase Tag?") },
            text = { Text("This will erase all content on the tag. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showEraseDialog = false
                    tag?.let { t ->
                        eraseTag(t).fold(
                            onSuccess = { onResult("Tag erased successfully!", false) },
                            onFailure = { onResult("Erase failed: ${it.message}", true) },
                        )
                    }
                }) { Text("Erase") }
            },
            dismissButton = {
                TextButton(onClick = { showEraseDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun WaitingForTag() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.Nfc,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Waiting for NFC tag...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Hold a tag against the back of your device",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}
