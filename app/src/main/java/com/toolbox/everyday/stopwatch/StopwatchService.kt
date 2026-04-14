package com.toolbox.everyday.stopwatch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.toolbox.MainActivity
import com.toolbox.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

class StopwatchService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tickJob: Job? = null

    private val _state = MutableStateFlow(StopwatchState())
    val state: StateFlow<StopwatchState> = _state.asStateFlow()

    private var startTimeNanos: Long = 0L
    private var accumulatedMs: Long = 0L

    inner class LocalBinder : Binder() {
        val service: StopwatchService get() = this@StopwatchService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification(_state.value.elapsedMs)
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, notification,
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
        return START_NOT_STICKY
    }

    fun startStopwatch() {
        startTimeNanos = System.nanoTime()
        _state.update { it.copy(isRunning = true) }
        ActiveTimerState.setStopwatchRunning(true)

        tickJob?.cancel()
        tickJob = scope.launch {
            while (true) {
                delay(16) // ~60fps for smooth UI
                val now = System.nanoTime()
                val elapsed = accumulatedMs + (now - startTimeNanos) / 1_000_000
                _state.update { it.copy(elapsedMs = elapsed) }
                // Update notification at ~1Hz to avoid excessive work
                if (elapsed % 1000 < 20) {
                    updateNotification(elapsed)
                }
            }
        }
    }

    fun pauseStopwatch() {
        tickJob?.cancel()
        val now = System.nanoTime()
        accumulatedMs += (now - startTimeNanos) / 1_000_000
        _state.update { it.copy(isRunning = false, elapsedMs = accumulatedMs) }
        ActiveTimerState.setStopwatchRunning(false)
        updateNotification(accumulatedMs)
    }

    fun lap() {
        _state.update { it.copy(laps = it.laps + it.elapsedMs) }
    }

    fun resetStopwatch() {
        tickJob?.cancel()
        accumulatedMs = 0L
        _state.value = StopwatchState()
        ActiveTimerState.setStopwatchRunning(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Stopwatch",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows stopwatch elapsed time"
            setShowBadge(true)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(elapsedMs: Long): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle("Stopwatch running")
            .setContentText(formatTime(elapsedMs))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setSilent(true)
            .setNumber(1)
            .build()
    }

    private fun updateNotification(elapsedMs: Long) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(elapsedMs))
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "stopwatch_channel"
        const val NOTIFICATION_ID = 1003

        fun start(context: Context) {
            val intent = Intent(context, StopwatchService::class.java)
            context.startForegroundService(intent)
        }
    }
}
