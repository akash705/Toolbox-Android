package com.toolbox.conversion.unitcircle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan
import kotlin.math.PI

private data class CommonAngle(val label: String, val degrees: Double)

private val CommonAngles = listOf(
    CommonAngle("0", 0.0),
    CommonAngle("π/6", 30.0),
    CommonAngle("π/4", 45.0),
    CommonAngle("π/3", 60.0),
    CommonAngle("π/2", 90.0),
    CommonAngle("2π/3", 120.0),
    CommonAngle("3π/4", 135.0),
    CommonAngle("5π/6", 150.0),
    CommonAngle("π", 180.0),
    CommonAngle("7π/6", 210.0),
    CommonAngle("5π/4", 225.0),
    CommonAngle("4π/3", 240.0),
    CommonAngle("3π/2", 270.0),
    CommonAngle("5π/3", 300.0),
    CommonAngle("7π/4", 315.0),
    CommonAngle("11π/6", 330.0),
)

@Composable
fun UnitCircleScreen() {
    var angleDeg by rememberSaveable { mutableFloatStateOf(45f) }
    val angleRad = angleDeg * PI / 180.0
    val sinV = sin(angleRad)
    val cosV = cos(angleRad)
    val tanV = if (kotlin.math.abs(cosV) < 1e-10) Double.NaN else tan(angleRad)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(16.dp),
            ) {
                CircleCanvas(
                    angleDeg = angleDeg.toDouble(),
                    onAngleChange = { angleDeg = it.toFloat() },
                )
            }
        }

        ValuesCard(angleDeg = angleDeg.toDouble(), sinV = sinV, cosV = cosV, tanV = tanV)
        CommonAnglesRow(currentDeg = angleDeg.toDouble(), onPick = { angleDeg = it.toFloat() })
        ReferenceCard()
    }
}

@Composable
private fun CircleCanvas(
    angleDeg: Double,
    onAngleChange: (Double) -> Unit,
) {
    val sinColor = Color(0xFFE53935)
    val cosColor = Color(0xFF1E88E5)
    val tanColor = Color(0xFF43A047)
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val outline = MaterialTheme.colorScheme.outline
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val primary = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val dx = change.position.x - center.x
                    val dy = change.position.y - center.y
                    // Screen y grows downward; flip for math angle.
                    val newDeg = (Math.toDegrees(atan2(-dy.toDouble(), dx.toDouble())) + 360) % 360
                    onAngleChange(newDeg)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val dx = offset.x - center.x
                    val dy = offset.y - center.y
                    val newDeg = (Math.toDegrees(atan2(-dy.toDouble(), dx.toDouble())) + 360) % 360
                    onAngleChange(newDeg)
                }
            },
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = (kotlin.math.min(size.width, size.height) / 2f) * 0.85f

        // Grid lines at 0.5 increments
        val dashed = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
        for (i in -1..1) {
            if (i == 0) continue
            val v = i * 0.5f
            drawLine(
                gridColor,
                Offset(cx - radius, cy - v * radius),
                Offset(cx + radius, cy - v * radius),
                strokeWidth = 1f,
                pathEffect = dashed,
            )
            drawLine(
                gridColor,
                Offset(cx + v * radius, cy - radius),
                Offset(cx + v * radius, cy + radius),
                strokeWidth = 1f,
                pathEffect = dashed,
            )
        }

        // Axes
        drawLine(axisColor, Offset(cx - radius * 1.1f, cy), Offset(cx + radius * 1.1f, cy), strokeWidth = 2f)
        drawLine(axisColor, Offset(cx, cy - radius * 1.1f), Offset(cx, cy + radius * 1.1f), strokeWidth = 2f)

        // Unit circle
        drawCircle(outline, radius = radius, center = Offset(cx, cy), style = Stroke(width = 3f))

        // Angle line and projections
        val rad = angleDeg * PI / 180.0
        val px = cx + (cos(rad) * radius).toFloat()
        val py = cy - (sin(rad) * radius).toFloat()

        // Cosine projection (horizontal blue line on x-axis)
        drawLine(
            cosColor,
            Offset(cx, cy),
            Offset(px, cy),
            strokeWidth = 6f,
        )
        // Sine projection (vertical red line from x-axis up to point)
        drawLine(
            sinColor,
            Offset(px, cy),
            Offset(px, py),
            strokeWidth = 6f,
        )
        // Hypotenuse (radius to point)
        drawLine(
            onSurface,
            Offset(cx, cy),
            Offset(px, py),
            strokeWidth = 3f,
        )

        // Tangent indicator: short tick where line through origin extended hits the vertical x=1 axis.
        // (Visual cue only — useful for understanding tan = sin/cos visually.)
        val cosAbs = kotlin.math.abs(cos(rad))
        if (cosAbs > 0.01) {
            val tanY = (sin(rad) / cos(rad))
            val tanScreenY = cy - (tanY * radius).toFloat()
            val xTangentLine = cx + radius
            // Only draw the tan tick when it stays in a reasonable visual range
            if (kotlin.math.abs(tanY) < 4) {
                drawLine(
                    tanColor.copy(alpha = 0.6f),
                    Offset(xTangentLine, cy),
                    Offset(xTangentLine, tanScreenY),
                    strokeWidth = 4f,
                    pathEffect = dashed,
                )
            }
        }

        // Point on circle (drag handle)
        drawCircle(primary, radius = 18f, center = Offset(px, py))
        drawCircle(Color.White, radius = 8f, center = Offset(px, py))

        // Axis labels
        val nativePaint = android.graphics.Paint().apply {
            color = onSurface.toArgb()
            textSize = 28f
            isAntiAlias = true
        }
        drawContext.canvas.nativeCanvas.apply {
            drawText("1", cx + radius + 8f, cy + 10f, nativePaint)
            drawText("-1", cx - radius - 30f, cy + 10f, nativePaint)
            drawText("1", cx + 8f, cy - radius - 4f, nativePaint)
            drawText("-1", cx + 8f, cy + radius + 28f, nativePaint)
        }
    }
}

