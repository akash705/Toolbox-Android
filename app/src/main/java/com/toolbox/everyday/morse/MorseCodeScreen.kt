package com.toolbox.everyday.morse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MorseCodeScreen(
    viewModel: MorseCodeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // Tab selector
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = state.tab == MorseTab.TextToMorse,
                onClick = { viewModel.setTab(MorseTab.TextToMorse) },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
            ) {
                Text("Text to Morse")
            }
            SegmentedButton(
                selected = state.tab == MorseTab.MorseToText,
                onClick = { viewModel.setTab(MorseTab.MorseToText) },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
            ) {
                Text("Morse to Text")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (state.tab) {
            MorseTab.TextToMorse -> TextToMorseTab(state, viewModel, haptic, clipboardManager)
            MorseTab.MorseToText -> MorseToTextTab(state, viewModel, haptic, clipboardManager)
        }
    }
}

@Composable
private fun TextToMorseTab(
    state: MorseUiState,
    viewModel: MorseCodeViewModel,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
) {
    // Text input
    OutlinedTextField(
        value = state.textInput,
        onValueChange = viewModel::setTextInput,
        label = { Text("Enter text") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
        maxLines = 4,
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Morse output card
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Morse Code",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(state.morseOutput))
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                }
            }
            Text(
                text = state.morseOutput.ifEmpty { "..." },
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                ),
                color = if (state.morseOutput.isEmpty())
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Output mode toggles
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        FilledIconToggleButton(
            checked = state.soundEnabled,
            onCheckedChange = { viewModel.toggleSound() },
        ) {
            Icon(Icons.Default.VolumeUp, contentDescription = "Sound")
        }
        FilledIconToggleButton(
            checked = state.hapticEnabled,
            onCheckedChange = { viewModel.toggleHaptic() },
        ) {
            Icon(Icons.Default.Vibration, contentDescription = "Haptic")
        }
        FilledIconToggleButton(
            checked = state.flashEnabled,
            onCheckedChange = { viewModel.toggleFlash() },
        ) {
            Icon(Icons.Default.FlashlightOn, contentDescription = "Flashlight")
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Play button
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        FilledIconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.togglePlayback()
            },
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
        ) {
            Icon(
                imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (state.isPlaying) "Stop" else "Play",
                modifier = Modifier.size(32.dp),
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Speed slider
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Speed", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.width(12.dp))
        Slider(
            value = state.wpm.toFloat(),
            onValueChange = { viewModel.setWpm(it.toInt()) },
            valueRange = 5f..25f,
            steps = 19,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${state.wpm} WPM",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(56.dp),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun MorseToTextTab(
    state: MorseUiState,
    viewModel: MorseCodeViewModel,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
) {
    // Decoded text output
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Decoded Text",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(state.textOutput))
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                }
            }
            Text(
                text = state.textOutput.ifEmpty { "..." },
                style = MaterialTheme.typography.headlineSmall,
                color = if (state.textOutput.isEmpty())
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Morse input text field
    OutlinedTextField(
        value = state.morseInput,
        onValueChange = viewModel::setMorseInput,
        label = { Text("Morse code (dots and dashes)") },
        placeholder = { Text("Type or paste: .... . .-.. .-.. ---") },
        modifier = Modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
        ),
        minLines = 2,
        maxLines = 4,
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Morse input buttons
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Dot button
        FilledTonalButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.appendMorseSymbol(".")
            },
            modifier = Modifier
                .weight(1f)
                .height(64.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(".", fontSize = 36.sp, fontWeight = FontWeight.Bold)
        }

        // Dash button
        FilledTonalButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.appendMorseSymbol("-")
            },
            modifier = Modifier
                .weight(1f)
                .height(64.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text("—", fontSize = 36.sp, fontWeight = FontWeight.Bold)
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Letter break (space between letters)
        FilledTonalButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.appendMorseSymbol(" ")
            },
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Letter Gap", style = MaterialTheme.typography.labelMedium)
        }

        // Word break
        FilledTonalButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.appendMorseSymbol(" / ")
            },
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Word Gap", style = MaterialTheme.typography.labelMedium)
        }

        // Backspace
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.backspaceMorseInput()
            },
        ) {
            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace")
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Clear button
    FilledTonalButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.clearMorseInput()
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Clear")
    }
}
