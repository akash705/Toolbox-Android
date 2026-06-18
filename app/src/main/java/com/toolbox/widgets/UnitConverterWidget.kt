package com.toolbox.widgets

import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

import android.content.Context
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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

private val Context.unitWidgetStore by preferencesDataStore(name = "unit_converter_widget")
private val PAIR_INDEX_KEY = intPreferencesKey("pair_index")
private val INPUT_VALUE_KEY = stringPreferencesKey("input_value")

/**
 * Common conversion pairs: from name, from symbol, to name, to symbol, conversion factor (from -> to)
 */
private data class ConversionPair(
    val fromName: String,
    val fromSymbol: String,
    val toName: String,
    val toSymbol: String,
    val factor: Double,
)

private val PRESET_PAIRS = listOf(
    ConversionPair("kg", "kg", "lb", "lb", 2.20462),
    ConversionPair("km", "km", "mi", "mi", 0.621371),
    ConversionPair("cm", "cm", "in", "in", 0.393701),
    ConversionPair("L", "L", "gal", "gal", 0.264172),
    ConversionPair("m", "m", "ft", "ft", 3.28084),
    ConversionPair("°C", "°C", "°F", "°F", Double.NaN), // special handling
)

private fun convert(pairIndex: Int, value: Double): Double {
    val pair = PRESET_PAIRS[pairIndex]
    return if (pair.fromSymbol == "°C") {
        value * 9.0 / 5.0 + 32.0
    } else {
        value * pair.factor
    }
}

private data class UnitWidgetState(
    val pairIndex: Int = 0,
    val inputValue: String = "1",
)

class UnitConverterWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = loadUnitState(context)
        val pair = PRESET_PAIRS[state.pairIndex.coerceIn(PRESET_PAIRS.indices)]
        val inputNum = state.inputValue.toDoubleOrNull() ?: 1.0
        val result = convert(state.pairIndex.coerceIn(PRESET_PAIRS.indices), inputNum)
        val resultText = if (result == result.toLong().toDouble()) {
            result.toLong().toString()
        } else {
            String.format("%.2f", result)
        }

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(Color(0xFFFFFFFF)))
                        .cornerRadius(16.dp)
                        .padding(12.dp)
                        .clickable(actionStartActivity<MainActivity>(
                            actionParametersOf(ActionParameters.Key<String>("tool_id") to "unit_converter")
                        )),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Input row
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = state.inputValue,
                            style = TextStyle(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(Color(0xFF000000)),
                            ),
                        )
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Text(
                            text = pair.fromSymbol,
                            style = TextStyle(
                                fontSize = 16.sp,
                                color = ColorProvider(Color(0xFF888888)),
                            ),
                        )
                    }
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        text = "=",
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = ColorProvider(Color(0xFF888888)),
                        ),
                    )
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    // Result row
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = resultText,
                            style = TextStyle(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(Color(0xFF1565C0)),
                            ),
                        )
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Text(
                            text = pair.toSymbol,
                            style = TextStyle(
                                fontSize = 16.sp,
                                color = ColorProvider(Color(0xFF888888)),
                            ),
                        )
                    }
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    // Controls: cycle pair, increment value
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = " ⇄ ",
                            modifier = GlanceModifier
                                .background(ColorProvider(Color(0xFFE3F2FD)))
                                .cornerRadius(8.dp)
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .clickable(actionRunCallback<CyclePairAction>()),
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(Color(0xFF1565C0)),
                            ),
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Text(
                            text = " − ",
                            modifier = GlanceModifier
                                .background(ColorProvider(Color(0xFFF0F0F0)))
                                .cornerRadius(8.dp)
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .clickable(actionRunCallback<DecrementValueAction>()),
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(Color(0xFFEF5350)),
                            ),
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Text(
                            text = " + ",
                            modifier = GlanceModifier
                                .background(ColorProvider(Color(0xFFF0F0F0)))
                                .cornerRadius(8.dp)
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .clickable(actionRunCallback<IncrementValueAction>()),
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(Color(0xFF66BB6A)),
                            ),
                        )
                    }
                }
            }
        }
    }
}

private suspend fun loadUnitState(context: Context): UnitWidgetState {
    return try {
        context.unitWidgetStore.data.map { prefs ->
            UnitWidgetState(
                pairIndex = prefs[PAIR_INDEX_KEY] ?: 0,
                inputValue = prefs[INPUT_VALUE_KEY] ?: "1",
            )
        }.first()
    } catch (_: Exception) {
        UnitWidgetState()
    }
}

private suspend fun saveUnitState(context: Context, state: UnitWidgetState) {
    context.unitWidgetStore.edit { prefs ->
        prefs[PAIR_INDEX_KEY] = state.pairIndex
        prefs[INPUT_VALUE_KEY] = state.inputValue
    }
}

class CyclePairAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val state = loadUnitState(context)
        val nextIndex = (state.pairIndex + 1) % PRESET_PAIRS.size
        saveUnitState(context, state.copy(pairIndex = nextIndex, inputValue = "1"))
        UnitConverterWidget().updateAll(context)
    }
}

class IncrementValueAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val state = loadUnitState(context)
        val current = state.inputValue.toDoubleOrNull() ?: 1.0
        val step = when {
            current >= 100 -> 10.0
            current >= 10 -> 5.0
            else -> 1.0
        }
        val newVal = current + step
        val display = if (newVal == newVal.toLong().toDouble()) newVal.toLong().toString() else String.format("%.1f", newVal)
        saveUnitState(context, state.copy(inputValue = display))
        UnitConverterWidget().updateAll(context)
    }
}

class DecrementValueAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val state = loadUnitState(context)
        val current = state.inputValue.toDoubleOrNull() ?: 1.0
        val step = when {
            current > 100 -> 10.0
            current > 10 -> 5.0
            else -> 1.0
        }
        val newVal = maxOf(0.0, current - step)
        val display = if (newVal == newVal.toLong().toDouble()) newVal.toLong().toString() else String.format("%.1f", newVal)
        saveUnitState(context, state.copy(inputValue = display))
        UnitConverterWidget().updateAll(context)
    }
}

class UnitConverterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UnitConverterWidget()
}
