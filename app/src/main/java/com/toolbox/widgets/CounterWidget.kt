package com.toolbox.widgets

import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.stringPreferencesKey
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
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.glance.unit.ColorProvider
import android.graphics.Color
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.appwidget.cornerRadius
import com.toolbox.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import com.toolbox.everyday.counter.CounterData

private val Context.counterDataStore by preferencesDataStore(name = "counters")
private val COUNTERS_KEY = stringPreferencesKey("counters_json")

class CounterWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val counters = loadCounters(context)
        val counter = counters.firstOrNull() ?: CounterData("Counter", 0)

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(Color.WHITE)
                        .cornerRadius(16)
                        .padding(12)
                        .clickable(actionStartActivity<MainActivity>(
                            actionParametersOf(ActionParameters.Key<String>("tool_id") to "counter")
                        )),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = counter.name,
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = ColorProvider(Color.GRAY),
                        ),
                        maxLines = 1,
                    )
                    Spacer(modifier = GlanceModifier.height(4))
                    Text(
                        text = "${counter.value}",
                        style = TextStyle(
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(Color.BLACK),
                            textAlign = TextAlign.Center,
                        ),
                    )
                    Spacer(modifier = GlanceModifier.height(8))
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "  −  ",
                            modifier = GlanceModifier
                                .background(Color.parseColor("#F0F0F0"))
                                .cornerRadius(8)
                                .padding(horizontal = 16, vertical = 6)
                                .clickable(actionRunCallback<DecrementAction>()),
                            style = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(Color.parseColor("#EF5350")),
                            ),
                        )
                        Spacer(modifier = GlanceModifier.width(12))
                        Text(
                            text = "  +  ",
                            modifier = GlanceModifier
                                .background(Color.parseColor("#F0F0F0"))
                                .cornerRadius(8)
                                .padding(horizontal = 16, vertical = 6)
                                .clickable(actionRunCallback<IncrementAction>()),
                            style = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(Color.parseColor("#66BB6A")),
                            ),
                        )
                    }
                }
            }
        }
    }
}

private suspend fun loadCounters(context: Context): List<CounterData> {
    return try {
        val json = context.counterDataStore.data.map { prefs ->
            prefs[COUNTERS_KEY]
        }.first()
        if (json != null) Json.decodeFromString<List<CounterData>>(json) else emptyList()
    } catch (_: Exception) {
        emptyList()
    }
}

private suspend fun saveCounters(context: Context, counters: List<CounterData>) {
    context.counterDataStore.edit { prefs ->
        prefs[COUNTERS_KEY] = Json.encodeToString(kotlinx.serialization.builtins.ListSerializer(CounterData.serializer()), counters)
    }
}

class IncrementAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val counters = loadCounters(context).toMutableList()
        if (counters.isNotEmpty()) {
            counters[0] = counters[0].copy(value = counters[0].value + 1)
            saveCounters(context, counters)
        }
        CounterWidget().updateAll(context)
    }
}

class DecrementAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val counters = loadCounters(context).toMutableList()
        if (counters.isNotEmpty()) {
            counters[0] = counters[0].copy(value = maxOf(0, counters[0].value - 1))
            saveCounters(context, counters)
        }
        CounterWidget().updateAll(context)
    }
}

class CounterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CounterWidget()
}
