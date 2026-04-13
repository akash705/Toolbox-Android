package com.toolbox.conversion.emi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.pow

private val principalColor = Color(0xFF4CAF50)
private val interestColor = Color(0xFFFF9800)

private val currencyFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}

private data class AmortizationRow(
    val month: Int,
    val principalPortion: Double,
    val interestPortion: Double,
    val balance: Double,
)

@Composable
fun EmiCalculatorScreen() {
    var loanAmountText by rememberSaveable { mutableStateOf("") }
    var interestRateText by rememberSaveable { mutableStateOf("") }
    var tenureText by rememberSaveable { mutableStateOf("") }
    var tenureInYears by rememberSaveable { mutableStateOf(true) }
    var isReducingBalance by rememberSaveable { mutableStateOf(true) }
    var showAmortization by rememberSaveable { mutableStateOf(false) }

    val principal = loanAmountText.toDoubleOrNull() ?: 0.0
    val annualRate = interestRateText.toDoubleOrNull() ?: 0.0
    val tenureValue = tenureText.toIntOrNull() ?: 0
    val totalMonths = if (tenureInYears) tenureValue * 12 else tenureValue

    val hasValidInput = principal > 0 && annualRate >= 0 && totalMonths > 0

    val emi: Double
    val totalPayment: Double
    val totalInterest: Double
    val amortizationSchedule: List<AmortizationRow>

    if (hasValidInput) {
        if (annualRate == 0.0) {
            emi = principal / totalMonths
            totalPayment = principal
            totalInterest = 0.0
            amortizationSchedule = buildList {
                var balance = principal
                for (m in 1..totalMonths) {
                    balance -= emi
                    add(AmortizationRow(m, emi, 0.0, maxOf(0.0, balance)))
                }
            }
        } else if (isReducingBalance) {
            val monthlyRate = annualRate / 100.0 / 12.0
            val factor = (1 + monthlyRate).pow(totalMonths)
            emi = principal * monthlyRate * factor / (factor - 1)
            totalPayment = emi * totalMonths
            totalInterest = totalPayment - principal
            amortizationSchedule = buildList {
                var balance = principal
                for (m in 1..totalMonths) {
                    val interestPortion = balance * monthlyRate
                    val principalPortion = emi - interestPortion
                    balance -= principalPortion
                    add(AmortizationRow(m, principalPortion, interestPortion, maxOf(0.0, balance)))
                }
            }
        } else {
            // Flat-rate
            totalInterest = principal * annualRate / 100.0 * (if (tenureInYears) tenureValue.toDouble() else tenureValue / 12.0)
            totalPayment = principal + totalInterest
            emi = totalPayment / totalMonths
            val monthlyPrincipal = principal / totalMonths
            val monthlyInterest = totalInterest / totalMonths
            amortizationSchedule = buildList {
                var balance = principal
                for (m in 1..totalMonths) {
                    balance -= monthlyPrincipal
                    add(AmortizationRow(m, monthlyPrincipal, monthlyInterest, maxOf(0.0, balance)))
                }
            }
        }
    } else {
        emi = 0.0
        totalPayment = 0.0
        totalInterest = 0.0
        amortizationSchedule = emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Method toggle
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = isReducingBalance,
                onClick = { isReducingBalance = true },
                label = { Text("Reducing Balance") },
            )
            FilterChip(
                selected = !isReducingBalance,
                onClick = { isReducingBalance = false },
                label = { Text("Flat Rate") },
            )
        }

        // Input fields
        OutlinedTextField(
            value = loanAmountText,
            onValueChange = { loanAmountText = it },
            label = { Text("Loan Amount") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )

        OutlinedTextField(
            value = interestRateText,
            onValueChange = { interestRateText = it },
            label = { Text("Annual Interest Rate (%)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = tenureText,
                onValueChange = { tenureText = it },
                label = { Text("Tenure") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            FilterChip(
                selected = tenureInYears,
                onClick = { tenureInYears = true },
                label = { Text("Years") },
            )
            FilterChip(
                selected = !tenureInYears,
                onClick = { tenureInYears = false },
                label = { Text("Months") },
            )
        }

        // Results
        if (hasValidInput) {
            // Summary card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Monthly EMI",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = currencyFormat.format(emi),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("Total Interest", style = MaterialTheme.typography.labelSmall)
                            Text(
                                currencyFormat.format(totalInterest),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = interestColor,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Payment", style = MaterialTheme.typography.labelSmall)
                            Text(
                                currencyFormat.format(totalPayment),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            // Pie chart
            if (totalPayment > 0) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Principal vs Interest",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val principalPercent = if (totalPayment > 0) (principal / totalPayment).toFloat() else 0f
                        val interestPercent = 1f - principalPercent

                        Box(contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.size(180.dp)) {
                                val strokeWidth = 36.dp.toPx()
                                val diameter = size.minDimension - strokeWidth
                                val topLeft = Offset(
                                    (size.width - diameter) / 2f,
                                    (size.height - diameter) / 2f,
                                )
                                val arcSize = Size(diameter, diameter)

                                val principalSweep = principalPercent * 360f
                                val interestSweep = interestPercent * 360f

                                drawArc(
                                    color = principalColor,
                                    startAngle = -90f,
                                    sweepAngle = principalSweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth),
                                )
                                drawArc(
                                    color = interestColor,
                                    startAngle = -90f + principalSweep,
                                    sweepAngle = interestSweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            LegendItem(principalColor, "Principal", "${(principalPercent * 100).toInt()}%")
                            LegendItem(interestColor, "Interest", "${(interestPercent * 100).toInt()}%")
                        }
                    }
                }
            }

            // Amortization schedule
            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAmortization = !showAmortization }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Amortization Schedule",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Icon(
                            if (showAmortization) Icons.Default.KeyboardArrowUp
                            else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (showAmortization) "Collapse" else "Expand",
                        )
                    }

                    AnimatedVisibility(
                        visible = showAmortization,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        Column {
                            // Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Text("#", modifier = Modifier.width(40.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Text("Principal", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                                Text("Interest", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                                Text("Balance", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                            // Use a fixed-height container with nested LazyColumn for large schedules
                            val listHeight = minOf(amortizationSchedule.size * 40, 400).dp
                            LazyColumn(
                                modifier = Modifier.height(listHeight),
                            ) {
                                itemsIndexed(amortizationSchedule) { _, row ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 6.dp),
                                    ) {
                                        Text(
                                            "${row.month}",
                                            modifier = Modifier.width(40.dp),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                        Text(
                                            currencyFormat.format(row.principalPortion),
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.End,
                                        )
                                        Text(
                                            currencyFormat.format(row.interestPortion),
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.End,
                                        )
                                        Text(
                                            currencyFormat.format(row.balance),
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.End,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(12.dp)) {
            drawCircle(color = color)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.width(4.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}
