package com.toolbox.measurement.ruler

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class RulerUnit { CM, IN }

@Composable
fun RulerScreen() {
    var unit by remember { mutableStateOf(RulerUnit.CM) }
    var scrollOffsetPx by remember { mutableFloatStateOf(0f) }

    val context = LocalContext.current
    // xdpi is the actual physical DPI reported by the hardware.
    val pxPerMm = context.resources.displayMetrics.xdpi / 25.4f

    val measuredMm = scrollOffsetPx / pxPerMm
    val readout = when (unit) {
        RulerUnit.CM -> "%.1f cm".format(measuredMm / 10f)
        RulerUnit.IN -> "%.2f in".format(measuredMm / 25.4f)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            SegmentedButton(
                selected = unit == RulerUnit.CM,
                onClick = { unit = RulerUnit.CM },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text("cm") }
            SegmentedButton(
                selected = unit == RulerUnit.IN,
                onClick = { unit = RulerUnit.IN },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text("in") }
        }

        RulerStrip(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            unit = unit,
            scrollOffsetPx = scrollOffsetPx,
            pxPerMm = pxPerMm,
            onScroll = { delta ->
                scrollOffsetPx = (scrollOffsetPx - delta).coerceAtLeast(0f)
            },
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "OBJECT LENGTH",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = readout,
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun RulerStrip(
    modifier: Modifier,
    unit: RulerUnit,
    scrollOffsetPx: Float,
    pxPerMm: Float,
    onScroll: (Float) -> Unit,
) {
    val textMeasurer = rememberTextMeasurer()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val outlineColor = MaterialTheme.colorScheme.outline
    val surfaceColor = MaterialTheme.colorScheme.surface

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                onScroll(dragAmount.x)
            }
        },
    ) {
        val width = size.width
        val height = size.height

        drawRect(color = surfaceColor)
        drawLine(color = outlineColor, start = Offset(0f, 0f), end = Offset(width, 0f), strokeWidth = 2f)
        drawLine(color = outlineColor, start = Offset(0f, height), end = Offset(width, height), strokeWidth = 2f)

        // The 0 mark starts at the center (width/2) so the fixed pointer always reads
        // scrollOffsetPx/pxPerMm. Ticks to the right of center are positive measurements.
        fun mmToX(mm: Float) = width / 2f + mm * pxPerMm - scrollOffsetPx

        when (unit) {
            RulerUnit.CM -> {
                // Iterate every mm up to 500mm (50cm)
                for (mmInt in 0..500) {
                    val x = mmToX(mmInt.toFloat())
                    if (x < -10f || x > width + 10f) continue

                    val isMajor = mmInt % 10 == 0
                    val isMid = mmInt % 5 == 0 && !isMajor

                    val tickHeight = when {
                        isMajor -> height * 0.50f
                        isMid -> height * 0.30f
                        else -> height * 0.15f
                    }
                    drawLine(
                        color = onSurfaceColor,
                        start = Offset(x, 0f),
                        end = Offset(x, tickHeight),
                        strokeWidth = if (isMajor) 2f else 1f,
                    )
                    if (isMajor) {
                        val label = "${mmInt / 10}"
                        val textLayout = textMeasurer.measure(
                            label,
                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                        )
                        drawText(
                            textMeasurer = textMeasurer,
                            text = label,
                            topLeft = Offset(x - textLayout.size.width / 2f, tickHeight + 4f),
                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                        )
                    }
                }
            }
            RulerUnit.IN -> {
                // Iterate in exact 1/16-inch steps to avoid accumulated mm-rounding drift.
                // 1 inch = 25.4mm exactly.
                val maxSixteenths = 20 * 16  // 20 inches
                for (sixteenths in 0..maxSixteenths) {
                    val mm = sixteenths * 25.4f / 16f
                    val x = mmToX(mm)
                    if (x < -10f || x > width + 10f) continue

                    val isMajor = sixteenths % 16 == 0       // whole inch
                    val isMid = sixteenths % 8 == 0 && !isMajor  // half inch

                    val tickHeight = when {
                        isMajor -> height * 0.50f
                        isMid -> height * 0.30f
                        else -> height * 0.15f
                    }
                    drawLine(
                        color = onSurfaceColor,
                        start = Offset(x, 0f),
                        end = Offset(x, tickHeight),
                        strokeWidth = if (isMajor) 2f else 1f,
                    )
                    if (isMajor) {
                        val label = "${sixteenths / 16}"
                        val textLayout = textMeasurer.measure(
                            label,
                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                        )
                        drawText(
                            textMeasurer = textMeasurer,
                            text = label,
                            topLeft = Offset(x - textLayout.size.width / 2f, tickHeight + 4f),
                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                        )
                    }
                }
            }
        }

        // Fixed red pointer at center
        drawLine(
            color = Color.Red,
            start = Offset(width / 2f, 0f),
            end = Offset(width / 2f, height),
            strokeWidth = 2f,
        )
    }
}

