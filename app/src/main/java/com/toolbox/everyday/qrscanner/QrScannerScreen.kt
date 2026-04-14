package com.toolbox.everyday.qrscanner

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.toolbox.core.camera.CameraPreview
import com.toolbox.core.permission.PermissionGate

@Composable
fun QrScannerScreen() {
    PermissionGate(
        permission = android.Manifest.permission.CAMERA,
        rationale = "The scanner needs camera access to read QR codes and barcodes.",
    ) {
        QrScannerContent()
    }
}

@Composable
private fun QrScannerContent() {
    val context = LocalContext.current
    var scannedResult by remember { mutableStateOf<ScanResult?>(null) }

    val analyzer = remember {
        QrCodeAnalyzer { result ->
            if (scannedResult == null) {
                scannedResult = result
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        // Camera preview with scan overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(16.dp)),
        ) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                imageAnalyzer = analyzer,
            )

            // Scan frame overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val frameSize = size.minDimension * 0.65f
                val left = (size.width - frameSize) / 2
                val top = (size.height - frameSize) / 2
                val cornerLength = 40f
                val strokeWidth = 4f
                val cornerColor = Color(0xFF2196F3)

                // Draw corner brackets
                val corners = listOf(
                    // Top-left
                    Offset(left, top) to listOf(
                        Offset(left + cornerLength, top),
                        Offset(left, top + cornerLength),
                    ),
                    // Top-right
                    Offset(left + frameSize, top) to listOf(
                        Offset(left + frameSize - cornerLength, top),
                        Offset(left + frameSize, top + cornerLength),
                    ),
                    // Bottom-left
                    Offset(left, top + frameSize) to listOf(
                        Offset(left + cornerLength, top + frameSize),
                        Offset(left, top + frameSize - cornerLength),
                    ),
                    // Bottom-right
                    Offset(left + frameSize, top + frameSize) to listOf(
                        Offset(left + frameSize - cornerLength, top + frameSize),
                        Offset(left + frameSize, top + frameSize - cornerLength),
                    ),
                )

                for ((corner, ends) in corners) {
                    for (end in ends) {
                        drawLine(
                            color = cornerColor,
                            start = corner,
                            end = end,
                            strokeWidth = strokeWidth,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Result card
        if (scannedResult != null) {
            val scan = scannedResult!!
            val result = scan.text
            val isUrl = result.startsWith("http://") || result.startsWith("https://")

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = if (isUrl) "DETECTED URL" else formatLabel(scan.format).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = result,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (isUrl) {
                            Button(
                                onClick = {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(result)))
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Default.OpenInBrowser, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open URL")
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("QR Result", result))
                                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                            },
                            modifier = if (!isUrl) Modifier.weight(1f) else Modifier,
                        ) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, result)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { scannedResult = null },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Scan Again")
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Point camera at a QR code or barcode",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private data class ScanResult(val text: String, val format: BarcodeFormat)

private class QrCodeAnalyzer(
    private val onResult: (ScanResult) -> Unit,
) : ImageAnalysis.Analyzer {
    private val reader = MultiFormatReader()

    override fun analyze(image: ImageProxy) {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val source = PlanarYUVLuminanceSource(
            bytes,
            image.width,
            image.height,
            0, 0,
            image.width,
            image.height,
            false,
        )
        val bitmap = BinaryBitmap(HybridBinarizer(source))

        try {
            val result = reader.decode(bitmap)
            onResult(ScanResult(result.text, result.barcodeFormat))
        } catch (_: NotFoundException) {
            // No code found in this frame
        } finally {
            image.close()
        }
    }
}

private fun formatLabel(format: BarcodeFormat): String = when (format) {
    BarcodeFormat.QR_CODE -> "QR Code"
    BarcodeFormat.EAN_13 -> "EAN-13"
    BarcodeFormat.EAN_8 -> "EAN-8"
    BarcodeFormat.UPC_A -> "UPC-A"
    BarcodeFormat.UPC_E -> "UPC-E"
    BarcodeFormat.CODE_128 -> "Code 128"
    BarcodeFormat.CODE_39 -> "Code 39"
    BarcodeFormat.CODE_93 -> "Code 93"
    BarcodeFormat.ITF -> "ITF"
    BarcodeFormat.PDF_417 -> "PDF 417"
    BarcodeFormat.DATA_MATRIX -> "Data Matrix"
    BarcodeFormat.AZTEC -> "Aztec"
    BarcodeFormat.CODABAR -> "Codabar"
    else -> format.name
}
