package com.toolbox.everyday.focustimer

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FocusTimerViewModel(application: Application) : AndroidViewModel(application) {

    // Re-exposed shared state (single writer is the service).
    val active: StateFlow<Boolean> = FocusTimerState.active
    val running: StateFlow<Boolean> = FocusTimerState.running
    val phase: StateFlow<FocusPhase> = FocusTimerState.phase
    val remainingMs: StateFlow<Long> = FocusTimerState.remainingMs
    val completedWorkSessions: StateFlow<Int> = FocusTimerState.completedWorkSessions

    private val _config = MutableStateFlow(FocusConfig())
    val config: StateFlow<FocusConfig> = _config.asStateFlow()

    private val _interrupted = MutableStateFlow(false)
    val interrupted: StateFlow<Boolean> = _interrupted.asStateFlow()

    init {
        // If a session was persisted but nothing is live, it was interrupted (force-stop/reboot).
        if (!FocusTimerState.active.value && FocusPersistence.load(application) != null) {
            _interrupted.value = true
            FocusPersistence.clear(application)
        }
    }

    fun setConfig(transform: (FocusConfig) -> FocusConfig) {
        _config.value = transform(_config.value)
    }

    fun dismissInterrupted() { _interrupted.value = false }

    fun start() = send(FocusTimerService.ACTION_START) {
        val c = _config.value
        it.putExtra(FocusPersistence.EXTRA_WORK, c.workMin)
        it.putExtra(FocusPersistence.EXTRA_SHORT, c.shortBreakMin)
        it.putExtra(FocusPersistence.EXTRA_LONG, c.longBreakMin)
        it.putExtra(FocusPersistence.EXTRA_SESSIONS, c.sessionsBeforeLongBreak)
    }

    fun pause() = send(FocusTimerService.ACTION_PAUSE)
    fun resume() = send(FocusTimerService.ACTION_RESUME)
    fun skip() = send(FocusTimerService.ACTION_SKIP)
    fun reset() = send(FocusTimerService.ACTION_RESET)

    private fun send(action: String, extras: (Intent) -> Unit = {}) {
        val app = getApplication<Application>()
        val intent = Intent(app, FocusTimerService::class.java).setAction(action).also(extras)
        ContextCompat.startForegroundService(app, intent)
    }
}
