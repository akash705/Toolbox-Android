package com.toolbox.conversion.unitprice

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

@Composable
fun UnitPriceScreen() {
    var priceA by rememberSaveable { mutableStateOf("") }
    var sizeA by rememberSaveable { mutableStateOf("") }
    var priceB by rememberSaveable { mutableStateOf("") }
    var sizeB by rememberSaveable { mutableStateOf("") }

    val perA = unitPrice(priceA, sizeA)
    val perB = unitPrice(priceB, sizeB)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Compare price per unit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        itemInputs("Item A", priceA, sizeA, { priceA = it }, { sizeA = it })
        itemInputs("Item B", priceB, sizeB, { priceB = it }, { sizeB = it })

        if (perA != null || perB != null) {
            val cheaper = when {
                perA != null && perB != null -> if (perA <= perB) "A" else "B"
                perA != null -> "A"
                else -> "B"
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    perA?.let { Text("Item A: ${fmt(it)} per unit", fontWeight = if (cheaper == "A") FontWeight.Bold else FontWeight.Normal) }
                    perB?.let { Text("Item B: ${fmt(it)} per unit", fontWeight = if (cheaper == "B") FontWeight.Bold else FontWeight.Normal) }
                    if (perA != null && perB != null) {
                        Text(
                            "Item $cheaper is the better value.",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun itemInputs(
    label: String,
    price: String,
    size: String,
    onPrice: (String) -> Unit,
    onSize: (String) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = price, onValueChange = onPrice, label = { Text("Price") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = size, onValueChange = onSize, label = { Text("Size / qty") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun unitPrice(price: String, size: String): Double? {
    val p = price.toDoubleOrNull() ?: return null
    val s = size.toDoubleOrNull() ?: return null
    if (s <= 0.0) return null
    return p / s
}

private fun fmt(v: Double): String = "%.4f".format(v).trimEnd('0').trimEnd('.')
