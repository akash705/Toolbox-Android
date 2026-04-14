package com.toolbox.widgets

import android.content.Context
import android.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.toolbox.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.stopwatchWidgetStore by preferencesDataStore(name = "stopwatch_widget")
private val SW_RUNNING_KEY = booleanPreferencesKey("sw_running")
private val SW_START_TIME_KEY = longPreferencesKey("sw_start_time")
private val SW_ACCUMULATED_KEY = longPreferencesKey("sw_accumulated")

data class StopwatchWidgetState(
    val isRunning: Boolean = false,
    val startTimeMs: Long = 0L,
    val accumulatedMs: Long = 0L,
) {
    val displayMs: Long
        get() = if (isRunning) {
            accumulatedMs + (System.currentTimeMillis() - startTimeMs)
        } else {
            accumulatedMs
        }
}

class StopwatchWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = loadStopwatchState(context)
        val elapsedMs = state.displayMs
        val timeText = formatTime(elapsedMs)

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(Color.WHITE)
                        .cornerRadius(16)
                        .padding(12)
                        .clickable(actionStartActivity<MainActivity>(
                            actionParametersOf(ActionParameters.Key<String>("tool_id") to "stopwatch_timer")
                        )),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Stopwatch",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = ColorProvider(Color.GRAY),
                        ),
                        maxLines = 1,
                    )
                    Spacer(modifier = GlanceModifier.height(4))
                    Text(
                        text = timeText,
                        style = TextStyle(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(
                                if (state.isRunning) Color.parseColor("#2E7D32") else Color.BLACK
                            ),
                            textAlign = TextAlign.Center,
                        ),
                    )
                    Spacer(modifier = GlanceModifier.height(8))
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Start/Pause button
                        Text(
                            text = if (state.isRunning) " ⏸ " else " ▶ ",
                            modifier = GlanceModifier
                                .background(
                                    if (state.isRunning) Color.parseColor("#FFF3E0")
                                    else Color.parseColor("#E8F5E9")
                                )
                                .cornerRadius(8)
                                .padding(horizontal = 14, vertical = 6)
                                .clickable(actionRunCallback<StopwatchStartPauseAction>()),
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(
                                    if (state.isRunning) Color.parseColor("#E65100")
                                    else Color.parseColor("#2E7D32")
                                ),
                            ),
                        )
                        Spacer(modifier = GlanceModifier.width(10))
                        // Reset button
                        Text(
                            text = " ↺ ",
                            modifier = GlanceModifier
                                .background(Color.parseColor("#F0F0F0"))
                                .cornerRadius(8)
                                .padding(horizontal = 14, vertical = 6)
                                .clickable(actionRunCallback<StopwatchResetAction>()),
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(Color.GRAY),
                            ),
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val tenths = (ms % 1000) / 100
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d.%d", minutes, seconds, tenths)
    }
}

private suspend fun loadStopwatchState(context: Context): StopwatchWidgetState {
    return try {
        context.stopwatchWidgetStore.data.map { prefs ->
            StopwatchWidgetState(
                isRunning = prefs[SW_RUNNING_KEY] ?: false,
                startTimeMs = prefs[SW_START_TIME_KEY] ?: 0L,
                accumulatedMs = prefs[SW_ACCUMULATED_KEY] ?: 0L,
            )
        }.first()
    } catch (_: Exception) {
        StopwatchWidgetState()
    }
}

private suspend fun saveStopwatchState(context: Context, state: StopwatchWidgetState) {
    context.stopwatchWidgetStore.edit { prefs ->
        prefs[SW_RUNNING_KEY] = state.isRunning
        prefs[SW_START_TIME_KEY] = state.startTimeMs
        prefs[SW_ACCUMULATED_KEY] = state.accumulatedMs
    }
}

class StopwatchStartPauseAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val state = loadStopwatchState(context)
        val newState = if (state.isRunning) {
            // Pause: accumulate elapsed time
            val elapsed = System.currentTimeMillis() - state.startTimeMs
            state.copy(
                isRunning = false,
                accumulatedMs = state.accumulatedMs + elapsed,
                startTimeMs = 0L,
            )
        } else {
            // Start: record start time
            state.copy(
                isRunning = true,
                startTimeMs = System.currentTimeMillis(),
            )
        }
        saveStopwatchState(context, newState)
        StopwatchWidget().updateAll(context)
    }
}

class StopwatchResetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        saveStopwatchState(context, StopwatchWidgetState())
        StopwatchWidget().updateAll(context)
    }
}

class StopwatchWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StopwatchWidget()
}
