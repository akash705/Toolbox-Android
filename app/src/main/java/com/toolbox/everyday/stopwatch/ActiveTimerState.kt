package com.toolbox.everyday.stopwatch

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global observable state so the dashboard can show an activity indicator
 * on the Stopwatch tile without binding to the services.
 */
object ActiveTimerState {
    private val _stopwatchRunning = MutableStateFlow(false)
    val stopwatchRunning: StateFlow<Boolean> = _stopwatchRunning.asStateFlow()

    private val _timerRunning = MutableStateFlow(false)
    val timerRunning: StateFlow<Boolean> = _timerRunning.asStateFlow()

    val anyActive: StateFlow<Boolean> get() = _anyActive
    private val _anyActive = MutableStateFlow(false)

    fun setStopwatchRunning(running: Boolean) {
        _stopwatchRunning.value = running
        _anyActive.value = _stopwatchRunning.value || _timerRunning.value
    }

    fun setTimerRunning(running: Boolean) {
        _timerRunning.value = running
        _anyActive.value = _stopwatchRunning.value || _timerRunning.value
    }
}
