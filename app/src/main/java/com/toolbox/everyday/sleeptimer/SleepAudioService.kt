package com.toolbox.everyday.sleeptimer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.toolbox.MainActivity
import com.toolbox.core.audio.NoiseEngine
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

/** Process-wide state for the sleep-timer ambience, observed by the screen. */
object SleepTimerState {
    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()
    private val _remainingSec = MutableStateFlow(0)
    val remainingSec: StateFlow<Int> = _remainingSec.asStateFlow()
    internal fun set(running: Boolean, remaining: Int) {
        _running.value = running
        _remainingSec.value = remaining
    }
    internal fun setRemaining(remaining: Int) { _remainingSec.value = remaining }
}

/**
 * Plays the ambient white-noise so it continues while the screen is off or the app is backgrounded,
 * backed by a [mediaPlayback] foreground service. Runs only while the user has an active sleep
 * timer, shows a persistent notification with a Stop action, and stops itself when the countdown
 * ends or the user stops it — consistent with the app's rule that a FGS is only for active audio.
 */
class SleepAudioService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tickJob: Job? = null
    private val engine = NoiseEngine()
    private var remaining = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Sleep Timer", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows while ambient sound is playing"
                setSound(null, null)
            },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopEverything()
            return START_NOT_STICKY
        }
        remaining = intent?.getIntExtra(EXTRA_DURATION_SEC, 0) ?: 0
        if (remaining <= 0) { stopSelf(); return START_NOT_STICKY }

        ServiceCompat.startForeground(
            this, NOTIF_ID, buildNotification(remaining),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
        engine.start()
        SleepTimerState.set(true, remaining)

        tickJob?.cancel()
        tickJob = scope.launch {
            while (remaining > 0) {
                delay(1000)
                remaining -= 1
                SleepTimerState.setRemaining(remaining)
                getSystemService(NotificationManager::class.java)
                    .notify(NOTIF_ID, buildNotification(remaining))
            }
            stopEverything()
        }
        return START_NOT_STICKY
    }

    private fun stopEverything() {
        tickJob?.cancel()
        engine.stop()
        SleepTimerState.set(false, 0)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(remainingSec: Int) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setContentTitle("Ambient sound playing")
            .setContentText("Stops in %02d:%02d".format(remainingSec / 60, remainingSec % 60))
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0, Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .addAction(
                0, "Stop",
                PendingIntent.getService(
                    this, 1,
                    Intent(this, SleepAudioService::class.java).setAction(ACTION_STOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .setOngoing(true)
            .build()

    override fun onDestroy() {
        engine.stop()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.toolbox.sleeptimer.STOP"
        const val EXTRA_DURATION_SEC = "duration_sec"
        const val CHANNEL_ID = "sleep_timer_channel"
        const val NOTIF_ID = 1011

        fun start(context: Context, durationSec: Int) {
            context.startForegroundService(
                Intent(context, SleepAudioService::class.java).putExtra(EXTRA_DURATION_SEC, durationSec),
            )
        }
        fun stop(context: Context) {
            context.startService(Intent(context, SleepAudioService::class.java).setAction(ACTION_STOP))
        }
    }
}
