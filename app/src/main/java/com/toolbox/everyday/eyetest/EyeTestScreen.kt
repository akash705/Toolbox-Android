package com.toolbox.everyday.eyetest

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun EyeTestScreen() {
    var tab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(tab == 0, onClick = { tab = 0 }, label = { Text("Acuity") })
            FilterChip(tab == 1, onClick = { tab = 1 }, label = { Text("Astigmatism") })
            FilterChip(tab == 2, onClick = { tab = 2 }, label = { Text("Color") })
        }
        Spacer(Modifier.height(16.dp))

        when (tab) {
            0 -> AcuityChart()
            1 -> AstigmatismFan()
            else -> ColorGame()
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "For self-checks only — this is not a medical diagnosis. See an eye-care professional for real testing.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ISO/IEC 7810 ID-1 card (credit/debit/most national ID cards) is exactly 85.6 mm wide.
private const val CARD_WIDTH_MM = 85.6f
private const val CARD_ASPECT = 85.6f / 53.98f // ID-1 width / height
// At a 40 cm near-test distance a 20/20 optotype's overall height is ~0.58 mm; each Snellen line
// scales linearly with its denominator. Monospace cap height is ~0.7 of the em, so we upsize the
// font by 1/0.7 to hit the target physical letter height.
private const val MM_PER_SNELLEN_DENOM = 0.582f / 20f
private const val CAP_HEIGHT_RATIO = 0.7f

@Composable
private fun AcuityChart() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("eyetest", Context.MODE_PRIVATE) }
    var pxPerMm by remember {
        mutableStateOf(prefs.getFloat("px_per_mm", 0f).takeIf { it > 0f })
    }

    val calibrated = pxPerMm
    if (calibrated == null) {
        CalibrationCard(onCalibrated = { v ->
            prefs.edit().putFloat("px_per_mm", v).apply()
            pxPerMm = v
        })
    } else {
        CalibratedAcuity(pxPerMm = calibrated, onRecalibrate = { pxPerMm = null })
    }
}

@Composable
private fun CalibrationCard(onCalibrated: (Float) -> Unit) {
    val density = LocalDensity.current
    var widthDp by remember { mutableFloatStateOf(260f) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            "One-time calibration: hold a bank card or ID card flat against the screen and drag the slider until the outline matches the card's width exactly.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .width(widthDp.dp)
                .height((widthDp / CARD_ASPECT).dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text("Match a real card", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.height(20.dp))
        Slider(value = widthDp, onValueChange = { widthDp = it }, valueRange = 150f..380f)
        Spacer(Modifier.height(8.dp))
        androidx.compose.material3.Button(
            onClick = {
                val widthPx = with(density) { widthDp.dp.toPx() }
                onCalibrated(widthPx / CARD_WIDTH_MM)
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("This matches my card") }
    }
}

@Composable
private fun CalibratedAcuity(pxPerMm: Float, onRecalibrate: () -> Unit) {
    val density = LocalDensity.current
    // (letters, Snellen denominator) — 20/denom at a 40 cm hold.
    val rows = listOf(
        "E" to 200, "F P" to 100, "T O Z" to 63, "L P E D" to 40,
        "P E C F D" to 32, "E D F C Z P" to 25, "F E L O P Z D" to 20,
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            "Hold the phone about 40 cm (16 in) away, cover one eye, and read down. The lowest row you can read cleanly is your rough acuity.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        rows.forEach { (letters, denom) ->
            val heightPx = denom * MM_PER_SNELLEN_DENOM * pxPerMm
            // Neutralise both density and the user's font-scale so the size is truly physical.
            val fontSp = (heightPx / CAP_HEIGHT_RATIO / (density.density * density.fontScale)).sp
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                Text(
                    "20/$denom",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(48.dp),
                )
                Text(letters, fontSize = fontSp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Recalibrate card",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { onRecalibrate() }.padding(8.dp),
        )
    }
}

@Composable
private fun AstigmatismFan() {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("Cover one eye and look at the center. If some lines look darker or sharper than others, you may have astigmatism.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(24.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = size.minDimension / 2f
            for (i in 0 until 18) {
                val a = Math.toRadians((i * 10).toDouble())
                val dx = (cos(a) * r).toFloat()
                val dy = (sin(a) * r).toFloat()
                drawLine(onSurface, Offset(cx - dx, cy - dy), Offset(cx + dx, cy + dy), strokeWidth = 3f)
            }
        }
    }
}

@Composable
private fun ColorGame() {
    var level by remember { mutableIntStateOf(1) }
    var baseHue by remember { mutableStateOf(Random.nextInt(360).toFloat()) }
    var oddIndex by remember { mutableIntStateOf(Random.nextInt(16)) }
    var message by remember { mutableStateOf("Tap the tile that looks different.") }

    fun regenerate(harder: Boolean) {
        if (harder) level++
        baseHue = Random.nextInt(360).toFloat()
        oddIndex = Random.nextInt(16)
    }

    val delta = (30f / (level + 1)).coerceAtLeast(2f) // shrinking hue difference
    val base = Color.hsv(baseHue, 0.6f, 0.85f)
    val odd = Color.hsv((baseHue + delta) % 360f, 0.6f, 0.85f)

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("Level $level", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        for (r in 0 until 4) {
            Row {
                for (c in 0 until 4) {
                    val idx = r * 4 + c
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(64.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (idx == oddIndex) odd else base)
                            .clickable {
                                if (idx == oddIndex) {
                                    message = "Correct! Getting harder…"
                                    regenerate(harder = true)
                                } else {
                                    message = "Not quite — try again."
                                }
                            },
                    )
                }
            }
        }
    }
}
