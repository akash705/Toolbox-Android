package com.toolbox.everyday.random

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

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

    Column(modifier = Modifier.fillMaxSize()) {
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

        // Result fills remaining space, centered
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            if (state.numberResult != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "RESULT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 2.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${state.numberResult}",
                        fontSize = 96.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        lineHeight = 96.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "between ${state.min} and ${state.max}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "—",
                        fontSize = 96.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        lineHeight = 96.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap Generate to get a number",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.generateNumber()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Generate", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun CoinTab(state: RandomUiState, viewModel: RandomViewModel) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val rotation = remember { Animatable(0f) }

    // displayedResult only changes after the animation completes — decoupled from ViewModel state
    var displayedResult by remember { mutableStateOf(state.coinResult) }
    var isFlipping by remember { mutableStateOf(false) }

    val isHeads = displayedResult == true
    val isTails = displayedResult == false
    val hasResult = displayedResult != null

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Coin — circular, color reflects displayed (post-animation) result
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .rotate(rotation.value)
                        .clip(CircleShape)
                        .background(
                            when {
                                isFlipping -> MaterialTheme.colorScheme.surfaceContainerHighest
                                isHeads -> MaterialTheme.colorScheme.primaryContainer
                                isTails -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.surfaceContainerHighest
                            }
                        )
                        .border(
                            width = 4.dp,
                            color = when {
                                isFlipping -> MaterialTheme.colorScheme.outlineVariant
                                isHeads -> MaterialTheme.colorScheme.primary
                                isTails -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.outlineVariant
                            },
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(172.dp)
                            .clip(CircleShape)
                            .border(
                                width = 2.dp,
                                color = when {
                                    isFlipping -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    isHeads -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    isTails -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                },
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            // Show ? while flipping so face doesn't reveal mid-spin
                            text = when {
                                isFlipping -> "?"
                                isHeads -> "H"
                                isTails -> "T"
                                else -> "?"
                            },
                            fontSize = 80.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isFlipping -> MaterialTheme.colorScheme.onSurfaceVariant
                                isHeads -> MaterialTheme.colorScheme.onPrimaryContainer
                                isTails -> MaterialTheme.colorScheme.onSecondaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = when {
                        isFlipping -> "Flipping…"
                        isHeads -> "Heads"
                        isTails -> "Tails"
                        else -> "Tap Flip to start"
                    },
                    style = if (hasResult && !isFlipping)
                        MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
                    else
                        MaterialTheme.typography.bodyLarge,
                    color = if (hasResult && !isFlipping)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Button(
            onClick = {
                if (!isFlipping) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    // Pre-generate the result so both animation and reveal use the same value
                    val result = kotlin.random.Random.nextBoolean()
                    isFlipping = true
                    scope.launch {
                        rotation.animateTo(
                            targetValue = rotation.value + 720f,
                            animationSpec = tween(
                                durationMillis = 800,
                                easing = FastOutSlowInEasing,
                            ),
                        )
                        // Reveal result only after spin completes
                        displayedResult = result
                        viewModel.setCoinResult(result)
                        isFlipping = false
                    }
                }
            },
            enabled = !isFlipping,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                text = if (isFlipping) "Flipping…" else "Flip",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiceTab(state: RandomUiState, viewModel: RandomViewModel) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Controls section
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "SIDES",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 2.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                diceSidesOptions.forEach { sides ->
                    FilterChip(
                        selected = state.diceSides == sides,
                        onClick = { viewModel.setDiceSides(sides) },
                        label = { Text("d$sides") },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "COUNT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 2.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconButton(
                    onClick = { viewModel.setDiceCount(state.diceCount - 1) },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(),
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Less dice")
                }
                Text(
                    text = "${state.diceCount}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.Center,
                )
                IconButton(
                    onClick = { viewModel.setDiceCount(state.diceCount + 1) },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "More dice")
                }
            }
        }

        // Results area fills remaining space
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            if (state.diceResults.isNotEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        state.diceResults.forEach { value ->
                            Card(
                                modifier = Modifier.size(72.dp),
                                shape = RoundedCornerShape(12.dp),
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
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "Total",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "${state.diceResults.sum()}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "⚄",
                        fontSize = 64.sp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Tap Roll to throw the dice",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.rollDice()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Roll", style = MaterialTheme.typography.titleMedium)
        }
    }
}
