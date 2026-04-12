package com.toolbox.everyday.stopwatch

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StopwatchViewModel(application: Application) : AndroidViewModel(application) {

    private val _serviceState = MutableStateFlow(StopwatchState())
    val state: StateFlow<StopwatchState> = _serviceState.asStateFlow()

    private var service: StopwatchService? = null
    private var bound = false
    private var collectJob: Job? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as? StopwatchService.LocalBinder)?.service
            service?.let { svc ->
                collectJob = viewModelScope.launch {
                    svc.state.collect { _serviceState.value = it }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            collectJob?.cancel()
        }
    }

    init {
        bindService()
    }

    private fun bindService() {
        val context = getApplication<Application>()
        val intent = Intent(context, StopwatchService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        bound = true
    }

    fun startPause() {
        val svc = service
        if (svc != null) {
            if (_serviceState.value.isRunning) {
                svc.pauseStopwatch()
            } else {
                StopwatchService.start(getApplication())
                svc.startStopwatch()
            }
        } else {
            StopwatchService.start(getApplication())
            bindService()
        }
    }

    fun lap() {
        service?.lap()
    }

    fun reset() {
        service?.resetStopwatch()
    }

    override fun onCleared() {
        collectJob?.cancel()
        if (bound) {
            try {
                getApplication<Application>().unbindService(connection)
            } catch (_: Exception) {
            }
            bound = false
        }
        super.onCleared()
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hundredths = (ms % 1000) / 10
    return "%02d:%02d.%02d".format(minutes, seconds, hundredths)
}
