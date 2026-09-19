package com.toolbox.everyday.eyetest

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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

@Composable
private fun AcuityChart() {
    val rows = listOf("E" to 60.sp, "F P" to 44.sp, "T O Z" to 32.sp, "L P E D" to 24.sp, "P E C F D" to 18.sp, "E D F C Z P" to 13.sp)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("Hold the phone about 2 m (6.5 ft) away, cover one eye, read down.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        rows.forEach { (letters, size) ->
            Text(letters, fontSize = size, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 4.sp)
            Spacer(Modifier.height(12.dp))
        }
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
