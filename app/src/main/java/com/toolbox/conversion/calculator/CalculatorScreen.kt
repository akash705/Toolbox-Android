package com.toolbox.conversion.calculator

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        // Display
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = state.expression.ifEmpty { " " },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = state.result.ifEmpty { " " },
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // DEG / RAD toggle
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = state.useDegrees,
                onClick = { if (!state.useDegrees) viewModel.toggleAngleMode() },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
            ) {
                Text("DEG")
            }
            SegmentedButton(
                selected = !state.useDegrees,
                onClick = { if (state.useDegrees) viewModel.toggleAngleMode() },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
            ) {
                Text("RAD")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Button grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val buttonModifier = Modifier.weight(1f)
            val onClick = { token: String ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.onInput(token)
            }

            // Row 1: sin cos tan ( ) C
            Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SciButton("sin", buttonModifier) { onClick("sin(") }
                SciButton("cos", buttonModifier) { onClick("cos(") }
                SciButton("tan", buttonModifier) { onClick("tan(") }
                SciButton("(", buttonModifier) { onClick("(") }
                SciButton(")", buttonModifier) { onClick(")") }
                ActionButton("C", buttonModifier) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.onClear()
                }
            }

            // Row 2: ln log √ 7 8 9
            Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SciButton("ln", buttonModifier) { onClick("ln(") }
                SciButton("log", buttonModifier) { onClick("log(") }
                SciButton("√", buttonModifier) { onClick("sqrt(") }
                NumButton("7", buttonModifier) { onClick("7") }
                NumButton("8", buttonModifier) { onClick("8") }
                NumButton("9", buttonModifier) { onClick("9") }
            }

            // Row 3: x^y π e 4 5 6
            Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SciButton("x^y", buttonModifier) { onClick("^") }
                SciButton("π", buttonModifier) { onClick("π") }
                SciButton("e", buttonModifier) { onClick("e") }
                NumButton("4", buttonModifier) { onClick("4") }
                NumButton("5", buttonModifier) { onClick("5") }
                NumButton("6", buttonModifier) { onClick("6") }
            }

            // Row 4: n! abs % 1 2 3
            Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SciButton("n!", buttonModifier) { onClick("!") }
                SciButton("abs", buttonModifier) { onClick("abs(") }
                SciButton("%", buttonModifier) { onClick("%") }
                NumButton("1", buttonModifier) { onClick("1") }
                NumButton("2", buttonModifier) { onClick("2") }
                NumButton("3", buttonModifier) { onClick("3") }
            }

            // Row 5: +/- ⌫ . 0 00 =
            Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SciButton("±", buttonModifier) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.onNegate()
                }
                ActionButton("⌫", buttonModifier) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.onBackspace()
                }
                NumButton(".", buttonModifier) { onClick(".") }
                NumButton("0", buttonModifier) { onClick("0") }
                NumButton("00", buttonModifier) { onClick("00") }
                EqualsButton("=", buttonModifier) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.onEquals()
                }
            }

            // Row 6: operators
            Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OpButton("÷", buttonModifier) { onClick("÷") }
                OpButton("×", buttonModifier) { onClick("×") }
                OpButton("−", buttonModifier) { onClick("-") }
                OpButton("+", buttonModifier) { onClick("+") }
            }
        }
    }
}

@Composable
private fun NumButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Text(label, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SciButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OpButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Text(label, fontSize = 20.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ActionButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EqualsButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}
