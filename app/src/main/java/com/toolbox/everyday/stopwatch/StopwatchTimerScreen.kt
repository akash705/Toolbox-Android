package com.toolbox.everyday.stopwatch

import android.app.AlarmManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.provider.Settings
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel

private enum class TabMode { Stopwatch, Timer }

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

        Spacer(modifier = Modifier.height(16.dp))

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
        // Hero time display + buttons centered in available space
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = formatTime(state.elapsedMs),
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (state.isRunning) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Secondary button: Lap or Reset
                if (state.isRunning) {
                    OutlinedButton(
                        onClick = viewModel::lap,
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = "Lap", modifier = Modifier.size(24.dp))
                    }
                } else if (state.elapsedMs > 0) {
                    OutlinedButton(
                        onClick = viewModel::reset,
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Reset", modifier = Modifier.size(24.dp))
                    }
                }

                // Primary button: Start/Pause
                Button(
                    onClick = viewModel::startPause,
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isRunning) "Pause" else "Start",
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
        }

        // Lap list
        if (state.laps.isNotEmpty()) {
            Text(
                text = "Laps",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            )

            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(state.laps.reversed()) { index, lapTotalMs ->
                    val lapNum = state.laps.size - index
                    val prevTotalMs = if (lapNum > 1) state.laps[lapNum - 2] else 0L
                    val lapSplitMs = lapTotalMs - prevTotalMs

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Lap $lapNum",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Split",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = formatTime(lapSplitMs),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Total",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = formatTime(lapTotalMs),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
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

@Composable
private fun ExactAlarmPermissionBanner() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var canSchedule by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
            else true
        )
    }

    // Re-check when the user returns from Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                canSchedule = context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (!canSchedule) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(20.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Timer may not ring if app is closed",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Allow exact alarms so the timer fires reliably in the background.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
                TextButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                            )
                        }
                    },
                ) {
                    Text(
                        text = "Fix",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold,
                    )
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
    var pendingDurationMs by remember { mutableStateOf<Long?>(null) }

    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val service = (binder as? TimerService.LocalBinder)?.service
                timerService = service
                // If a timer start was pending while binding, fire it now
                pendingDurationMs?.let { duration ->
                    service?.startTimer(duration)
                    pendingDurationMs = null
                }
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
            try { context.unbindService(connection) } catch (_: Exception) {}
        }
    }

    val remainingMs by timerService?.remainingMs?.collectAsState() ?: remember { mutableStateOf(0L) }
    val isRunning by timerService?.isRunning?.collectAsState() ?: remember { mutableStateOf(false) }

    // Custom time input state
    var customHours by rememberSaveable { mutableStateOf("") }
    var customMinutes by rememberSaveable { mutableStateOf("") }
    var customSeconds by rememberSaveable { mutableStateOf("") }

    val presets = listOf(
        "1 min" to 60_000L,
        "3 min" to 180_000L,
        "5 min" to 300_000L,
        "10 min" to 600_000L,
        "15 min" to 900_000L,
        "30 min" to 1_800_000L,
    )

    fun startTimerWithDuration(durationMs: Long) {
        val service = timerService
        if (service != null) {
            service.startTimer(durationMs)
        } else {
            // Service not bound yet — store pending duration and start+bind
            pendingDurationMs = durationMs
            val intent = Intent(context, TimerService::class.java)
            context.startForegroundService(intent)
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ExactAlarmPermissionBanner()

        // Hero countdown display
        Text(
            text = formatTimerRemaining(remainingMs),
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = if (isRunning) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 32.dp),
        )

        if (!isRunning) {
            // Preset duration chips
            Text(
                text = "Quick start",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                presets.forEach { (label, durationMs) ->
                    SuggestionChip(
                        onClick = { startTimerWithDuration(durationMs) },
                        label = { Text(label) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Custom time input
            Text(
                text = "Custom time",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = customHours,
                            onValueChange = { customHours = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Hours") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = customMinutes,
                            onValueChange = { customMinutes = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Min") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = customSeconds,
                            onValueChange = { customSeconds = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Sec") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val totalCustomMs = ((customHours.toLongOrNull() ?: 0) * 3_600_000L) +
                            ((customMinutes.toLongOrNull() ?: 0) * 60_000L) +
                            ((customSeconds.toLongOrNull() ?: 0) * 1_000L)

                    Button(
                        onClick = {
                            startTimerWithDuration(totalCustomMs)
                            customHours = ""
                            customMinutes = ""
                            customSeconds = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = totalCustomMs > 0,
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Timer")
                    }
                }
            }
        } else {
            // Cancel button
            Button(
                onClick = { timerService?.cancelTimer() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
            ) {
                Icon(
                    Icons.Default.Stop,
                    contentDescription = "Cancel Timer",
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}
