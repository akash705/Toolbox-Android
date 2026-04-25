package com.toolbox.everyday.ocr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun OcrScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var recognized by remember { mutableStateOf("") }
    var blocksCount by remember { mutableStateOf(0) }
    var processing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            pickedUri = uri
            recognized = ""
            blocksCount = 0
            error = null
            preview = decodePreview(context, uri)
        }
    }

    LaunchedEffect(pickedUri) {
        val uri = pickedUri ?: return@LaunchedEffect
        processing = true
        error = null
        try {
            val text = withContext(Dispatchers.Default) { recognizeText(context, uri) }
            recognized = text.fullText
            blocksCount = text.blockCount
        } catch (t: Throwable) {
            error = t.message ?: "Could not recognize text."
        } finally {
            processing = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Button(
            onClick = {
                pickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (pickedUri == null) "Pick photo" else "Pick another photo")
        }

        preview?.let { bmp ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Selected image preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(bmp.width.toFloat() / bmp.height.coerceAtLeast(1)),
                )
            }
        }

        when {
            processing -> ProcessingCard()
            error != null -> ErrorCard(error!!)
            recognized.isNotEmpty() -> ResultCard(
                text = recognized,
                blockCount = blocksCount,
                onCopy = { copyText(context, recognized) },
                onShare = { shareText(context, recognized) },
            )
            pickedUri != null -> EmptyTextCard()
            else -> HintCard()
        }
    }
}

@Composable
private fun HintCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("How it works", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Pick any photo containing printed text — a sign, receipt, business card, document. Recognition runs entirely on-device using ML Kit's Latin-script model. No image leaves your phone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProcessingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.height(20.dp).aspectRatio(1f), strokeWidth = 2.dp)
            Spacer(Modifier.width(12.dp))
            Text("Recognizing text…", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Text(
            "Couldn't recognize text: $message",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun EmptyTextCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Text(
            "No text detected in this image. Try a clearer or more contrasted photo.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun ResultCard(
    text: String,
    blockCount: Int,
    onCopy: () -> Unit,
    onShare: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Recognized text — $blockCount block${if (blockCount == 1) "" else "s"}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCopy, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Copy")
                }
                FilledTonalButton(onClick = onShare, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Share")
                }
            }
        }
    }
}

private data class OcrText(val fullText: String, val blockCount: Int)

private suspend fun recognizeText(context: Context, uri: Uri): OcrText {
    val image = InputImage.fromFilePath(context, uri)
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    return suspendCancellableCoroutine { cont ->
        recognizer.process(image)
            .addOnSuccessListener { result ->
                cont.resume(OcrText(fullText = result.text, blockCount = result.textBlocks.size))
            }
            .addOnFailureListener { e ->
                cont.resumeWithException(e)
            }
        cont.invokeOnCancellation { recognizer.close() }
    }
}

private fun decodePreview(context: Context, uri: Uri): Bitmap? {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    val sample = run {
        val target = 1080
        val longEdge = maxOf(opts.outWidth, opts.outHeight).coerceAtLeast(1)
        var s = 1
        while (longEdge / (s * 2) >= target) s *= 2
        s
    }
    val full = BitmapFactory.Options().apply { inSampleSize = sample }
    return context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, full)
    }
}

private fun copyText(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("OCR text", text))
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share text"))
}
