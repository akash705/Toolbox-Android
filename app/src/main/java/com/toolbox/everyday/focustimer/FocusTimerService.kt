package com.toolbox.everyday.focustimer

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.toolbox.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Dedicated foreground service for the Focus Timer. Mirrors the design of the shipped
 * [com.toolbox.everyday.stopwatch.TimerService] (wall-clock endTime + AlarmManager backup + tick),
 * but uses its OWN notification IDs, channel, alarm receiver and state holder so it can never
 * collide with the stopwatch/countdown timer when both run at once.
 *
 * Phase state is persisted so an alarm-driven advance survives a recoverable process reap, and so
 * the UI can reconcile on next launch after an unrecoverable force-stop/reboot.
 */
class FocusTimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tickJob: Job? = null

    private var config = FocusConfig()
    private var phase = FocusPhase.Work
    private var completedWorkSessions = 0
    private var endTimeMs = 0L
    private var pausedRemainingMs = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Promote to foreground immediately for any command that keeps us alive.
        ServiceCompat.startForeground(
            this, NOTIF_ID, buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
        when (intent?.action) {
            ACTION_START -> startSession(FocusPersistence.readConfig(this, intent))
            ACTION_PAUSE -> pause()
            ACTION_RESUME -> resume()
            ACTION_SKIP -> skip()
            ACTION_ADVANCE -> advanceFromAlarm()
            ACTION_RESET, null -> if (intent?.action == ACTION_RESET) stopSession()
        }
        return START_NOT_STICKY
    }

    private fun startSession(config: FocusConfig) {
        this.config = config
        completedWorkSessions = 0
        beginPhase(FocusPhase.Work)
    }

    private fun beginPhase(newPhase: FocusPhase, resumeRemainingMs: Long? = null) {
        phase = newPhase
        val duration = resumeRemainingMs ?: config.durationMsFor(newPhase)
        endTimeMs = System.currentTimeMillis() + duration
        persist(paused = false)
        FocusTimerState.set(
            active = true, running = true, phase = phase,
            remainingMs = duration, completedWorkSessions = completedWorkSessions,
        )
        updateNotification()
        scheduleAlarm(duration)
        startTick()
    }

    private fun startTick() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (true) {
                val remaining = endTimeMs - System.currentTimeMillis()
                if (remaining <= 0) {
                    onPhaseComplete()
                    break
                }
                FocusTimerState.set(remainingMs = remaining, running = true)
                updateNotification()
                delay(1000)
            }
        }
    }

    private fun onPhaseComplete() {
        val finished = phase
        if (finished == FocusPhase.Work) completedWorkSessions++
        fireTransitionAlert(finished)
        beginPhase(nextPhase(finished, completedWorkSessions, config))
    }

    /** Alarm-driven advance after the tick may be dead (recoverable reap). Rebuild from prefs. */
    private fun advanceFromAlarm() {
        FocusPersistence.load(this)?.let {
            config = it.config
            phase = it.phase
            completedWorkSessions = it.completedWorkSessions
            endTimeMs = it.endTimeMs
        }
        if (System.currentTimeMillis() >= endTimeMs) {
            onPhaseComplete()
        } else {
            // Spurious fire; resume ticking toward the real end.
            startTick()
        }
    }

    private fun pause() {
        tickJob?.cancel()
        cancelAlarm()
        pausedRemainingMs = (endTimeMs - System.currentTimeMillis()).coerceAtLeast(0)
        persist(paused = true)
        FocusTimerState.set(running = false, remainingMs = pausedRemainingMs)
        updateNotification()
    }

    private fun resume() {
        beginPhase(phase, resumeRemainingMs = pausedRemainingMs.takeIf { it > 0 })
    }

    private fun skip() {
        tickJob?.cancel()
        cancelAlarm()
        // Skipping does not count the current work phase as completed.
        beginPhase(nextPhase(phase, completedWorkSessions, config))
    }

    private fun stopSession() {
        tickJob?.cancel()
        cancelAlarm()
        FocusPersistence.clear(this)
        FocusTimerState.clear()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // --- Alarm backup ---

    private fun scheduleAlarm(durationMs: Long) {
        val am = getSystemService(AlarmManager::class.java)
        val pi = alarmPendingIntent()
        val triggerAt = SystemClock.elapsedRealtime() + durationMs
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi)
        } else {
            // Exact alarms unavailable: approximate (Doze may delay ~9-15 min). UI warns the user.
            am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi)
        }
    }

    private fun cancelAlarm() {
        getSystemService(AlarmManager::class.java).cancel(alarmPendingIntent())
    }

    private fun alarmPendingIntent(): PendingIntent {
        val intent = Intent(this, FocusTimerAlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            this, ALARM_REQ, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    // --- Notifications ---

    private fun createChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Focus Timer", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows the running focus session"
                setShowBadge(false)
            },
        )
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        nm.createNotificationChannel(
            NotificationChannel(ALERT_CHANNEL_ID, "Focus Timer Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Chimes when a focus or break phase ends"
                setSound(
                    alarmSound,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
            },
        )
    }

    private fun buildNotification(): Notification {
        val running = FocusTimerState.running.value
        val remaining = FocusTimerState.remainingMs.value
        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("${phase.label} · ${formatMs(remaining)}")
            .setContentText(if (running) "Focus session in progress" else "Paused")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setSilent(true)

        if (running) {
            builder.addAction(0, "Pause", servicePendingIntent(ACTION_PAUSE, REQ_PAUSE))
        } else {
            builder.addAction(0, "Resume", servicePendingIntent(ACTION_RESUME, REQ_RESUME))
        }
        builder.addAction(0, "Skip", servicePendingIntent(ACTION_SKIP, REQ_SKIP))
        return builder.build()
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification())
    }

    private fun fireTransitionAlert(finished: FocusPhase) {
        val next = nextPhase(finished, completedWorkSessions, config)
        val text = "${finished.label} finished — starting ${next.label.lowercase()}."
        val n = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Focus Timer")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(ALERT_NOTIF_ID, n)
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, FocusTimerService::class.java).setAction(action)
        return PendingIntent.getService(
            this, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun persist(paused: Boolean) {
        FocusPersistence.save(
            this,
            FocusPersistence.Snapshot(
                config = config,
                phase = phase,
                completedWorkSessions = completedWorkSessions,
                endTimeMs = endTimeMs,
                paused = paused,
            ),
        )
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "focus_timer_channel"
        const val ALERT_CHANNEL_ID = "focus_timer_alert_channel"
        const val NOTIF_ID = 1004
        const val ALERT_NOTIF_ID = 1005
        const val ALARM_REQ = 8801
        private const val REQ_PAUSE = 8802
        private const val REQ_RESUME = 8803
        private const val REQ_SKIP = 8804

        const val ACTION_START = "com.toolbox.focustimer.START"
        const val ACTION_PAUSE = "com.toolbox.focustimer.PAUSE"
        const val ACTION_RESUME = "com.toolbox.focustimer.RESUME"
        const val ACTION_SKIP = "com.toolbox.focustimer.SKIP"
        const val ACTION_RESET = "com.toolbox.focustimer.RESET"
        const val ACTION_ADVANCE = "com.toolbox.focustimer.ADVANCE"
    }
}

fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
