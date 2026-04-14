package com.toolbox.widgets

import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.text.NumberFormat
import java.time.LocalDate
import kotlin.coroutines.resume

private val Context.pedometerWidgetStore by preferencesDataStore(name = "pedometer_widget")
private val STEP_COUNT_KEY = intPreferencesKey("step_count")
private val BASELINE_KEY = intPreferencesKey("baseline")
private val BASELINE_DATE_KEY = stringPreferencesKey("baseline_date")
private val SESSION_ACTIVE_KEY = booleanPreferencesKey("session_active")
private val SESSION_BASELINE_KEY = intPreferencesKey("session_baseline")
private val SESSION_START_KEY = longPreferencesKey("session_start_time")

data class PedometerWidgetState(
    val totalStepsSinceBoot: Int = 0,
    val baseline: Int = 0,
    val baselineDate: String = "",
    val sessionActive: Boolean = false,
    val sessionBaseline: Int = 0,
    val sessionStartTimeMs: Long = 0L,
) {
    val dailySteps: Int
        get() {
            val today = LocalDate.now().toString()
            return if (baselineDate == today && baseline > 0) {
                (totalStepsSinceBoot - baseline).coerceAtLeast(0)
            } else {
                0
            }
        }

    val sessionSteps: Int
        get() = if (sessionActive && sessionBaseline > 0) {
            (totalStepsSinceBoot - sessionBaseline).coerceAtLeast(0)
        } else {
            0
        }

    val sessionDurationMs: Long
        get() = if (sessionActive && sessionStartTimeMs > 0) {
            System.currentTimeMillis() - sessionStartTimeMs
        } else {
            0L
        }
}

class PedometerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = loadPedometerState(context)
        val numberFormat = NumberFormat.getNumberInstance()

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(Color.WHITE)
                        .cornerRadius(16)
                        .padding(12)
                        .clickable(actionStartActivity<MainActivity>(
                            actionParametersOf(
                                ActionParameters.Key<String>("tool_id") to "pedometer"
                            )
                        )),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Title
                    Text(
                        text = "Pedometer",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = ColorProvider(Color.GRAY),
                        ),
                        maxLines = 1,
                    )
                    Spacer(modifier = GlanceModifier.height(4))

                    // Step count
                    Text(
                        text = numberFormat.format(state.dailySteps),
                        style = TextStyle(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(Color.BLACK),
                            textAlign = TextAlign.Center,
                        ),
                    )
                    Text(
                        text = "steps today",
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = ColorProvider(Color.parseColor("#2E7D32")),
                        ),
                    )

                    // Session info (when active)
                    if (state.sessionActive) {
                        Spacer(modifier = GlanceModifier.height(4))
                        Text(
                            text = "${numberFormat.format(state.sessionSteps)} session · ${formatDuration(state.sessionDurationMs)}",
                            style = TextStyle(
                                fontSize = 11.sp,
                                color = ColorProvider(Color.parseColor("#1565C0")),
                            ),
                            maxLines = 1,
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(8))

                    // Buttons row
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Refresh button
                        Text(
                            text = " ↻ ",
                            modifier = GlanceModifier
                                .background(Color.parseColor("#F0F0F0"))
                                .cornerRadius(8)
                                .padding(horizontal = 12, vertical = 6)
                                .clickable(actionRunCallback<PedometerRefreshAction>()),
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(Color.GRAY),
                            ),
                        )
                        Spacer(modifier = GlanceModifier.width(8))
                        // Session start/stop button
                        Text(
                            text = if (state.sessionActive) " ⏹ " else " ▶ ",
                            modifier = GlanceModifier
                                .background(
                                    if (state.sessionActive) Color.parseColor("#FFEBEE")
                                    else Color.parseColor("#E8F5E9")
                                )
                                .cornerRadius(8)
                                .padding(horizontal = 12, vertical = 6)
                                .clickable(actionRunCallback<PedometerSessionAction>()),
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(
                                    if (state.sessionActive) Color.parseColor("#C62828")
                                    else Color.parseColor("#2E7D32")
                                ),
                            ),
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

/**
 * Reads the current step counter value from the hardware sensor.
 * Returns null if sensor is unavailable or times out.
 */
private suspend fun readStepCounterOnce(context: Context): Int? {
    val sensorManager = context.getSystemService(SensorManager::class.java) ?: return null
    val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return null

    return withTimeoutOrNull(3000L) {
        suspendCancellableCoroutine { cont ->
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    sensorManager.unregisterListener(this)
                    cont.resume(event.values[0].toInt())
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
            cont.invokeOnCancellation { sensorManager.unregisterListener(listener) }
        }
    }
}

private suspend fun loadPedometerState(context: Context): PedometerWidgetState {
    return try {
        context.pedometerWidgetStore.data.map { prefs ->
            PedometerWidgetState(
                totalStepsSinceBoot = prefs[STEP_COUNT_KEY] ?: 0,
                baseline = prefs[BASELINE_KEY] ?: 0,
                baselineDate = prefs[BASELINE_DATE_KEY] ?: "",
                sessionActive = prefs[SESSION_ACTIVE_KEY] ?: false,
                sessionBaseline = prefs[SESSION_BASELINE_KEY] ?: 0,
                sessionStartTimeMs = prefs[SESSION_START_KEY] ?: 0L,
            )
        }.first()
    } catch (_: Exception) {
        PedometerWidgetState()
    }
}

private suspend fun savePedometerState(context: Context, state: PedometerWidgetState) {
    context.pedometerWidgetStore.edit { prefs ->
        prefs[STEP_COUNT_KEY] = state.totalStepsSinceBoot
        prefs[BASELINE_KEY] = state.baseline
        prefs[BASELINE_DATE_KEY] = state.baselineDate
        prefs[SESSION_ACTIVE_KEY] = state.sessionActive
        prefs[SESSION_BASELINE_KEY] = state.sessionBaseline
        prefs[SESSION_START_KEY] = state.sessionStartTimeMs
    }
}

/**
 * Reads the sensor and updates stored step count + resets baseline if new day.
 */
private suspend fun refreshStepCount(context: Context): PedometerWidgetState {
    val currentSteps = readStepCounterOnce(context)
    val state = loadPedometerState(context)
    val today = LocalDate.now().toString()

    val updated = if (currentSteps != null) {
        val newBaseline = if (state.baselineDate != today) {
            // New day — set today's baseline to current sensor value
            currentSteps
        } else {
            state.baseline
        }
        state.copy(
            totalStepsSinceBoot = currentSteps,
            baseline = newBaseline,
            baselineDate = today,
        )
    } else {
        state
    }

    savePedometerState(context, updated)
    return updated
}

class PedometerRefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        refreshStepCount(context)
        PedometerWidget().updateAll(context)
    }
}

class PedometerSessionAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val state = refreshStepCount(context)

        val updated = if (state.sessionActive) {
            // Stop session
            state.copy(
                sessionActive = false,
                sessionBaseline = 0,
                sessionStartTimeMs = 0L,
            )
        } else {
            // Start session
            state.copy(
                sessionActive = true,
                sessionBaseline = state.totalStepsSinceBoot,
                sessionStartTimeMs = System.currentTimeMillis(),
            )
        }
        savePedometerState(context, updated)
        PedometerWidget().updateAll(context)
    }
}

class PedometerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PedometerWidget()
}
