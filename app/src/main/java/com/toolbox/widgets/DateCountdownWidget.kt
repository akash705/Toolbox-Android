package com.toolbox.widgets

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
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
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.fillMaxSize as glanceFillMaxSize
import androidx.glance.layout.padding as glancePadding
import androidx.glance.layout.Column as GlanceColumn
import androidx.glance.layout.Spacer as GlanceSpacer
import androidx.glance.layout.height as glanceHeight
import androidx.glance.layout.Alignment as GlanceAlignment
import androidx.glance.text.FontWeight
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.toolbox.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val Context.countdownWidgetStore by preferencesDataStore(name = "countdown_widget")
private val TARGET_DATE_KEY = longPreferencesKey("target_date_epoch")
private val TARGET_LABEL_KEY = stringPreferencesKey("target_label")

private data class CountdownState(
    val targetEpochDay: Long = 0L,
    val label: String = "",
) {
    val isConfigured: Boolean get() = targetEpochDay > 0L

    fun daysRemaining(): Long {
        val target = LocalDate.ofEpochDay(targetEpochDay)
        val today = LocalDate.now()
        return ChronoUnit.DAYS.between(today, target)
    }

    fun targetDateFormatted(): String {
        val target = LocalDate.ofEpochDay(targetEpochDay)
        return target.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }
}

class DateCountdownWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = loadCountdownState(context)

        provideContent {
            GlanceTheme {
                if (!state.isConfigured) {
                    GlanceColumn(
                        modifier = GlanceModifier
                            .glanceFillMaxSize()
                            .background(Color.WHITE)
                            .cornerRadius(16)
                            .glancePadding(12)
                            .clickable(actionStartActivity<CountdownConfigActivity>()),
                        verticalAlignment = GlanceAlignment.CenterVertically,
                        horizontalAlignment = GlanceAlignment.CenterHorizontally,
                    ) {
                        androidx.glance.text.Text(
                            text = "📅",
                            style = TextStyle(fontSize = 28.sp),
                        )
                        GlanceSpacer(modifier = GlanceModifier.glanceHeight(4))
                        androidx.glance.text.Text(
                            text = "Tap to set\ncountdown",
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = ColorProvider(Color.GRAY),
                                textAlign = TextAlign.Center,
                            ),
                        )
                    }
                } else {
                    val days = state.daysRemaining()
                    val bgColor = when {
                        days < 0 -> Color.parseColor("#FFEBEE") // past
                        days == 0L -> Color.parseColor("#FFF3E0") // today
                        days <= 7 -> Color.parseColor("#FFF8E1") // soon
                        else -> Color.WHITE
                    }
                    val daysColor = when {
                        days < 0 -> Color.parseColor("#C62828")
                        days == 0L -> Color.parseColor("#E65100")
                        days <= 7 -> Color.parseColor("#F57F17")
                        else -> Color.parseColor("#1565C0")
                    }

                    GlanceColumn(
                        modifier = GlanceModifier
                            .glanceFillMaxSize()
                            .background(bgColor)
                            .cornerRadius(16)
                            .glancePadding(12)
                            .clickable(actionStartActivity<MainActivity>(
                                actionParametersOf(ActionParameters.Key<String>("tool_id") to "date_calculator")
                            )),
                        verticalAlignment = GlanceAlignment.CenterVertically,
                        horizontalAlignment = GlanceAlignment.CenterHorizontally,
                    ) {
                        if (state.label.isNotBlank()) {
                            androidx.glance.text.Text(
                                text = state.label,
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    color = ColorProvider(Color.GRAY),
                                ),
                                maxLines = 1,
                            )
                            GlanceSpacer(modifier = GlanceModifier.glanceHeight(2))
                        }
                        androidx.glance.text.Text(
                            text = when {
                                days < 0 -> "${-days}"
                                days == 0L -> "Today!"
                                else -> "$days"
                            },
                            style = TextStyle(
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(daysColor),
                                textAlign = TextAlign.Center,
                            ),
                        )
                        if (days != 0L) {
                            androidx.glance.text.Text(
                                text = if (days < 0) "days ago" else if (days == 1L) "day left" else "days left",
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    color = ColorProvider(Color.GRAY),
                                ),
                            )
                        }
                        GlanceSpacer(modifier = GlanceModifier.glanceHeight(2))
                        androidx.glance.text.Text(
                            text = state.targetDateFormatted(),
                            style = TextStyle(
                                fontSize = 10.sp,
                                color = ColorProvider(Color.LTGRAY),
                            ),
                        )
                    }
                }
            }
        }
    }
}

private suspend fun loadCountdownState(context: Context): CountdownState {
    return try {
        context.countdownWidgetStore.data.map { prefs ->
            CountdownState(
                targetEpochDay = prefs[TARGET_DATE_KEY] ?: 0L,
                label = prefs[TARGET_LABEL_KEY] ?: "",
            )
        }.first()
    } catch (_: Exception) {
        CountdownState()
    }
}

private suspend fun saveCountdownState(context: Context, state: CountdownState) {
    context.countdownWidgetStore.edit { prefs ->
        prefs[TARGET_DATE_KEY] = state.targetEpochDay
        prefs[TARGET_LABEL_KEY] = state.label
    }
}

class CountdownConfigActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var label by remember { mutableStateOf("") }
                    val datePickerState = rememberDatePickerState()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Set Countdown",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = label,
                            onValueChange = { label = it },
                            label = { Text("Event name (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        DatePicker(
                            state = datePickerState,
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            onClick = {
                                val selectedMillis = datePickerState.selectedDateMillis
                                if (selectedMillis != null) {
                                    val epochDay = Instant.ofEpochMilli(selectedMillis)
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()
                                        .toEpochDay()
                                    val ctx = this@CountdownConfigActivity
                                    ctx.lifecycleScope.launch {
                                        saveCountdownState(ctx, CountdownState(epochDay, label))
                                        DateCountdownWidget().updateAll(ctx)
                                    }
                                }
                                setResult(Activity.RESULT_OK)
                                finish()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

class DateCountdownWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DateCountdownWidget()
}
