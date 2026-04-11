package com.toolbox.conversion.tipcalculator

import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val tipPresets = listOf(10, 15, 18, 20, 25)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TipCalculatorScreen() {
    var billAmount by rememberSaveable { mutableStateOf("") }
    var selectedTipPercent by rememberSaveable { mutableIntStateOf(15) }
    var customTipText by rememberSaveable { mutableStateOf("") }
    var isCustomTip by rememberSaveable { mutableStateOf(false) }
    var splitCount by rememberSaveable { mutableIntStateOf(1) }

    val effectiveTip = if (isCustomTip) customTipText.toIntOrNull() ?: 0 else selectedTipPercent
    val bill = billAmount.toDoubleOrNull() ?: 0.0
    val tipAmount = bill * effectiveTip / 100.0
    val total = bill + tipAmount
    val perPerson = if (splitCount > 0) total / splitCount else total
    val tipPerPerson = if (splitCount > 0) tipAmount / splitCount else tipAmount

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // Bill amount input
        OutlinedTextField(
            value = billAmount,
            onValueChange = { billAmount = it },
            label = { Text("Bill amount") },
            prefix = { Text("$") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Tip percentage section
        Text("Tip percentage", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tipPresets.forEach { percent ->
                FilterChip(
                    selected = !isCustomTip && selectedTipPercent == percent,
                    onClick = {
                        isCustomTip = false
                        selectedTipPercent = percent
                    },
                    label = { Text("$percent%") },
                )
            }
            FilterChip(
                selected = isCustomTip,
                onClick = { isCustomTip = true },
                label = { Text("Custom") },
            )
        }

        if (isCustomTip) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = customTipText,
                onValueChange = { customTipText = it },
                label = { Text("Custom tip %") },
                suffix = { Text("%") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Split control
        Text("Split between", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { if (splitCount > 1) splitCount-- }) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease")
            }
            Text(
                text = "$splitCount",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            IconButton(onClick = { if (splitCount < 99) splitCount++ }) {
                Icon(Icons.Default.Add, contentDescription = "Increase")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (splitCount == 1) "person" else "people",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            Column(modifier = Modifier.padding(20.dp)) {
                // Total per person — prominent
                Text(
                    text = "Total per person",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$%.2f".format(perPerson),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(16.dp))

                // Tip per person
                ResultRow(
                    label = "Tip per person",
                    value = "$%.2f".format(tipPerPerson),
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Total bill with tip
                ResultRow(
                    label = "Total bill",
                    value = "$%.2f".format(total),
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Total tip
                ResultRow(
                    label = "Total tip",
                    value = "$%.2f".format(tipAmount),
                )
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
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
