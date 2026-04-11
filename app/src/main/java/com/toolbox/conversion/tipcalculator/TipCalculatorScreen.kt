package com.toolbox.conversion.tipcalculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private val tipPresets = listOf(10, 15, 18, 20, 25)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TipCalculatorScreen() {
    var billAmount by rememberSaveable { mutableStateOf("") }
    var selectedTipPercent by rememberSaveable { mutableIntStateOf(15) }
    var splitCount by rememberSaveable { mutableIntStateOf(1) }

    val bill = billAmount.toDoubleOrNull() ?: 0.0
    val tipAmount = bill * selectedTipPercent / 100.0
    val total = bill + tipAmount
    val perPerson = if (splitCount > 0) total / splitCount else total

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        OutlinedTextField(
            value = billAmount,
            onValueChange = { billAmount = it },
            label = { Text("Bill amount") },
            prefix = { Text("$") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Tip percentage", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tipPresets.forEach { percent ->
                FilterChip(
                    selected = selectedTipPercent == percent,
                    onClick = { selectedTipPercent = percent },
                    label = { Text("$percent%") },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Split control
        Text("Split between", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconButton(onClick = { if (splitCount > 1) splitCount-- }) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease")
            }
            Text(
                text = "$splitCount",
                style = MaterialTheme.typography.headlineSmall,
            )
            IconButton(onClick = { splitCount++ }) {
                Icon(Icons.Default.Add, contentDescription = "Increase")
            }
            Text(
                text = if (splitCount == 1) "person" else "people",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Results card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ResultRow("Tip amount", "$%.2f".format(tipAmount))
                Spacer(modifier = Modifier.height(4.dp))
                ResultRow("Total", "$%.2f".format(total))
                if (splitCount > 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                    ResultRow("Per person", "$%.2f".format(perPerson))
                }
            }
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
