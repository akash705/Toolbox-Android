package com.toolbox.measurement.spectrum

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.toolbox.core.permission.PermissionGate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// Band colors matching Stitch design
private val BandColors = listOf(
    Color(0xFF7B1FA2), // Sub-bass - purple
    Color(0xFF1976D2), // Bass - blue
    Color(0xFF00897B), // Low-mid - teal
    Color(0xFF388E3C), // Mid - green
    Color(0xFFF57C00), // High-mid - orange
    Color(0xFFE64A19), // Presence - deep orange
    Color(0xFFD32F2F), // Brilliance - red
)

private val ChartBackground = Color(0xFF1E1E2E)

@Composable
fun SpectrumScreen() {
    PermissionGate(
        permission = Manifest.permission.RECORD_AUDIO,
        rationale = "The spectrum analyzer needs microphone access to analyze audio frequencies.",
    ) {
        SpectrumContent()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpectrumContent() {
    val context = LocalContext.current

    var isAnalyzing by remember { mutableStateOf(false) }
    var bandMagnitudes by remember { mutableStateOf(FloatArray(7) { -60f }) }
    var peakFrequency by remember { mutableFloatStateOf(0f) }
    var peakLevel by remember { mutableFloatStateOf(-60f) }
    var avgLevel by remember { mutableFloatStateOf(-60f) }
    var peakHolds by remember { mutableStateOf(FloatArray(7) { -60f }) }
    var selectedView by remember { mutableIntStateOf(0) } // 0 = Bars, 1 = Waterfall
    val waterfallHistory = remember { mutableListOf<FloatArray>() }

    val fftProcessor = remember { FFTProcessor(1024) }
    val textMeasurer = rememberTextMeasurer()

    // Audio recording and FFT processing
    DisposableEffect(isAnalyzing) {
        if (!isAnalyzing) {
            return@DisposableEffect onDispose {}
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            isAnalyzing = false
            return@DisposableEffect onDispose {}
        }

        val sampleRate = 44100
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(2048)

        val audioRecord = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
            )
        } catch (_: SecurityException) {
            null
        }

        if (audioRecord == null || audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord?.release()
            isAnalyzing = false
            return@DisposableEffect onDispose {}
        }

        audioRecord.startRecording()

        val scope = CoroutineScope(Dispatchers.IO)
        val job: Job = scope.launch {
            val buffer = ShortArray(1024)
            val floatBuffer = FloatArray(1024)
            while (isActive) {
                val read = audioRecord.read(buffer, 0, buffer.size)
                if (read > 0) {
                    for (i in 0 until read) {
                        floatBuffer[i] = buffer[i].toFloat() / Short.MAX_VALUE
                    }

                    val result = fftProcessor.process(floatBuffer, sampleRate)
                    bandMagnitudes = result.bandMagnitudes
                    peakFrequency = result.peakFrequency
                    peakLevel = result.peakLevel
                    avgLevel = result.averageLevel

                    // Update peak holds (decay slowly)
                    val currentPeaks = peakHolds.copyOf()
                    for (i in currentPeaks.indices) {
                        if (result.bandMagnitudes[i] > currentPeaks[i]) {
                            currentPeaks[i] = result.bandMagnitudes[i]
                        } else {
                            currentPeaks[i] = (currentPeaks[i] - 0.5f).coerceAtLeast(result.bandMagnitudes[i])
                        }
                    }
                    peakHolds = currentPeaks

                    // Waterfall history
                    if (selectedView == 1) {
                        waterfallHistory.add(0, result.bandMagnitudes.copyOf())
                        if (waterfallHistory.size > 50) waterfallHistory.removeAt(waterfallHistory.size - 1)
                    }
                }
            }
        }

        onDispose {
            job.cancel()
            audioRecord.stop()
            audioRecord.release()
        }
    }

    val bandLabels = FFTProcessor.FrequencyBand.entries.map { it.label }
    val freqLabels = listOf("20Hz", "100Hz", "500Hz", "1kHz", "5kHz", "20kHz")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Spectrum chart
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            colors = CardDefaults.cardColors(containerColor = ChartBackground),
            shape = RoundedCornerShape(16.dp),
        ) {
            if (selectedView == 0) {
                // Bar chart view
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                ) {
                    drawBarSpectrum(bandMagnitudes, peakHolds, textMeasurer)
                }
            } else {
                // Waterfall view
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                ) {
                    drawWaterfall(waterfallHistory, textMeasurer)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // View toggle chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilterChip(
                selected = selectedView == 0,
                onClick = { selectedView = 0 },
                label = { Text("Bars") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
            FilterChip(
                selected = selectedView == 1,
                onClick = {
                    selectedView = 1
                    waterfallHistory.clear()
                },
                label = { Text("Waterfall") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legend card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            FlowRow(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                bandLabels.forEachIndexed { index, label ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(modifier = Modifier.size(10.dp)) {
                            drawCircle(color = BandColors[index])
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SpectrumStatCard(
                label = "PEAK FREQ",
                value = formatFrequency(peakFrequency),
                modifier = Modifier.weight(1f),
            )
            SpectrumStatCard(
                label = "PEAK LEVEL",
                value = "${peakLevel.toInt()} dB",
                modifier = Modifier.weight(1f),
            )
            SpectrumStatCard(
                label = "AVG LEVEL",
                value = "${avgLevel.toInt()} dB",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Start/Stop button
        Button(
            onClick = {
                if (isAnalyzing) {
                    isAnalyzing = false
                    bandMagnitudes = FloatArray(7) { -60f }
                    peakHolds = FloatArray(7) { -60f }
                    waterfallHistory.clear()
                } else {
                    isAnalyzing = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isAnalyzing) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
            ),
        ) {
            Icon(
                imageVector = if (isAnalyzing) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isAnalyzing) "Stop Analysis" else "Start Analysis",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Requires microphone access for audio analysis",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SpectrumStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun DrawScope.drawBarSpectrum(
    magnitudes: FloatArray,
    peakHolds: FloatArray,
    textMeasurer: TextMeasurer,
) {
    val bandCount = magnitudes.size
    val chartLeft = 30f
    val chartBottom = size.height - 20f
    val chartTop = 10f
    val chartWidth = size.width - chartLeft - 10f
    val chartHeight = chartBottom - chartTop

    val barWidth = chartWidth / bandCount * 0.7f
    val barGap = chartWidth / bandCount * 0.3f

    // Y-axis labels
    val dbLabels = listOf("0", "-20", "-40", "-60")
    val labelStyle = TextStyle(fontSize = 8.sp, color = Color.White.copy(alpha = 0.5f))

    for ((i, label) in dbLabels.withIndex()) {
        val y = chartTop + chartHeight * i / (dbLabels.size - 1)
        val measured = textMeasurer.measure(label, labelStyle)
        drawText(measured, topLeft = Offset(0f, y - measured.size.height / 2))
        // Grid line
        drawLine(
            Color.White.copy(alpha = 0.1f),
            Offset(chartLeft, y),
            Offset(chartLeft + chartWidth, y),
            strokeWidth = 1f,
        )
    }

    // X-axis freq labels
    val xLabels = listOf("20Hz", "100Hz", "500Hz", "1kHz", "5kHz", "10kHz", "20kHz")
    for (i in 0 until bandCount) {
        val x = chartLeft + i * (barWidth + barGap) + barWidth / 2
        if (i < xLabels.size) {
            val measured = textMeasurer.measure(xLabels[i], labelStyle)
            drawText(measured, topLeft = Offset(x - measured.size.width / 2, chartBottom + 4f))
        }
    }

    // Draw bars
    for (i in 0 until bandCount) {
        val db = magnitudes[i].coerceIn(-60f, 0f)
        val barHeight = ((db + 60f) / 60f) * chartHeight
        val x = chartLeft + i * (barWidth + barGap)
        val y = chartBottom - barHeight

        drawRoundRect(
            color = BandColors[i],
            topLeft = Offset(x, y),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(4f, 4f),
        )

        // Peak hold marker
        val peakDb = peakHolds[i].coerceIn(-60f, 0f)
        val peakY = chartBottom - ((peakDb + 60f) / 60f) * chartHeight
        drawLine(
            color = BandColors[i].copy(alpha = 0.8f),
            start = Offset(x, peakY),
            end = Offset(x + barWidth, peakY),
            strokeWidth = 2f,
        )
    }
}

private fun DrawScope.drawWaterfall(
    history: List<FloatArray>,
    textMeasurer: TextMeasurer,
) {
    if (history.isEmpty()) {
        val style = TextStyle(fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
        val measured = textMeasurer.measure("Waiting for data...", style)
        drawText(measured, topLeft = Offset(size.width / 2 - measured.size.width / 2, size.height / 2))
        return
    }

    val bandCount = history.firstOrNull()?.size ?: return
    val rowHeight = size.height / 50f
    val colWidth = size.width / bandCount

    for ((rowIdx, row) in history.withIndex()) {
        val y = rowIdx * rowHeight
        for (bandIdx in 0 until bandCount.coerceAtMost(row.size)) {
            val db = row[bandIdx].coerceIn(-60f, 0f)
            val intensity = (db + 60f) / 60f // 0 to 1
            val color = BandColors[bandIdx].copy(alpha = intensity.coerceIn(0.05f, 1f))
            drawRect(
                color = color,
                topLeft = Offset(bandIdx * colWidth, y),
                size = Size(colWidth, rowHeight),
            )
        }
    }
}

private fun formatFrequency(hz: Float): String = when {
    hz < 1f -> "-- Hz"
    hz >= 1000f -> String.format("%.1f kHz", hz / 1000f)
    else -> "${hz.toInt()} Hz"
}
