package com.toolbox.conversion.paint

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.roundToInt

private enum class PaintUnit(val label: String) {
    METRIC("Metric (m, L)"),
    IMPERIAL("Imperial (ft, gal)"),
}

@Composable
fun PaintCalculatorScreen() {
    var unit by rememberSaveable { mutableStateOf(PaintUnit.METRIC) }
    var lengthStr by rememberSaveable { mutableStateOf("") }
    var widthStr by rememberSaveable { mutableStateOf("") }
    var heightStr by rememberSaveable { mutableStateOf("") }
    var doors by rememberSaveable { mutableStateOf("1") }
    var windows by rememberSaveable { mutableStateOf("1") }
    var coats by rememberSaveable { mutableFloatStateOf(2f) }
    var includeCeiling by rememberSaveable { mutableStateOf(false) }

    val length = lengthStr.toDoubleOrNull() ?: 0.0
    val width = widthStr.toDoubleOrNull() ?: 0.0
    val height = heightStr.toDoubleOrNull() ?: 0.0
    val doorCount = doors.toIntOrNull() ?: 0
    val windowCount = windows.toIntOrNull() ?: 0

    // Standard subtractions: 1.6 m² per door, 1.4 m² per window. Imperial: 17 ft² door, 15 ft² window.
    val perimeter = 2 * (length + width)
    val wallArea = perimeter * height
    val ceilingArea = if (includeCeiling) length * width else 0.0
    val doorSubtract = doorCount * (if (unit == PaintUnit.METRIC) 1.6 else 17.0)
    val windowSubtract = windowCount * (if (unit == PaintUnit.METRIC) 1.4 else 15.0)
    val paintableArea = (wallArea + ceilingArea - doorSubtract - windowSubtract).coerceAtLeast(0.0)

    // Coverage rates: 10 m²/L (typical interior emulsion) or 350 ft²/gallon
    val coveragePerUnit = if (unit == PaintUnit.METRIC) 10.0 else 350.0
    val totalAreaToCover = paintableArea * coats
    val volumeNeeded = if (paintableArea > 0) totalAreaToCover / coveragePerUnit else 0.0
    val volumeRoundedUp = ceil(volumeNeeded * 10) / 10  // nearest 0.1

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
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Units", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaintUnit.entries.forEach { u ->
                        FilterChip(
                            selected = unit == u,
                            onClick = { unit = u },
                            label = { Text(u.label) },
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                val lengthLabel = "Length (${if (unit == PaintUnit.METRIC) "m" else "ft"})"
                val widthLabel = "Width (${if (unit == PaintUnit.METRIC) "m" else "ft"})"
                val heightLabel = "Wall height (${if (unit == PaintUnit.METRIC) "m" else "ft"})"

                OutlinedTextField(
                    value = lengthStr, onValueChange = { lengthStr = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(lengthLabel) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = widthStr, onValueChange = { widthStr = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(widthLabel) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = heightStr, onValueChange = { heightStr = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(heightLabel) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = doors, onValueChange = { doors = it.filter { c -> c.isDigit() } },
                        label = { Text("Doors") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = windows, onValueChange = { windows = it.filter { c -> c.isDigit() } },
                        label = { Text("Windows") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = includeCeiling,
                        onClick = { includeCeiling = !includeCeiling },
                        label = { Text("Include ceiling") },
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text("Coats: ${coats.roundToInt()}", style = MaterialTheme.typography.bodyMedium)
                Slider(value = coats, onValueChange = { coats = it }, valueRange = 1f..3f, steps = 1)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "You'll need",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (paintableArea > 0) "%.1f %s".format(volumeRoundedUp, if (unit == PaintUnit.METRIC) "L" else "gal")
                    else "—",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                ResultLine("Wall area", wallArea, unit)
                if (includeCeiling) ResultLine("Ceiling", ceilingArea, unit)
                ResultLine("Doors removed", -doorSubtract, unit)
                ResultLine("Windows removed", -windowSubtract, unit)
                ResultLine("Paintable", paintableArea, unit, bold = true)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Coverage: ${if (unit == PaintUnit.METRIC) "10 m²/L" else "350 ft²/gal"} typical interior emulsion. Add ~10% for textured walls.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ResultLine(label: String, value: Double, unit: PaintUnit, bold: Boolean = false) {
    val unitLabel = if (unit == PaintUnit.METRIC) "m²" else "ft²"
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal)
        Text("%.1f %s".format(value, unitLabel), style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal)
    }
}
