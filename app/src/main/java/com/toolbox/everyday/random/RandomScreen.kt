package com.toolbox.everyday.random

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

private val diceSidesOptions = listOf(4, 6, 8, 10, 12, 20)

@Composable
fun RandomScreen(
    viewModel: RandomViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            RandomMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = state.mode == mode,
                    onClick = { viewModel.setMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, RandomMode.entries.size),
                ) {
                    Text(mode.name)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (state.mode) {
            RandomMode.Number -> NumberTab(state, viewModel)
            RandomMode.Coin -> CoinTab(state, viewModel)
            RandomMode.Dice -> DiceTab(state, viewModel)
        }
    }
}

@Composable
private fun NumberTab(state: RandomUiState, viewModel: RandomViewModel) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.min,
                onValueChange = viewModel::setMin,
                label = { Text("Min") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.max,
                onValueChange = viewModel::setMax,
                label = { Text("Max") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Result card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Result",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.numberResult?.toString() ?: "—",
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.generateNumber()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Generate")
        }
    }
}

@Composable
private fun CoinTab(state: RandomUiState, viewModel: RandomViewModel) {
    val haptic = LocalHapticFeedback.current
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(state.coinResult) {
        if (state.coinResult != null) {
            rotation.animateTo(
                targetValue = rotation.value + 720f,
                animationSpec = tween(600),
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier
                .size(140.dp)
                .rotate(rotation.value),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when (state.coinResult) {
                        true -> "H"
                        false -> "T"
                        null -> "?"
                    },
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = when (state.coinResult) {
                true -> "Heads"
                false -> "Tails"
                null -> "Tap Flip to start"
            },
            style = MaterialTheme.typography.headlineSmall,
            color = if (state.coinResult == null) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.flipCoin()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Flip")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiceTab(state: RandomUiState, viewModel: RandomViewModel) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Dice sides selector
        Text(
            text = "Sides",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            diceSidesOptions.forEach { sides ->
                FilterChip(
                    selected = state.diceSides == sides,
                    onClick = { viewModel.setDiceSides(sides) },
                    label = { Text("d$sides") },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dice count selector
        Text(
            text = "Count",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconButton(onClick = { viewModel.setDiceCount(state.diceCount - 1) }) {
                Icon(Icons.Default.Remove, contentDescription = "Less dice")
            }
            Text("${state.diceCount}", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = { viewModel.setDiceCount(state.diceCount + 1) }) {
                Icon(Icons.Default.Add, contentDescription = "More dice")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Dice results
        if (state.diceResults.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.diceResults.forEach { value ->
                    Card(
                        modifier = Modifier.size(64.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "$value",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }

            if (state.diceResults.size > 1) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Total: ${state.diceResults.sum()}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Text(
                    text = "Tap Roll to start",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.rollDice()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Roll")
        }
    }
}
