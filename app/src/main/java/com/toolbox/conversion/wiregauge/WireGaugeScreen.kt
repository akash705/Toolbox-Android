package com.toolbox.conversion.wiregauge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Reference table for solid-copper wire at room temperature. Ampacity values are conservative. */
private data class WireRow(
    val awg: String,
    val mm2: Double,
    val diameterMm: Double,
    val ampsChassis: Int,
    val ampsPower: Int,
)

private val WireTable = listOf(
    WireRow("0000 (4/0)", 107.2, 11.684, 380, 302),
    WireRow("000 (3/0)", 85.0, 10.404, 328, 239),
    WireRow("00 (2/0)", 67.4, 9.266, 283, 190),
    WireRow("0 (1/0)", 53.5, 8.252, 245, 150),
    WireRow("1", 42.4, 7.348, 211, 119),
    WireRow("2", 33.6, 6.544, 181, 94),
    WireRow("4", 21.2, 5.189, 135, 60),
    WireRow("6", 13.3, 4.115, 101, 37),
    WireRow("8", 8.37, 3.264, 73, 24),
    WireRow("10", 5.26, 2.588, 55, 15),
    WireRow("12", 3.31, 2.053, 41, 9),
    WireRow("14", 2.08, 1.628, 32, 6),
    WireRow("16", 1.31, 1.291, 22, 4),
    WireRow("18", 0.823, 1.024, 16, 2),
    WireRow("20", 0.518, 0.812, 11, 1),
    WireRow("22", 0.326, 0.644, 7, 1),
    WireRow("24", 0.205, 0.511, 4, 0),
    WireRow("26", 0.129, 0.405, 3, 0),
    WireRow("28", 0.0810, 0.321, 2, 0),
    WireRow("30", 0.0509, 0.255, 1, 0),
)

@Composable
fun WireGaugeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("AWG → metric reference", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Solid copper at room temperature. Ampacity is approximate — derate for bundled or insulated runs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                HeaderRow()
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                WireTable.forEachIndexed { index, row ->
                    DataRow(row)
                    if (index < WireTable.lastIndex) HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Quick reference", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("• Chassis: short runs in open air; max ampacity", style = MaterialTheme.typography.bodySmall)
                Text("• Power: long household runs; conservative ampacity", style = MaterialTheme.typography.bodySmall)
                Text("• Each AWG step = ~1.26× area, ~1.12× diameter", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun HeaderRow() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cell("AWG", weight = 1.4f, header = true)
        Cell("mm²", weight = 1.1f, header = true)
        Cell("Ø mm", weight = 1.1f, header = true)
        Cell("Chassis", weight = 1.1f, header = true)
        Cell("Power", weight = 1f, header = true)
    }
}

@Composable
private fun DataRow(row: WireRow) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cell(row.awg, weight = 1.4f)
        Cell("%.3f".format(row.mm2), weight = 1.1f)
        Cell("%.3f".format(row.diameterMm), weight = 1.1f)
        Cell("${row.ampsChassis}A", weight = 1.1f)
        Cell(if (row.ampsPower > 0) "${row.ampsPower}A" else "—", weight = 1f)
    }
}

@Composable
private fun RowScope.Cell(
    text: String,
    weight: Float,
    header: Boolean = false,
) {
    Text(
        text = text,
        style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall,
        fontWeight = if (header) FontWeight.SemiBold else FontWeight.Normal,
        fontFamily = if (header) null else FontFamily.Monospace,
        color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.weight(weight),
    )
}
