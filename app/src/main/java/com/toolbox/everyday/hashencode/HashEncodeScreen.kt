package com.toolbox.everyday.hashencode

import android.util.Base64
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toolbox.core.ui.copyToClipboard
import java.net.URLEncoder
import java.security.MessageDigest

@Composable
fun HashEncodeScreen() {
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = input, onValueChange = { input = it }, label = { Text("Text") },
            modifier = Modifier.fillMaxWidth().height(120.dp),
        )
        Text("Tap a result to copy", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        val bytes = input.toByteArray(Charsets.UTF_8)
        resultRow(context, "MD5", if (input.isEmpty()) "" else hash("MD5", bytes))
        resultRow(context, "SHA-1", if (input.isEmpty()) "" else hash("SHA-1", bytes))
        resultRow(context, "SHA-256", if (input.isEmpty()) "" else hash("SHA-256", bytes))
        resultRow(context, "Base64", if (input.isEmpty()) "" else Base64.encodeToString(bytes, Base64.NO_WRAP))
        resultRow(context, "URL-encoded", if (input.isEmpty()) "" else URLEncoder.encode(input, "UTF-8"))
    }
}

@Composable
private fun resultRow(context: android.content.Context, label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = value.isNotEmpty()) {
            copyToClipboard(context, label, value)
        },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Text(
                value.ifEmpty { "—" },
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

private fun hash(algorithm: String, bytes: ByteArray): String =
    MessageDigest.getInstance(algorithm).digest(bytes).joinToString("") { "%02x".format(it) }
