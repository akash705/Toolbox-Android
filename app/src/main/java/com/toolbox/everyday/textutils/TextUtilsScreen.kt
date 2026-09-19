package com.toolbox.everyday.textutils

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toolbox.core.ui.copyToClipboard
import java.util.Locale

@Composable
fun TextUtilsScreen() {
    val context = LocalContext.current
    var text by rememberSaveable { mutableStateOf("") }

    val chars = text.length
    val words = if (text.isBlank()) 0 else text.trim().split(Regex("\\s+")).size
    val lines = if (text.isEmpty()) 0 else text.lines().size

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Text") },
            modifier = Modifier.fillMaxWidth().height(180.dp),
        )

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                stat("Characters", chars)
                stat("Words", words)
                stat("Lines", lines)
            }
        }

        Text("Transform", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            chip("UPPER") { text = text.uppercase() }
            chip("lower") { text = text.lowercase() }
            chip("Title") { text = toTitleCase(text) }
            chip("Sentence") { text = toSentenceCase(text) }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            chip("Trim spaces") { text = text.replace(Regex("[ \\t]+"), " ").trim() }
            chip("No line breaks") { text = text.replace(Regex("\\s*\\n\\s*"), " ").trim() }
            chip("Slugify") { text = slugify(text) }
            chip("Reverse") { text = text.reversed() }
        }

        Spacer(Modifier.height(4.dp))
        OutlinedButton(
            onClick = { copyToClipboard(context, "Text", text) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Copy result") }
    }
}

@Composable
private fun stat(label: String, value: Int) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun chip(label: String, onClick: () -> Unit) {
    AssistChip(onClick = onClick, label = { Text(label) })
}

private fun toTitleCase(s: String): String =
    s.split(" ").joinToString(" ") { w ->
        w.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

private fun toSentenceCase(s: String): String {
    val lower = s.lowercase()
    val sb = StringBuilder()
    var capitalizeNext = true
    for (ch in lower) {
        if (capitalizeNext && ch.isLetter()) {
            sb.append(ch.uppercaseChar()); capitalizeNext = false
        } else {
            sb.append(ch)
            if (ch == '.' || ch == '!' || ch == '?') capitalizeNext = true
        }
    }
    return sb.toString()
}

private fun slugify(s: String): String =
    s.trim().lowercase()
        .replace(Regex("[^a-z0-9\\s-]"), "")
        .replace(Regex("[\\s-]+"), "-")
        .trim('-')
