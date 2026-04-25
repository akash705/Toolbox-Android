package com.toolbox.lighting.screengrid

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas

private enum class GridColor(val label: String, val fill: Color, val line: Color) {
    BLACK("Black", Color.Black, Color(0xFF888888)),
    WHITE("White", Color.White, Color(0xFFAAAAAA)),
    RED("Red", Color(0xFFD32F2F), Color.White),
    GREEN("Green", Color(0xFF2E7D32), Color.White),
    BLUE("Blue", Color(0xFF1565C0), Color.White),
    GRAY("Gray", Color(0xFF808080), Color.White),
}

@Composable
fun ScreenGridScreen() {
    var color by rememberSaveable { mutableStateOf(GridColor.WHITE) }
    var showGrid by rememberSaveable { mutableStateOf(true) }

    // Keep the screen at full brightness while the tool is open so dust/scratches show clearly.
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val params = activity?.window?.attributes
        val previousBrightness = params?.screenBrightness
        params?.let {
            it.screenBrightness = 1f
            activity.window.attributes = it
        }
        onDispose {
            params?.let {
                it.screenBrightness = previousBrightness ?: -1f
                activity.window.attributes = it
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(color.fill),
        ) {
            if (showGrid) GridOverlay(color.line)
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Background", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.padding(top = 4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    GridColor.entries.forEach { c ->
                        FilterChip(
                            selected = color == c,
                            onClick = { color = c },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 6.dp)
                                            .width(12.dp)
                                            .fillMaxHeight()
                                            .clip(CircleShape)
                                            .background(c.fill),
                                    )
                                    Text(c.label)
                                }
                            },
                        )
                    }
                }
                Spacer(Modifier.padding(top = 8.dp))
                FilterChip(
                    selected = showGrid,
                    onClick = { showGrid = !showGrid },
                    label = { Text(if (showGrid) "Grid: on" else "Grid: off") },
                )
                Spacer(Modifier.padding(top = 8.dp))
                Text(
                    "Hold the device close to the screen at an angle. Switch backgrounds to surface dust on light areas, scratches on dark, and pixel issues with red/green/blue.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun GridOverlay(lineColor: Color) {
    val density = LocalDensity.current
    Canvas(modifier = Modifier.fillMaxSize()) {
        val stepPx = with(density) { 24.dp.toPx() }
        val stroke = Stroke(width = with(density) { 0.5.dp.toPx() })
        val brush = SolidColor(lineColor.copy(alpha = 0.5f))

        var x = 0f
        while (x < size.width) {
            drawLine(brush, Offset(x, 0f), Offset(x, size.height), strokeWidth = stroke.width)
            x += stepPx
        }
        var y = 0f
        while (y < size.height) {
            drawLine(brush, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke.width)
            y += stepPx
        }
    }
}
