package com.toolbox.everyday.stopwatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StopwatchState(
    val elapsedMs: Long = 0L,
    val isRunning: Boolean = false,
    val laps: List<Long> = emptyList(),
)

class StopwatchViewModel : ViewModel() {

    private val _state = MutableStateFlow(StopwatchState())
    val state: StateFlow<StopwatchState> = _state.asStateFlow()

    private var tickJob: Job? = null
    private var startTimeNanos: Long = 0L
    private var accumulatedMs: Long = 0L

    fun startPause() {
        if (_state.value.isRunning) {
            pause()
        } else {
            start()
        }
    }

    private fun start() {
        startTimeNanos = System.nanoTime()
        _state.update { it.copy(isRunning = true) }
        tickJob = viewModelScope.launch {
            while (true) {
                delay(16) // ~60fps
                val now = System.nanoTime()
                val elapsed = accumulatedMs + (now - startTimeNanos) / 1_000_000
                _state.update { it.copy(elapsedMs = elapsed) }
            }
        }
    }

    private fun pause() {
        tickJob?.cancel()
        val now = System.nanoTime()
        accumulatedMs += (now - startTimeNanos) / 1_000_000
        _state.update { it.copy(isRunning = false, elapsedMs = accumulatedMs) }
    }

    fun lap() {
        _state.update { it.copy(laps = it.laps + it.elapsedMs) }
    }

    fun reset() {
        tickJob?.cancel()
        accumulatedMs = 0L
        _state.value = StopwatchState()
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hundredths = (ms % 1000) / 10
    return "%02d:%02d.%02d".format(minutes, seconds, hundredths)
}
