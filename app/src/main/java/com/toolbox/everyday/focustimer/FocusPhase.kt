package com.toolbox.everyday.focustimer

enum class FocusPhase(val label: String) {
    Work("Focus"),
    ShortBreak("Short break"),
    LongBreak("Long break"),
}

/** Pomodoro configuration; durations in minutes. Defaults are the classic 25/5/15/4. */
data class FocusConfig(
    val workMin: Int = 25,
    val shortBreakMin: Int = 5,
    val longBreakMin: Int = 15,
    val sessionsBeforeLongBreak: Int = 4,
) {
    fun durationMsFor(phase: FocusPhase): Long = when (phase) {
        FocusPhase.Work -> workMin
        FocusPhase.ShortBreak -> shortBreakMin
        FocusPhase.LongBreak -> longBreakMin
    }.coerceAtLeast(1) * 60_000L
}

/**
 * Given the phase that just finished and how many work sessions have completed in this cycle,
 * returns the next phase. Work → (long break every N sessions, else short break); any break → Work.
 */
fun nextPhase(finished: FocusPhase, completedWorkSessions: Int, config: FocusConfig): FocusPhase =
    when (finished) {
        FocusPhase.Work ->
            if (completedWorkSessions > 0 && completedWorkSessions % config.sessionsBeforeLongBreak == 0) {
                FocusPhase.LongBreak
            } else {
                FocusPhase.ShortBreak
            }
        FocusPhase.ShortBreak, FocusPhase.LongBreak -> FocusPhase.Work
    }
