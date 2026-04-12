package com.toolbox.everyday.stopwatch

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat

class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val nm = context.getSystemService(NotificationManager::class.java)

        // Ensure channel exists (receiver may fire after process death)
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val channel = NotificationChannel(
            TimerService.COMPLETION_CHANNEL_ID,
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
        nm.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, TimerService.COMPLETION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Timer complete!")
            .setContentText("Your timer has finished.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(alarmSound)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .setAutoCancel(true)
            .build()
        nm.notify(TimerService.COMPLETION_NOTIFICATION_ID, notification)
    }
}
