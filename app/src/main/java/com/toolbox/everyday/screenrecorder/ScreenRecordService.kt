package com.toolbox.everyday.screenrecorder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.toolbox.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/** Process-wide state for the screen recorder (dedicated to this tool). */
object ScreenRecordState {
    private val _recording = MutableStateFlow(false)
    val recording: StateFlow<Boolean> = _recording.asStateFlow()
    internal fun set(value: Boolean) { _recording.value = value }
}

/** Records the screen via MediaProjection + MediaRecorder while running as a mediaProjection FGS. */
class ScreenRecordService : Service() {

    private var projection: MediaProjection? = null
    private var recorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var outFile: File? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Screen Recorder", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows while the screen is being recorded"
            },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopRecording()
            return START_NOT_STICKY
        }
        ServiceCompat.startForeground(this, NOTIF_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)

        val code = intent?.getIntExtra(EXTRA_CODE, 0) ?: 0
        @Suppress("DEPRECATION")
        val data: Intent? = intent?.getParcelableExtra(EXTRA_DATA)
        if (data == null) { stopSelf(); return START_NOT_STICKY }

        val withAudio = intent?.getBooleanExtra(EXTRA_AUDIO, false) ?: false

        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val proj = mpm.getMediaProjection(code, data) ?: run { stopSelf(); return START_NOT_STICKY }
        proj.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() { stopRecording() }
        }, null)
        projection = proj

        startRecording(proj, withAudio)
        ScreenRecordState.set(true)
        return START_NOT_STICKY
    }

    private fun startRecording(proj: MediaProjection, withAudio: Boolean) {
        val metrics = resources.displayMetrics
        val width = (metrics.widthPixels / 2) * 2   // even dimensions for the encoder
        val height = (metrics.heightPixels / 2) * 2
        val dpi = metrics.densityDpi

        val dir = File(filesDir, "screen_recordings").apply { mkdirs() }
        val file = File(dir, "screen_${System.currentTimeMillis()}.mp4")
        outFile = file

        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()
        rec.apply {
            // MediaRecorder requires audio source before output format, and the audio
            // encoder after it. Video source/encoder are set alongside.
            if (withAudio) setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            if (withAudio) {
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44_100)
            }
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setVideoSize(width, height)
            setVideoFrameRate(30)
            setVideoEncodingBitRate(8_000_000)
            setOutputFile(file.absolutePath)
            prepare()
        }
        recorder = rec

        virtualDisplay = proj.createVirtualDisplay(
            "toolbox-screen",
            width, height, dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            rec.surface, null, null,
        )
        rec.start()
    }

    private fun stopRecording() {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching { projection?.stop() }
        projection = null
        ScreenRecordState.set(false)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.presence_video_online)
        .setContentTitle("Recording screen")
        .setContentText("Tap the app to stop")
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        .setOngoing(true)
        .build()

    override fun onDestroy() {
        runCatching { recorder?.release() }
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.toolbox.screenrecord.STOP"
        const val EXTRA_CODE = "code"
        const val EXTRA_DATA = "data"
        const val EXTRA_AUDIO = "audio"
        const val CHANNEL_ID = "screen_record_channel"
        const val NOTIF_ID = 1010
    }
}
