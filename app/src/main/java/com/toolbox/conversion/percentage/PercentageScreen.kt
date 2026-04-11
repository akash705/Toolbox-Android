package com.toolbox.conversion.percentage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private enum class PercentMode(val label: String) {
    PercentOf("X% of Y"),
    WhatPercent("X is ?% of Y"),
    PercentChange("% Change"),
}

@Composable
fun PercentageScreen() {
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var input1 by rememberSaveable { mutableStateOf("") }
    var input2 by rememberSaveable { mutableStateOf("") }

    val result = calculatePercentage(mode, input1, input2)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            PercentMode.entries.forEachIndexed { index, percentMode ->
                SegmentedButton(
                    selected = mode == index,
                    onClick = {
                        mode = index
                        input1 = ""
                        input2 = ""
                    },
                    shape = SegmentedButtonDefaults.itemShape(index, PercentMode.entries.size),
                ) {
                    Text(percentMode.label)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val (label1, label2) = when (mode) {
            0 -> "Percentage (%)" to "Of value"
            1 -> "Value" to "Total"
            else -> "From" to "To"
        }

        OutlinedTextField(
            value = input1,
            onValueChange = { input1 = it },
            label = { Text(label1) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = input2,
            onValueChange = { input2 = it },
            label = { Text(label2) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = result,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

private fun calculatePercentage(mode: Int, input1: String, input2: String): String {
    val a = input1.toDoubleOrNull() ?: return ""
    val b = input2.toDoubleOrNull() ?: return ""
    if (b == 0.0 && mode == 1) return "—"

    val result = when (mode) {
        0 -> a / 100.0 * b          // X% of Y
        1 -> if (b != 0.0) (a / b) * 100.0 else return "—"  // X is ?% of Y
        2 -> if (a != 0.0) ((b - a) / a) * 100.0 else return "—"  // % change
        else -> return ""
    }
    return "%.4g".format(result) + if (mode != 0) "%" else ""
}
