package com.toolbox.everyday.qrgen

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import com.toolbox.core.media.MediaStoreWriter
import com.toolbox.core.sharing.ImageSharer

@Composable
fun QrGeneratorScreen(viewModel: QrGeneratorViewModel = viewModel()) {
    val context = LocalContext.current
    val fields by viewModel.fields.collectAsState()
    val preview by viewModel.preview.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Content-type picker: horizontally-scrollable chip row.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QrContentType.entries.forEach { type ->
                FilterChip(
                    selected = fields.type == type,
                    onClick = { viewModel.selectType(type) },
                    label = { Text(type.label) },
                )
            }
        }

        // Per-type input form.
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (fields.type) {
                    QrContentType.Text -> InputField("Text", fields.text) { v -> viewModel.update { it.copy(text = v) } }
                    QrContentType.Url -> InputField("URL", fields.url, KeyboardType.Uri) { v -> viewModel.update { it.copy(url = v) } }
                    QrContentType.Email -> {
                        InputField("Email address", fields.emailAddress, KeyboardType.Email) { v -> viewModel.update { it.copy(emailAddress = v) } }
                        InputField("Subject (optional)", fields.emailSubject) { v -> viewModel.update { it.copy(emailSubject = v) } }
                        InputField("Body (optional)", fields.emailBody) { v -> viewModel.update { it.copy(emailBody = v) } }
                    }
                    QrContentType.Phone -> InputField("Phone number", fields.phone, KeyboardType.Phone) { v -> viewModel.update { it.copy(phone = v) } }
                    QrContentType.Sms -> {
                        InputField("Phone number", fields.smsNumber, KeyboardType.Phone) { v -> viewModel.update { it.copy(smsNumber = v) } }
                        InputField("Message (optional)", fields.smsMessage) { v -> viewModel.update { it.copy(smsMessage = v) } }
                    }
                    QrContentType.VCard -> {
                        InputField("Name", fields.vcardName) { v -> viewModel.update { it.copy(vcardName = v) } }
                        InputField("Phone (optional)", fields.vcardPhone, KeyboardType.Phone) { v -> viewModel.update { it.copy(vcardPhone = v) } }
                        InputField("Email (optional)", fields.vcardEmail, KeyboardType.Email) { v -> viewModel.update { it.copy(vcardEmail = v) } }
                        InputField("Organization (optional)", fields.vcardOrg) { v -> viewModel.update { it.copy(vcardOrg = v) } }
                    }
                    QrContentType.Geo -> {
                        InputField("Latitude", fields.geoLat, KeyboardType.Number) { v -> viewModel.update { it.copy(geoLat = v) } }
                        InputField("Longitude", fields.geoLng, KeyboardType.Number) { v -> viewModel.update { it.copy(geoLng = v) } }
                    }
                }
            }
        }

        // Preview + actions.
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (val p = preview) {
                    is QrGeneratorViewModel.Preview.Valid -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(16.dp),
                        ) {
                            Image(
                                bitmap = p.bitmap.asImageBitmap(),
                                contentDescription = "Generated QR code",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                ImageSharer.shareBitmap(context, p.bitmap, "qr_code", "Share QR code")
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Share QR code")
                        }
                        Spacer(Modifier.height(8.dp))
                        FilledTonalButton(
                            onClick = {
                                val msg = try {
                                    val result = MediaStoreWriter.saveJpeg(
                                        context = context,
                                        bitmap = p.bitmap,
                                        subfolder = "Toolbox",
                                        baseName = "qr_code",
                                        quality = 100,
                                    )
                                    "Saved to ${result.displayPath}"
                                } catch (e: Exception) {
                                    "Couldn't save: ${e.message ?: "unknown error"}"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Save to gallery")
                        }
                    }
                    is QrGeneratorViewModel.Preview.Invalid -> statusText(p.message, error = true)
                    QrGeneratorViewModel.Preview.TooLong ->
                        statusText("Content is too long for a QR code — shorten it.", error = true)
                    QrGeneratorViewModel.Preview.Placeholder ->
                        statusText("Enter content above to generate a QR code.", error = false)
                }
            }
        }
    }
}

@Composable
private fun InputField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = label != "Body (optional)" && label != "Message (optional)",
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun statusText(text: String, error: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 24.dp),
    )
}
