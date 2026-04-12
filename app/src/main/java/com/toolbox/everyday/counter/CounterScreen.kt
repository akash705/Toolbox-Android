package com.toolbox.everyday.counter

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.toolbox.core.sharing.ShareButton

@Composable
fun CounterScreen(
    viewModel: CounterViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val haptic = LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Counter tabs as chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(state.counters) { index, counter ->
                FilterChip(
                    selected = state.activeIndex == index,
                    onClick = {
                        if (state.activeIndex == index) {
                            viewModel.showEditDialog(index)
                        } else {
                            viewModel.selectCounter(index)
                        }
                    },
                    label = { Text(counter.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
            item {
                FilterChip(
                    selected = false,
                    onClick = { viewModel.showAddDialog() },
                    label = { Text("+ New") },
                )
            }
        }

        // Large tap target for increment
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.increment()
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${state.activeCounter.value}",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 96.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "CURRENT COUNT",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                )
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Minus button — smaller, outlined style
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.decrement()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Decrement",
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Plus button — larger, filled primary
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.increment()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Increment",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reset button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { viewModel.showResetDialog() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Reset",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            ShareButton(
                toolName = "Counter",
                value = "${state.activeCounter.value}",
                unit = "",
                label = state.activeCounter.name,
                modifier = Modifier.height(40.dp),
            )
        }
    }

    // Reset confirmation dialog
    if (state.showResetDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissResetDialog,
            title = { Text("Reset counter?") },
            text = { Text("This will set ${state.activeCounter.name} back to 0.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmReset) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissResetDialog) { Text("Cancel") }
            },
        )
    }

    // Edit counter dialog
    if (state.editingIndex >= 0) {
        val editingCounter = state.counters[state.editingIndex]
        var editName by remember(state.editingIndex) { mutableStateOf(editingCounter.name) }
        AlertDialog(
            onDismissRequest = viewModel::dismissEditDialog,
            title = { Text("Edit counter") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Name") },
                        singleLine = true,
                    )
                    if (state.counters.size > 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = { viewModel.deleteCounter(state.editingIndex) },
                        ) {
                            Text(
                                "Delete counter",
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.renameCounter(state.editingIndex, editName.ifBlank { editingCounter.name }) },
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissEditDialog) { Text("Cancel") }
            },
        )
    }

    // Add counter dialog
    if (state.showAddDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = viewModel::dismissAddDialog,
            title = { Text("New counter") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.addCounter(name.ifBlank { "Counter ${state.counters.size + 1}" }) },
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissAddDialog) { Text("Cancel") }
            },
        )
    }
}
