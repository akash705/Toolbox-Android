package com.toolbox.conversion.screwbolt

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class FastenerRow(
    val designation: String,
    val majorDiameterMm: Double,
    val pitchMm: Double,
    val tapDrillMm: Double,
    val clearanceMm: Double,
    val hexAcrossFlatsMm: Double,
)

/** ISO metric coarse thread (M-series) common reference. */
private val MetricTable = listOf(
    FastenerRow("M2", 2.0, 0.4, 1.6, 2.4, 4.0),
    FastenerRow("M2.5", 2.5, 0.45, 2.05, 2.9, 5.0),
    FastenerRow("M3", 3.0, 0.5, 2.5, 3.4, 5.5),
    FastenerRow("M4", 4.0, 0.7, 3.3, 4.5, 7.0),
    FastenerRow("M5", 5.0, 0.8, 4.2, 5.5, 8.0),
    FastenerRow("M6", 6.0, 1.0, 5.0, 6.6, 10.0),
    FastenerRow("M8", 8.0, 1.25, 6.8, 9.0, 13.0),
    FastenerRow("M10", 10.0, 1.5, 8.5, 11.0, 16.0),
    FastenerRow("M12", 12.0, 1.75, 10.2, 13.5, 18.0),
    FastenerRow("M14", 14.0, 2.0, 12.0, 15.5, 21.0),
    FastenerRow("M16", 16.0, 2.0, 14.0, 17.5, 24.0),
    FastenerRow("M20", 20.0, 2.5, 17.5, 22.0, 30.0),
)

/** Unified imperial coarse thread (UNC) common reference. */
private val ImperialTable = listOf(
    FastenerRow("#4-40", 0.112 * 25.4, 25.4 / 40, 0.0890 * 25.4, 0.116 * 25.4, 0.250 * 25.4),
    FastenerRow("#6-32", 0.138 * 25.4, 25.4 / 32, 0.1065 * 25.4, 0.144 * 25.4, 0.3125 * 25.4),
    FastenerRow("#8-32", 0.164 * 25.4, 25.4 / 32, 0.1360 * 25.4, 0.170 * 25.4, 0.3438 * 25.4),
    FastenerRow("#10-24", 0.190 * 25.4, 25.4 / 24, 0.1495 * 25.4, 0.196 * 25.4, 0.375 * 25.4),
    FastenerRow("1/4\"-20", 0.250 * 25.4, 25.4 / 20, 0.201 * 25.4, 0.266 * 25.4, 0.4375 * 25.4),
    FastenerRow("5/16\"-18", 0.3125 * 25.4, 25.4 / 18, 0.257 * 25.4, 0.328 * 25.4, 0.500 * 25.4),
    FastenerRow("3/8\"-16", 0.375 * 25.4, 25.4 / 16, 0.3125 * 25.4, 0.391 * 25.4, 0.5625 * 25.4),
    FastenerRow("1/2\"-13", 0.500 * 25.4, 25.4 / 13, 0.422 * 25.4, 0.516 * 25.4, 0.750 * 25.4),
)

@Composable
fun ScrewBoltScreen() {
    var metric by rememberSaveable { mutableStateOf(true) }
    val table = if (metric) MetricTable else ImperialTable

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = metric,
                onClick = { metric = true },
                label = { Text("Metric (M)") },
            )
            FilterChip(
                selected = !metric,
                onClick = { metric = false },
                label = { Text("Imperial (UNC)") },
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Cell("Size", weight = 1.4f, header = true)
                    Cell("Ø mm", weight = 1f, header = true)
                    Cell("Pitch", weight = 1f, header = true)
                    Cell("Tap", weight = 1f, header = true)
                    Cell("Clear.", weight = 1f, header = true)
                    Cell("Hex", weight = 0.9f, header = true)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                table.forEachIndexed { index, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Cell(row.designation, weight = 1.4f)
                        Cell("%.2f".format(row.majorDiameterMm), weight = 1f)
                        Cell("%.2f".format(row.pitchMm), weight = 1f)
                        Cell("%.2f".format(row.tapDrillMm), weight = 1f)
                        Cell("%.2f".format(row.clearanceMm), weight = 1f)
                        Cell("%.1f".format(row.hexAcrossFlatsMm), weight = 0.9f)
                    }
                    if (index < table.lastIndex) HorizontalDivider(
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
                Text("Column meanings", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("• Ø — major (outer) thread diameter", style = MaterialTheme.typography.bodySmall)
                Text("• Pitch — distance between adjacent threads", style = MaterialTheme.typography.bodySmall)
                Text("• Tap — drill bit diameter for cutting threads", style = MaterialTheme.typography.bodySmall)
                Text("• Clear. — drill bit diameter for clearance hole", style = MaterialTheme.typography.bodySmall)
                Text("• Hex — wrench size across flats for the bolt head", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    "All values are nominal coarse-thread reference. Imperial entries use UNC. For fine threads (UNF, M-fine), values differ — consult your fastener spec.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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
