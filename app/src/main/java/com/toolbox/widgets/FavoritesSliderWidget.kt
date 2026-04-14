package com.toolbox.widgets

import android.content.Context
import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
import com.toolbox.core.persistence.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Widget-specific state for slider position
private val Context.favSliderStore by preferencesDataStore(name = "favorites_slider_widget")
private val PAGE_INDEX_KEY = intPreferencesKey("page_index")

private const val ITEMS_PER_PAGE = 4

/** Maps tool IDs to emoji + display name for the widget. */
private val toolEmojiMap = mapOf(
    "level" to ("\uD83D\uDCD0" to "Level"),
    "compass" to ("\uD83E\uDDED" to "Compass"),
    "protractor" to ("\uD83D\uDCD0" to "Protractor"),
    "sound_meter" to ("\uD83D\uDD0A" to "Sound"),
    "ruler" to ("\uD83D\uDCCF" to "Ruler"),
    "vibrometer" to ("\uD83D\uDCF3" to "Vibration"),
    "light_meter" to ("\u2600\uFE0F" to "Light"),
    "barometer" to ("\uD83C\uDF21\uFE0F" to "Barometer"),
    "humidity" to ("\uD83D\uDCA7" to "Humidity"),
    "pedometer" to ("\uD83D\uDEB6" to "Pedometer"),
    "gyroscope" to ("\uD83D\uDD04" to "Gyroscope"),
    "metal_detector" to ("\uD83E\uDDF2" to "Metal"),
    "unit_converter" to ("\uD83D\uDD04" to "Converter"),
    "percentage" to ("\uFF05" to "Percent"),
    "tip_calculator" to ("\uD83D\uDCB0" to "Tip"),
    "number_base" to ("#\uFE0F\u20E3" to "Base"),
    "scientific_calculator" to ("\uD83E\uDDEE" to "Calculator"),
    "date_calculator" to ("\uD83D\uDCC5" to "Date/Age"),
    "bmi_calculator" to ("\uD83C\uDFCB\uFE0F" to "BMI"),
    "aspect_ratio" to ("\uD83D\uDDB2\uFE0F" to "Ratio"),
    "flashlight" to ("\uD83D\uDD26" to "Flashlight"),
    "qr_scanner" to ("\uD83D\uDCF7" to "QR Code"),
    "counter" to ("\uD83D\uDD22" to "Counter"),
    "stopwatch_timer" to ("\u23F1\uFE0F" to "Stopwatch"),
    "random_generator" to ("\uD83C\uDFB2" to "Random"),
    "color_picker" to ("\uD83C\uDFA8" to "Color"),
    "magnifier" to ("\uD83D\uDD0D" to "Magnifier"),
    "mirror" to ("\uD83E\uDE9E" to "Mirror"),
    "heart_rate" to ("\u2764\uFE0F" to "Heart Rate"),
    "breathing_exercise" to ("\uD83C\uDF2C\uFE0F" to "Breathing"),
    "morse_code" to ("\uD83D\uDCE1" to "Morse"),
    "password_generator" to ("\uD83D\uDD12" to "Password"),
    "white_noise" to ("\uD83C\uDF19" to "Ambient"),
    "metronome" to ("\uD83C\uDFB5" to "Metronome"),
    "pitch_tuner" to ("\uD83C\uDFB6" to "Tuner"),
    "screen_flash" to ("\u26A1" to "Flash"),
    "plumb_bob" to ("\uD83E\uDDF1" to "Plumb Bob"),
    "wifi_signal" to ("\uD83D\uDCF6" to "WiFi"),
    "spectrum_analyzer" to ("\uD83C\uDF08" to "Spectrum"),
    "speedometer" to ("\uD83D\uDE97" to "Speed"),
    "altitude" to ("\u26F0\uFE0F" to "Altitude"),
    "emi_calculator" to ("\uD83C\uDFE6" to "Loan/EMI"),
    "formula_reference" to ("\uD83D\uDCD6" to "Formulas"),
    "device_info" to ("\uD83D\uDCF1" to "Device"),
    "network_info" to ("\uD83C\uDF10" to "Network"),
    "nfc_toolkit" to ("\uD83D\uDCE5" to "NFC"),
    "tone_generator" to ("\uD83C\uDFB9" to "Tone Gen"),
)

class FavoritesSliderWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val favorites = loadFavorites(context)
        val pageIndex = loadPageIndex(context)

        provideContent {
            GlanceTheme {
                if (favorites.isEmpty()) {
                    EmptyFavoritesContent()
                } else {
                    SliderContent(favorites, pageIndex)
                }
            }
        }
    }
}

@Composable
private fun EmptyFavoritesContent() {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color.WHITE)
            .cornerRadius(16)
            .padding(16)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "\u2B50",
            style = TextStyle(fontSize = 28.sp),
        )
        Spacer(modifier = GlanceModifier.height(8))
        Text(
            text = "No favorites yet",
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = ColorProvider(Color.GRAY),
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(modifier = GlanceModifier.height(4))
        Text(
            text = "Long-press any tool to favorite it",
            style = TextStyle(
                fontSize = 11.sp,
                color = ColorProvider(Color.LTGRAY),
                textAlign = TextAlign.Center,
            ),
        )
    }
}

