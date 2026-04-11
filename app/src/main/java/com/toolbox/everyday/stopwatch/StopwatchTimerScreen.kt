package com.toolbox.everyday.stopwatch

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

private enum class TabMode { Stopwatch, Timer }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StopwatchTimerScreen(
    stopwatchViewModel: StopwatchViewModel = viewModel(),
) {
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            TabMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = tabIndex == index,
                    onClick = { tabIndex = index },
                    shape = SegmentedButtonDefaults.itemShape(index, TabMode.entries.size),
                ) {
                    Text(mode.name)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        when (tabIndex) {
            0 -> StopwatchTab(stopwatchViewModel)
            1 -> TimerTab()
        }
    }
}

@Composable
private fun StopwatchTab(viewModel: StopwatchViewModel) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = formatTime(state.elapsedMs),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 32.dp),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Button(onClick = viewModel::startPause) {
                Text(if (state.isRunning) "Pause" else if (state.elapsedMs > 0) "Resume" else "Start")
            }
            if (state.isRunning) {
                OutlinedButton(onClick = viewModel::lap) {
                    Text("Lap")
                }
            }
            if (!state.isRunning && state.elapsedMs > 0) {
                OutlinedButton(onClick = viewModel::reset) {
                    Text("Reset")
                }
            }
        }

        if (state.laps.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            LazyColumn {
                itemsIndexed(state.laps.reversed()) { index, lapMs ->
                    val lapNum = state.laps.size - index
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Lap $lapNum", style = MaterialTheme.typography.bodyMedium)
                        Text(formatTime(lapMs), style = MaterialTheme.typography.bodyMedium)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimerTab() {
    val context = LocalContext.current
    var timerService by remember { mutableStateOf<TimerService?>(null) }

    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                timerService = (binder as? TimerService.LocalBinder)?.service
            }
            override fun onServiceDisconnected(name: ComponentName?) {
                timerService = null
            }
        }
    }

    DisposableEffect(Unit) {
        val intent = Intent(context, TimerService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        onDispose {
            context.unbindService(connection)
        }
    }

    val remainingMs by timerService?.remainingMs?.collectAsState() ?: remember { mutableStateOf(0L) }
    val isRunning by timerService?.isRunning?.collectAsState() ?: remember { mutableStateOf(false) }

    val presets = listOf(
        "1 min" to 60_000L,
        "3 min" to 180_000L,
        "5 min" to 300_000L,
        "10 min" to 600_000L,
        "15 min" to 900_000L,
        "30 min" to 1_800_000L,
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = formatTimerRemaining(remainingMs),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp),
            textAlign = TextAlign.Center,
            color = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 32.dp),
        )

        if (!isRunning) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                presets.forEach { (label, durationMs) ->
                    SuggestionChip(
                        onClick = {
                            val intent = Intent(context, TimerService::class.java)
                            context.startForegroundService(intent)
                            // Rebind and start after short delay to ensure service is created
                            timerService?.startTimer(durationMs) ?: run {
                                context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
                            }
                            timerService?.startTimer(durationMs)
                        },
                        label = { Text(label) },
                    )
                }
            }
        } else {
            Button(onClick = {
                timerService?.cancelTimer()
            }) {
                Text("Cancel Timer")
            }
        }
    }
}
