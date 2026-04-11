package com.toolbox.everyday.counter

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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun CounterScreen(
    viewModel: CounterViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val haptic = LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Counter tabs
        if (state.counters.size > 1) {
            PrimaryScrollableTabRow(selectedTabIndex = state.activeIndex) {
                state.counters.forEachIndexed { index, counter ->
                    Tab(
                        selected = state.activeIndex == index,
                        onClick = { viewModel.selectCounter(index) },
                        text = { Text(counter.name) },
                    )
                }
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
            Text(
                text = "${state.activeCounter.value}",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 96.sp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // Bottom controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.decrement()
            }) {
                Icon(Icons.Default.Remove, contentDescription = "Decrement", modifier = Modifier.padding(8.dp))
            }

            IconButton(onClick = { viewModel.showResetDialog() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset")
            }

            IconButton(onClick = { viewModel.showAddDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Add counter")
            }
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