@Composable
private fun SliderContent(favorites: List<String>, pageIndex: Int) {
    val totalPages = ((favorites.size + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE).coerceAtLeast(1)
    val safePageIndex = pageIndex.coerceIn(0, totalPages - 1)
    val startIdx = safePageIndex * ITEMS_PER_PAGE
    val pageItems = favorites.subList(startIdx, (startIdx + ITEMS_PER_PAGE).coerceAtMost(favorites.size))
    val hasPrev = safePageIndex > 0
    val hasNext = safePageIndex < totalPages - 1

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color.WHITE)
            .cornerRadius(16)
            .padding(8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Header
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "\u2B50 Favorites",
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(Color.parseColor("#A67C00")),
                ),
            )
            if (totalPages > 1) {
                Text(
                    text = "  ${safePageIndex + 1}/$totalPages",
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = ColorProvider(Color.GRAY),
                    ),
                )
            }
        }
        Spacer(modifier = GlanceModifier.height(6))

        // Slider row: [<] [tool] [tool] [tool] [tool] [>]
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Left arrow
            Text(
                text = " ◀ ",
                modifier = GlanceModifier
                    .cornerRadius(8)
                    .padding(vertical = 8, horizontal = 4)
                    .clickable(
                        if (hasPrev) actionRunCallback<FavSliderPrevAction>()
                        else actionRunCallback<FavSliderNoOpAction>()
                    ),
                style = TextStyle(
                    fontSize = 14.sp,
                    color = ColorProvider(
                        if (hasPrev) Color.parseColor("#333333") else Color.parseColor("#DDDDDD")
                    ),
                ),
            )

            // Tool items
            for (toolId in pageItems) {
                val (emoji, name) = toolEmojiMap[toolId] ?: ("\uD83D\uDD27" to toolId)
                Spacer(modifier = GlanceModifier.width(4))
                FavoriteToolItem(emoji, name, toolId)
            }
            // Fill remaining slots with spacers if less than ITEMS_PER_PAGE
            repeat(ITEMS_PER_PAGE - pageItems.size) {
                Spacer(modifier = GlanceModifier.width(4))
                Spacer(modifier = GlanceModifier.width(52))
            }

            Spacer(modifier = GlanceModifier.width(4))
            // Right arrow
            Text(
                text = " ▶ ",
                modifier = GlanceModifier
                    .cornerRadius(8)
                    .padding(vertical = 8, horizontal = 4)
                    .clickable(
                        if (hasNext) actionRunCallback<FavSliderNextAction>()
                        else actionRunCallback<FavSliderNoOpAction>()
                    ),
                style = TextStyle(
                    fontSize = 14.sp,
                    color = ColorProvider(
                        if (hasNext) Color.parseColor("#333333") else Color.parseColor("#DDDDDD")
                    ),
                ),
            )
        }
    }
}

@Composable
private fun FavoriteToolItem(emoji: String, name: String, toolId: String) {
    Column(
        modifier = GlanceModifier
            .background(Color.parseColor("#F5F5F5"))
            .cornerRadius(12)
            .padding(6)
            .clickable(
                actionStartActivity<MainActivity>(
                    actionParametersOf(ActionParameters.Key<String>("tool_id") to toolId)
                )
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = emoji,
            style = TextStyle(fontSize = 22.sp),
        )
        Spacer(modifier = GlanceModifier.height(2))
        Text(
            text = name,
            style = TextStyle(
                fontSize = 9.sp,
                color = ColorProvider(Color.parseColor("#555555")),
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
        )
    }
}

private suspend fun loadFavorites(context: Context): List<String> {
    return try {
        val repo = UserPreferencesRepository(context)
        repo.favoriteToolIds.first().toList().sorted()
    } catch (_: Exception) {
        emptyList()
    }
}

private suspend fun loadPageIndex(context: Context): Int {
    return try {
        context.favSliderStore.data.map { prefs ->
            prefs[PAGE_INDEX_KEY] ?: 0
        }.first()
    } catch (_: Exception) {
        0
    }
}

private suspend fun savePageIndex(context: Context, index: Int) {
    context.favSliderStore.edit { prefs ->
        prefs[PAGE_INDEX_KEY] = index
    }
}

class FavSliderPrevAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val current = loadPageIndex(context)
        if (current > 0) {
            savePageIndex(context, current - 1)
        }
        FavoritesSliderWidget().updateAll(context)
    }
}

class FavSliderNextAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val current = loadPageIndex(context)
        val favorites = loadFavorites(context)
        val totalPages = ((favorites.size + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE).coerceAtLeast(1)
        if (current < totalPages - 1) {
            savePageIndex(context, current + 1)
        }
        FavoritesSliderWidget().updateAll(context)
    }
}

class FavSliderNoOpAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        // Do nothing — arrow is disabled
    }
}

class FavoritesSliderWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FavoritesSliderWidget()
}