private fun Color.toArgb(): Int =
    android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt(),
    )

@Composable
private fun ValuesCard(angleDeg: Double, sinV: Double, cosV: Double, tanV: Double) {
    val radFraction = simplifyToPi(angleDeg)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ValueRow(
                "Angle",
                "%.2f°".format(angleDeg),
                radFraction ?: "%.4f rad".format(angleDeg * PI / 180.0),
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ValueRow("sin θ", "%.6f".format(sinV), color = Color(0xFFE53935))
            ValueRow("cos θ", "%.6f".format(cosV), color = Color(0xFF1E88E5))
            ValueRow(
                "tan θ",
                if (tanV.isNaN() || kotlin.math.abs(tanV) > 1e6) "undefined" else "%.6f".format(tanV),
                color = Color(0xFF43A047),
            )
        }
    }
}

@Composable
private fun ValueRow(label: String, primary: String, secondary: String? = null, color: Color? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = color ?: MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                primary,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                fontWeight = FontWeight.SemiBold,
            )
            if (secondary != null) {
                Text(
                    secondary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CommonAnglesRow(currentDeg: Double, onPick: (Double) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Common angles",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CommonAngles.forEach { a ->
                    val selected = kotlin.math.abs(((currentDeg % 360 + 360) % 360) - a.degrees) < 0.5
                    FilterChip(
                        selected = selected,
                        onClick = { onPick(a.degrees) },
                        label = { Text("${a.label} (${a.degrees.toInt()}°)") },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReferenceCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("How to read the diagram", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text("• The blue segment is cos θ — the x-coordinate of the point on the unit circle.", style = MaterialTheme.typography.bodySmall)
            Text("• The red segment is sin θ — the y-coordinate.", style = MaterialTheme.typography.bodySmall)
            Text("• The green dashed tick at x=1 shows tan θ visually (vertical distance).", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            Text("Drag the dot or tap anywhere on the diagram to change the angle.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Returns a π-fraction string like "π/4", "2π/3" if the angle matches a common multiple, else null. */
private fun simplifyToPi(degrees: Double): String? {
    val tolerance = 0.001
    val match = CommonAngles.firstOrNull { kotlin.math.abs(((degrees % 360 + 360) % 360) - it.degrees) < tolerance }
    return match?.let { "${it.label} rad" }
}
