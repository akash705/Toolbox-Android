package com.toolbox.everyday.morse

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

    Box(modifier = Modifier.fillMaxSize()) {
        // Screen flash overlay
        if (state.screenFlashOn) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
        }

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
}

@Composable
private fun TextToMorseTab(
    state: MorseUiState,
    viewModel: MorseCodeViewModel,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
) {
    // Text input with character counter
    OutlinedTextField(
        value = state.textInput,
        onValueChange = { if (it.length <= 500) viewModel.setTextInput(it) },
        placeholder = { Text("Enter text to translate...") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
        maxLines = 5,
        supportingText = {
            Text(
                text = "${state.textInput.length} / 500",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )
        },
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Morse output card
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "MORSE OUTPUT",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = state.morseOutput.ifEmpty { "..." },
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 3.sp,
                ),
                color = if (state.morseOutput.isEmpty())
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            clipboardManager.setText(AnnotatedString(state.morseOutput))
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Copy",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Speed card
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Speed",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                Text(
                    text = "${state.wpm} WPM",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = state.wpm.toFloat(),
                onValueChange = { viewModel.setWpm(it.toInt()) },
                valueRange = 5f..25f,
                steps = 19,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "5 WPM",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "25 WPM",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Output mode toggles - 4 square buttons
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutputToggle(
            icon = Icons.Default.VolumeUp,
            label = "SOUND",
            checked = state.soundEnabled,
            onToggle = { viewModel.toggleSound() },
            modifier = Modifier.weight(1f),
        )
        OutputToggle(
            icon = Icons.Default.FlashlightOn,
            label = "FLASH",
            checked = state.flashEnabled,
            onToggle = { viewModel.toggleFlash() },
            modifier = Modifier.weight(1f),
        )
        OutputToggle(
            icon = Icons.Default.Vibration,
            label = "HAPTIC",
            checked = state.hapticEnabled,
            onToggle = { viewModel.toggleHaptic() },
            modifier = Modifier.weight(1f),
        )
        OutputToggle(
            icon = Icons.Default.LightMode,
            label = "SCREEN",
            checked = state.screenFlashEnabled,
            onToggle = { viewModel.toggleScreenFlash() },
            modifier = Modifier.weight(1f),
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

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
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
        ) {
            Icon(
                imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (state.isPlaying) "Stop" else "Play",
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

@Composable
private fun OutputToggle(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary
        else Color.Transparent,
        label = "toggleBg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "toggleContent",
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant,
        label = "toggleBorder",
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable { onToggle() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = contentColor,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.Medium,
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
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "DECODED TEXT",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = state.textOutput.ifEmpty { "..." },
                style = MaterialTheme.typography.headlineSmall,
                color = if (state.textOutput.isEmpty())
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            clipboardManager.setText(AnnotatedString(state.textOutput))
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Copy",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Morse input text field
    OutlinedTextField(
        value = state.morseInput,
        onValueChange = viewModel::setMorseInput,
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
