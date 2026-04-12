package com.toolbox.conversion.bmi

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.pow
import kotlin.math.roundToInt

private val underweightColor = Color(0xFF42A5F5)
private val normalColor = Color(0xFF66BB6A)
private val overweightColor = Color(0xFFFFA726)
private val obeseColor = Color(0xFFEF5350)

@Composable
fun BmiCalculatorScreen() {
    var isMetric by rememberSaveable { mutableStateOf(true) }
    var heightCm by rememberSaveable { mutableStateOf("") }
    var weightKg by rememberSaveable { mutableStateOf("") }
    var heightFt by rememberSaveable { mutableStateOf("") }
    var heightIn by rememberSaveable { mutableStateOf("") }
    var weightLb by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Unit toggle
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = isMetric,
                onClick = { isMetric = true },
                label = { Text("Metric") },
            )
            FilterChip(
                selected = !isMetric,
                onClick = { isMetric = false },
                label = { Text("Imperial") },
            )
        }

        // Input fields
        if (isMetric) {
            OutlinedTextField(
                value = heightCm,
                onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) heightCm = it },
                label = { Text("Height (cm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = weightKg,
                onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) weightKg = it },
                label = { Text("Weight (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = heightFt,
                    onValueChange = { if (it.all { c -> c.isDigit() }) heightFt = it },
                    label = { Text("Feet") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = heightIn,
                    onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) heightIn = it },
                    label = { Text("Inches") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }
            OutlinedTextField(
                value = weightLb,
                onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) weightLb = it },
                label = { Text("Weight (lb)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        // Calculate BMI
        val bmi = calculateBmi(isMetric, heightCm, weightKg, heightFt, heightIn, weightLb)
        val heightM = getHeightInMeters(isMetric, heightCm, heightFt, heightIn)

        if (bmi != null && bmi > 0 && bmi < 100) {
            val category = bmiCategory(bmi)
            val categoryColor = bmiColor(bmi)

            // Results card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Your BMI",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "%.1f".format(bmi),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = categoryColor,
                    )
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = categoryColor,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // BMI gauge
                    BmiGauge(bmi = bmi)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Healthy weight range
                    if (heightM != null && heightM > 0) {
                        val minWeight = 18.5 * heightM.pow(2)
                        val maxWeight = 24.9 * heightM.pow(2)

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        if (isMetric) {
                            Text(
                                text = "Healthy weight: %.1f – %.1f kg".format(minWeight, maxWeight),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            val minLb = minWeight * 2.20462
                            val maxLb = maxWeight * 2.20462
                            Text(
                                text = "Healthy weight: %.0f – %.0f lb".format(minLb, maxLb),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // Disclaimer
        Text(
            text = "BMI is a screening tool, not a diagnostic measure. Consult a healthcare provider for health assessments.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BmiGauge(bmi: Double) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp),
    ) {
        val totalWidth = size.width
        val barHeight = 16.dp.toPx()
        val barY = (size.height - barHeight) / 2
        val radius = CornerRadius(barHeight / 2)

        // The gauge covers BMI 10-40
        val minBmi = 10f
        val maxBmi = 40f
        val range = maxBmi - minBmi

        // Zone boundaries as fractions of the bar
        fun bmiFraction(value: Float) = ((value - minBmi) / range).coerceIn(0f, 1f)

        val zones = listOf(
            Triple(bmiFraction(10f), bmiFraction(18.5f), underweightColor),
            Triple(bmiFraction(18.5f), bmiFraction(25f), normalColor),
            Triple(bmiFraction(25f), bmiFraction(30f), overweightColor),
            Triple(bmiFraction(30f), bmiFraction(40f), obeseColor),
        )

        // Draw zones
        zones.forEach { (startFrac, endFrac, color) ->
            drawRect(
                color = color,
                topLeft = Offset(startFrac * totalWidth, barY),
                size = Size((endFrac - startFrac) * totalWidth, barHeight),
            )
        }

        // Round the ends
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(0f, barY),
            size = Size(totalWidth, barHeight),
            cornerRadius = radius,
        )

        // Draw indicator
        val indicatorFrac = ((bmi.toFloat() - minBmi) / range).coerceIn(0f, 1f)
        val indicatorX = indicatorFrac * totalWidth
        val indicatorRadius = 10.dp.toPx()

        drawCircle(
            color = Color.White,
            radius = indicatorRadius,
            center = Offset(indicatorX, size.height / 2),
        )
        drawCircle(
            color = bmiColor(bmi),
            radius = indicatorRadius - 3.dp.toPx(),
            center = Offset(indicatorX, size.height / 2),
        )
    }

    // Labels
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Underweight", style = MaterialTheme.typography.labelSmall, color = underweightColor)
        Text("Normal", style = MaterialTheme.typography.labelSmall, color = normalColor)
        Text("Overweight", style = MaterialTheme.typography.labelSmall, color = overweightColor)
        Text("Obese", style = MaterialTheme.typography.labelSmall, color = obeseColor)
    }
}

private fun calculateBmi(
    isMetric: Boolean,
    heightCm: String,
    weightKg: String,
    heightFt: String,
    heightIn: String,
    weightLb: String,
): Double? {
    return if (isMetric) {
        val h = heightCm.toDoubleOrNull() ?: return null
        val w = weightKg.toDoubleOrNull() ?: return null
        if (h <= 0) return null
        w / (h / 100.0).pow(2)
    } else {
        val ft = heightFt.toDoubleOrNull() ?: 0.0
        val inches = heightIn.toDoubleOrNull() ?: 0.0
        val totalInches = ft * 12 + inches
        val w = weightLb.toDoubleOrNull() ?: return null
        if (totalInches <= 0) return null
        703.0 * w / totalInches.pow(2)
    }
}

private fun getHeightInMeters(
    isMetric: Boolean,
    heightCm: String,
    heightFt: String,
    heightIn: String,
): Double? {
    return if (isMetric) {
        val h = heightCm.toDoubleOrNull() ?: return null
        h / 100.0
    } else {
        val ft = heightFt.toDoubleOrNull() ?: 0.0
        val inches = heightIn.toDoubleOrNull() ?: 0.0
        val totalInches = ft * 12 + inches
        if (totalInches <= 0) return null
        totalInches * 0.0254
    }
}

private fun bmiCategory(bmi: Double): String = when {
    bmi < 18.5 -> "Underweight"
    bmi < 25.0 -> "Normal"
    bmi < 30.0 -> "Overweight"
    else -> "Obese"
}

private fun bmiColor(bmi: Double): Color = when {
    bmi < 18.5 -> underweightColor
    bmi < 25.0 -> normalColor
    bmi < 30.0 -> overweightColor
    else -> obeseColor
}
