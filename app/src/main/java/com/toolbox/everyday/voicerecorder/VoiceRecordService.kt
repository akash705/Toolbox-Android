package com.toolbox.everyday.voicerecorder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.toolbox.MainActivity
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
import java.io.File

/** Process-wide state for voice recording, observed by the screen. */
object VoiceRecordState {
    private val _recording = MutableStateFlow(false)
    val recording: StateFlow<Boolean> = _recording.asStateFlow()
    private val _elapsedSec = MutableStateFlow(0)
    val elapsedSec: StateFlow<Int> = _elapsedSec.asStateFlow()
    internal fun set(recording: Boolean, elapsed: Int) {
        _recording.value = recording
        _elapsedSec.value = elapsed
    }
    internal fun setElapsed(v: Int) { _elapsedSec.value = v }
}

/**
 * Records the microphone to an .m4a file while running as a [microphone] foreground service so the
 * recording continues when the screen is off or the app is backgrounded. Started only after the UI
 * has confirmed RECORD_AUDIO is granted; shows a persistent notification with a Stop action and
 * stops itself when the user stops recording.
 */
class VoiceRecordService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tickJob: Job? = null
    private var recorder: MediaRecorder? = null
    private var elapsed = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Voice Recorder", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows while a voice memo is recording"
                setSound(null, null)
            },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopRecording()
            return START_NOT_STICKY
        }

        ServiceCompat.startForeground(
            this, NOTIF_ID, buildNotification(0),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
        )

        val dir = File(filesDir, "recordings").apply { mkdirs() }
        val file = File(dir, "rec_${System.currentTimeMillis()}.m4a")
        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()
        val started = runCatching {
            rec.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
        }.isSuccess
        if (!started) {
            runCatching { rec.release() }
            stopRecording()
            return START_NOT_STICKY
        }
        recorder = rec
        elapsed = 0
        VoiceRecordState.set(true, 0)

        tickJob?.cancel()
        tickJob = scope.launch {
            while (true) {
                delay(1000)
                elapsed += 1
                VoiceRecordState.setElapsed(elapsed)
                getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification(elapsed))
            }
        }
        return START_NOT_STICKY
    }

    private fun stopRecording() {
        tickJob?.cancel()
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        VoiceRecordState.set(false, 0)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(elapsedSec: Int) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Recording voice memo")
            .setContentText("%02d:%02d".format(elapsedSec / 60, elapsedSec % 60))
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
                    Intent(this, VoiceRecordService::class.java).setAction(ACTION_STOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .setOngoing(true)
            .build()

    override fun onDestroy() {
        runCatching { recorder?.release() }
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.toolbox.voicerecord.STOP"
        const val CHANNEL_ID = "voice_record_channel"
        const val NOTIF_ID = 1012

        fun start(context: Context) {
            context.startForegroundService(Intent(context, VoiceRecordService::class.java))
        }
        fun stop(context: Context) {
            context.startService(Intent(context, VoiceRecordService::class.java).setAction(ACTION_STOP))
        }
    }
}
