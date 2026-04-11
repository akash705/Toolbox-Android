package com.toolbox.conversion.percentage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.floor

private enum class PercentMode(
    val label: String,
    val description: String,
) {
    PercentOf(
        label = "X% of Y",
        description = "Calculate what a percentage of a number is — e.g. what is 20% of 150?",
    ),
    WhatPercent(
        label = "What %",
        description = "Find what percentage one number is of another — e.g. 30 is what % of 200?",
    ),
    PercentChange(
        label = "% Change",
        description = "Find the percentage increase or decrease between two values — e.g. 100 → 125.",
    ),
}

@Composable
fun PercentageScreen() {
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var input1 by rememberSaveable { mutableStateOf("") }
    var input2 by rememberSaveable { mutableStateOf("") }
    var committedResult by rememberSaveable { mutableStateOf<PercentResult?>(null) }

    val focusManager = LocalFocusManager.current

    fun calculate() {
        focusManager.clearFocus()
        committedResult = calculateResult(mode, input1, input2)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // Spaced mode chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PercentMode.entries.forEachIndexed { index, percentMode ->
                FilterChip(
                    selected = mode == index,
                    onClick = {
                        mode = index
                        input1 = ""
                        input2 = ""
                        committedResult = null
                    },
                    label = { Text(percentMode.label) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Mode description
        Text(
            text = PercentMode.entries[mode].description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Input card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val (label1, placeholder1, label2, placeholder2) = when (mode) {
                    0 -> Quad("Percentage (%)", "Enter percentage, e.g. 20", "Value", "Enter value, e.g. 150")
                    1 -> Quad("Part", "The smaller amount, e.g. 30", "Whole", "The total amount, e.g. 200")
                    else -> Quad("Original Value", "Value before change, e.g. 100", "New Value", "Value after change, e.g. 125")
                }

                OutlinedTextField(
                    value = input1,
                    onValueChange = { input1 = it; committedResult = null },
                    label = { Text(label1) },
                    placeholder = { Text(placeholder1) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                    ),
                    singleLine = true,
                    trailingIcon = {
                        Icon(
                            Icons.Default.Percent,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = input2,
                    onValueChange = { input2 = it; committedResult = null },
                    label = { Text(label2) },
                    placeholder = { Text(placeholder2) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { calculate() }),
                    singleLine = true,
                    trailingIcon = {
                        Icon(
                            Icons.Default.Calculate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { calculate() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        Icons.Default.Calculate,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Calculate")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Result card — always visible
        val isPositiveChange = committedResult?.isPositive
        val resultColor = when {
            mode == 2 && isPositiveChange == true -> Color(0xFF4CAF50)
            mode == 2 && isPositiveChange == false -> Color(0xFFEF5350)
            else -> MaterialTheme.colorScheme.primary
        }
        val resultContainerColor = when {
            mode == 2 && isPositiveChange == true -> Color(0xFF4CAF50).copy(alpha = 0.12f)
            mode == 2 && isPositiveChange == false -> Color(0xFFEF5350).copy(alpha = 0.12f)
            else -> MaterialTheme.colorScheme.primaryContainer
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = resultContainerColor),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Result",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (mode == 2 && committedResult != null) {
                        Icon(
                            imageVector = when (isPositiveChange) {
                                true -> Icons.AutoMirrored.Filled.TrendingUp
                                false -> Icons.AutoMirrored.Filled.TrendingDown
                                else -> Icons.AutoMirrored.Filled.TrendingFlat
                            },
                            contentDescription = null,
                            tint = resultColor,
                        )
                    }
                }

                if (committedResult != null) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "= ${committedResult!!.value}",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = resultColor,
                    )

                    val desc = committedResult?.description
                    if (desc != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Formula",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (mode) {
                        0 -> "result = (percentage ÷ 100) × value"
                        1 -> "result = (part ÷ whole) × 100"
                        else -> "result = ((new − original) ÷ original) × 100"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private data class Quad(val a: String, val b: String, val c: String, val d: String)

private data class PercentResult(
    val value: String,
    val description: String? = null,
    val isPositive: Boolean? = null,
)

private fun calculateResult(mode: Int, input1: String, input2: String): PercentResult? {
    val a = input1.toDoubleOrNull() ?: return null
    val b = input2.toDoubleOrNull() ?: return null

    return when (mode) {
        0 -> {
            val r = a / 100.0 * b
            PercentResult(
                value = fmt(r),
                description = "${fmt(a)}% of ${fmt(b)} = ${fmt(r)}",
            )
        }
        1 -> {
            if (b == 0.0) return PercentResult("Error", "Whole cannot be zero")
            val r = (a / b) * 100.0
            PercentResult(
                value = "${fmt(r)}%",
                description = "${fmt(a)} is ${fmt(r)}% of ${fmt(b)}",
            )
        }
        2 -> {
            if (a == 0.0) return PercentResult("Error", "Original value cannot be zero")
            val r = ((b - a) / a) * 100.0
            val positive = r >= 0
            PercentResult(
                value = "${if (positive) "+" else ""}${fmt(r)}%",
                description = if (positive) "increase from ${fmt(a)} to ${fmt(b)}"
                              else "decrease from ${fmt(a)} to ${fmt(b)}",
                isPositive = positive,
            )
        }
        else -> null
    }
}

private fun fmt(value: Double): String =
    if (value == floor(value) && !value.isInfinite()) value.toLong().toString()
    else "%.4g".format(value)
