package com.toolbox.everyday.stopwatch

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Binder
import android.os.IBinder
import android.os.SystemClock
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
import kotlinx.coroutines.launch

class TimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tickJob: Job? = null

    private val _remainingMs = MutableStateFlow(0L)
    val remainingMs: StateFlow<Long> = _remainingMs.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var endTimeMs: Long = 0L

    inner class LocalBinder : Binder() {
        val service: TimerService get() = this@TimerService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Immediately promote to foreground so Android doesn't kill us
        val notification = buildNotification(_remainingMs.value)
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        return START_NOT_STICKY
    }

    fun startTimer(durationMs: Long) {
        endTimeMs = System.currentTimeMillis() + durationMs
        _remainingMs.value = durationMs
        _isRunning.value = true
        ActiveTimerState.setTimerRunning(true)

        // Schedule AlarmManager as backup for guaranteed completion
        scheduleAlarm(durationMs)

        // Update foreground notification with actual duration
        val notification = buildNotification(durationMs)
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)

        // Tick loop for live UI updates
        tickJob?.cancel()
        tickJob = scope.launch {
            while (true) {
                val remaining = endTimeMs - System.currentTimeMillis()
                if (remaining <= 0) {
                    _remainingMs.value = 0
                    _isRunning.value = false
                    onTimerComplete()
                    break
                }
                _remainingMs.value = remaining
                updateNotification(remaining)
                delay(1000)
            }
        }
    }

    fun cancelTimer() {
        tickJob?.cancel()
        cancelAlarm()
        _remainingMs.value = 0
        _isRunning.value = false
        ActiveTimerState.setTimerRunning(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun onTimerComplete() {
        cancelAlarm()
        ActiveTimerState.setTimerRunning(false)
        val nm = getSystemService(NotificationManager::class.java)
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notification = NotificationCompat.Builder(this, COMPLETION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Timer complete!")
            .setContentText("Your timer has finished.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(alarmSound)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .setAutoCancel(true)
            .build()
        nm.notify(COMPLETION_NOTIFICATION_ID, notification)

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun scheduleAlarm(durationMs: Long) {
        val am = getSystemService(AlarmManager::class.java)
        if (!am.canScheduleExactAlarms()) return
        val intent = Intent(this, TimerAlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        am.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + durationMs,
            pi,
        )
    }

    private fun cancelAlarm() {
        val am = getSystemService(AlarmManager::class.java)
        val intent = Intent(this, TimerAlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        am.cancel(pi)
    }

    private fun createNotificationChannel() {
        val nm = getSystemService(NotificationManager::class.java)

        val countdownChannel = NotificationChannel(
            CHANNEL_ID,
            "Timer",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows timer countdown"
            setShowBadge(true)
        }
        nm.createNotificationChannel(countdownChannel)

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val completionChannel = NotificationChannel(
            COMPLETION_CHANNEL_ID,
            "Timer Alarm",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Alarm when timer finishes"
            setSound(
                alarmSound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
        }
        nm.createNotificationChannel(completionChannel)
    }

    private fun buildNotification(remainingMs: Long): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Timer running")
            .setContentText(formatTimerRemaining(remainingMs))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setSilent(true)
            .setNumber(1)
            .build()
    }

    private fun updateNotification(remainingMs: Long) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(remainingMs))
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "timer_channel"
        const val COMPLETION_CHANNEL_ID = "timer_alarm_channel"
        const val NOTIFICATION_ID = 1001
        const val COMPLETION_NOTIFICATION_ID = 1002
    }
}

fun formatTimerRemaining(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}
