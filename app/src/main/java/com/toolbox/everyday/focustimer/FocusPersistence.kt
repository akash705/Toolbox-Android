package com.toolbox.everyday.focustimer

import android.content.Context
import android.content.Intent

/**
 * Lightweight SharedPreferences persistence for the Focus Timer, so an alarm-driven advance can
 * rebuild phase state after a recoverable process reap, and the UI can reconcile on next launch
 * after a force-stop/reboot.
 */
object FocusPersistence {

    private const val PREFS = "focus_timer_state"
    private const val K_ACTIVE = "active"
    private const val K_PHASE = "phase"
    private const val K_COMPLETED = "completed"
    private const val K_END = "end_time_ms"
    private const val K_PAUSED = "paused"
    private const val K_WORK = "work_min"
    private const val K_SHORT = "short_min"
    private const val K_LONG = "long_min"
    private const val K_SESSIONS = "sessions_before_long"

    const val EXTRA_WORK = "extra_work_min"
    const val EXTRA_SHORT = "extra_short_min"
    const val EXTRA_LONG = "extra_long_min"
    const val EXTRA_SESSIONS = "extra_sessions"

    data class Snapshot(
        val config: FocusConfig,
        val phase: FocusPhase,
        val completedWorkSessions: Int,
        val endTimeMs: Long,
        val paused: Boolean,
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(context: Context, s: Snapshot) {
        prefs(context).edit().apply {
            putBoolean(K_ACTIVE, true)
            putString(K_PHASE, s.phase.name)
            putInt(K_COMPLETED, s.completedWorkSessions)
            putLong(K_END, s.endTimeMs)
            putBoolean(K_PAUSED, s.paused)
            putInt(K_WORK, s.config.workMin)
            putInt(K_SHORT, s.config.shortBreakMin)
            putInt(K_LONG, s.config.longBreakMin)
            putInt(K_SESSIONS, s.config.sessionsBeforeLongBreak)
            apply()
        }
    }

    fun load(context: Context): Snapshot? {
        val p = prefs(context)
        if (!p.getBoolean(K_ACTIVE, false)) return null
        val phase = runCatching { FocusPhase.valueOf(p.getString(K_PHASE, null) ?: return null) }
            .getOrNull() ?: return null
        return Snapshot(
            config = FocusConfig(
                workMin = p.getInt(K_WORK, 25),
                shortBreakMin = p.getInt(K_SHORT, 5),
                longBreakMin = p.getInt(K_LONG, 15),
                sessionsBeforeLongBreak = p.getInt(K_SESSIONS, 4),
            ),
            phase = phase,
            completedWorkSessions = p.getInt(K_COMPLETED, 0),
            endTimeMs = p.getLong(K_END, 0L),
            paused = p.getBoolean(K_PAUSED, false),
        )
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    /** Read the config carried on an ACTION_START intent (falls back to defaults). */
    fun readConfig(context: Context, intent: Intent): FocusConfig {
        val d = FocusConfig()
        return FocusConfig(
            workMin = intent.getIntExtra(EXTRA_WORK, d.workMin),
            shortBreakMin = intent.getIntExtra(EXTRA_SHORT, d.shortBreakMin),
            longBreakMin = intent.getIntExtra(EXTRA_LONG, d.longBreakMin),
            sessionsBeforeLongBreak = intent.getIntExtra(EXTRA_SESSIONS, d.sessionsBeforeLongBreak),
        )
    }
}
