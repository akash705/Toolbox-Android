package com.toolbox.everyday.focustimer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide observable state for the Focus Timer. Dedicated to this tool — deliberately NOT
 * the shared `ActiveTimerState` used by the stopwatch/countdown timer, so the two never collide.
 * [FocusTimerService] is the only writer; the UI observes.
 */
object FocusTimerState {

    private val _active = MutableStateFlow(false)
    /** True while a Pomodoro session exists (running or paused). */
    val active: StateFlow<Boolean> = _active.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _phase = MutableStateFlow(FocusPhase.Work)
    val phase: StateFlow<FocusPhase> = _phase.asStateFlow()

    private val _remainingMs = MutableStateFlow(0L)
    val remainingMs: StateFlow<Long> = _remainingMs.asStateFlow()

    private val _completedWorkSessions = MutableStateFlow(0)
    val completedWorkSessions: StateFlow<Int> = _completedWorkSessions.asStateFlow()

    internal fun set(
        active: Boolean = _active.value,
        running: Boolean = _running.value,
        phase: FocusPhase = _phase.value,
        remainingMs: Long = _remainingMs.value,
        completedWorkSessions: Int = _completedWorkSessions.value,
    ) {
        _active.value = active
        _running.value = running
        _phase.value = phase
        _remainingMs.value = remainingMs
        _completedWorkSessions.value = completedWorkSessions
    }

    internal fun clear() {
        _active.value = false
        _running.value = false
        _remainingMs.value = 0L
    }
}
