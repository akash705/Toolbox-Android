package com.toolbox.measurement.ruler

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RulerScreen() {
    val density = LocalDensity.current
    val xdpi = with(density) { density.density * 160f } // approximate DPI
    val pxPerMm = xdpi / 25.4f
    val pxPerInch = xdpi

    var scrollOffset by remember { mutableFloatStateOf(0f) }
    val textMeasurer = rememberTextMeasurer()

    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val outlineColor = MaterialTheme.colorScheme.outline

    Column(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        scrollOffset -= dragAmount.y
                        if (scrollOffset < 0f) scrollOffset = 0f
                    }
                },
        ) {
            val width = size.width
            val height = size.height
            val rulerLengthMm = 500 // 50cm ruler
            val totalPx = rulerLengthMm * pxPerMm

            // Draw cm scale on left side
            for (mm in 0..rulerLengthMm) {
                val y = mm * pxPerMm - scrollOffset
                if (y < -10f || y > height + 10f) continue

                val tickLength = when {
                    mm % 10 == 0 -> width * 0.15f  // cm mark
                    mm % 5 == 0 -> width * 0.1f    // half cm
                    else -> width * 0.06f           // mm
                }
                val strokeWidth = if (mm % 10 == 0) 2f else 1f

                drawLine(
                    color = onSurfaceColor,
                    start = Offset(0f, y),
                    end = Offset(tickLength, y),
                    strokeWidth = strokeWidth,
                )

                if (mm % 10 == 0) {
                    val cm = mm / 10
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "$cm",
                        topLeft = Offset(tickLength + 8f, y - 14f),
                    )
                }
            }

            // Draw inch scale on right side
            val rulerLengthInches = (rulerLengthMm / 25.4).toInt() + 1
            for (sixteenth in 0..rulerLengthInches * 16) {
                val inches = sixteenth / 16.0
                val y = (inches * pxPerInch).toFloat() - scrollOffset
                if (y < -10f || y > height + 10f) continue

                val tickLength = when {
                    sixteenth % 16 == 0 -> width * 0.15f  // inch
                    sixteenth % 8 == 0 -> width * 0.1f    // half inch
                    sixteenth % 4 == 0 -> width * 0.07f   // quarter
                    else -> width * 0.04f                  // eighth/sixteenth
                }
                val strokeWidth = if (sixteenth % 16 == 0) 2f else 1f

                drawLine(
                    color = outlineColor,
                    start = Offset(width, y),
                    end = Offset(width - tickLength, y),
                    strokeWidth = strokeWidth,
                )

                if (sixteenth % 16 == 0) {
                    val inch = sixteenth / 16
                    val textLayout = textMeasurer.measure("$inch\"")
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "$inch\"",
                        topLeft = Offset(width - tickLength - textLayout.size.width - 8f, y - 14f),
                    )
                }
            }

            // Draw zero line
            val zeroY = -scrollOffset
            if (zeroY >= 0f) {
                drawLine(
                    color = primaryColor,
                    start = Offset(0f, zeroY),
                    end = Offset(width, zeroY),
                    strokeWidth = 3f,
                )
            }
        }

        // Info text
        Text(
            text = "Drag to scroll • cm on left, inches on right",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
    }
}
