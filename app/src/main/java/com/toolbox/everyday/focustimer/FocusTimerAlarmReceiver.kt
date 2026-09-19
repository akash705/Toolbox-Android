package com.toolbox.everyday.focustimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * Backup for the tick loop: fires at a phase boundary even if the process was reaped. It hands the
 * advance to [FocusTimerService], which rebuilds phase state from persistence and schedules the
 * next phase. Starting an FGS from the background is permitted here because an allow-while-idle
 * alarm grants a temporary exemption; if the OS still refuses (e.g. after force-stop), the timer is
 * unrecoverable until the user reopens the app and the UI reconciles from persisted state.
 */
class FocusTimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val serviceIntent = Intent(context, FocusTimerService::class.java)
            .setAction(FocusTimerService.ACTION_ADVANCE)
        runCatching {
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}
