package com.toolbox.core.persistence

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { System, Light, Dark }

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val FAVORITE_TOOLS = stringSetPreferencesKey("favorite_tools")
        val PINNED_TOOLS = stringSetPreferencesKey("pinned_tools")
        val FAVORITE_CONVERSIONS = stringSetPreferencesKey("favorite_conversions")
        val DEFAULT_SCREEN = stringPreferencesKey("default_screen")
        val LAUNCH_COUNT = intPreferencesKey("launch_count")
        val RATE_COMPLETED = booleanPreferencesKey("rate_completed")
        val RATE_NEVER_ASK = booleanPreferencesKey("rate_never_ask")
        val RATE_NEXT_ELIGIBLE = intPreferencesKey("rate_next_eligible_launch")
        const val MAX_PINNED = 3
        const val RATE_FIRST_PROMPT_AT = 3
        const val RATE_SNOOZE_LAUNCHES = 5
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        val name = prefs[THEME_MODE] ?: ThemeMode.System.name
        ThemeMode.entries.find { it.name == name } ?: ThemeMode.System
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[THEME_MODE] = mode.name
        }
    }

    val defaultScreenId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[DEFAULT_SCREEN] ?: "dashboard"
    }

    suspend fun setDefaultScreen(screenId: String) {
        context.dataStore.edit { prefs ->
            prefs[DEFAULT_SCREEN] = screenId
        }
    }

    val favoriteToolIds: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[FAVORITE_TOOLS] ?: emptySet()
    }

    suspend fun toggleFavorite(toolId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[FAVORITE_TOOLS] ?: emptySet()
            if (toolId in current) {
                prefs[FAVORITE_TOOLS] = current - toolId
                // Also unpin if removing from favorites
                val pinned = prefs[PINNED_TOOLS] ?: emptySet()
                prefs[PINNED_TOOLS] = pinned - toolId
            } else {
                prefs[FAVORITE_TOOLS] = current + toolId
            }
        }
    }

    val pinnedToolIds: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[PINNED_TOOLS] ?: emptySet()
    }

    suspend fun togglePin(toolId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[PINNED_TOOLS] ?: emptySet()
            prefs[PINNED_TOOLS] = if (toolId in current) {
                current - toolId
            } else if (current.size < MAX_PINNED) {
                current + toolId
            } else {
                current // Already at max, don't add
            }
        }
    }

    // --- Rate-us prompt ---

    /** Emits true when the rate popup should be shown: user hasn't rated or opted out, and has
     *  reached the next eligible launch (first at [RATE_FIRST_PROMPT_AT], then +[RATE_SNOOZE_LAUNCHES]
     *  each time they pick "Not now"). */
    val shouldShowRatePrompt: Flow<Boolean> = context.dataStore.data.map { prefs ->
        val count = prefs[LAUNCH_COUNT] ?: 0
        val completed = prefs[RATE_COMPLETED] ?: false
        val neverAsk = prefs[RATE_NEVER_ASK] ?: false
        val eligibleAt = prefs[RATE_NEXT_ELIGIBLE] ?: RATE_FIRST_PROMPT_AT
        !completed && !neverAsk && count >= eligibleAt
    }

    /** Increment the cold-start launch counter (call once per app launch). */
    suspend fun incrementLaunchCount() {
        context.dataStore.edit { prefs ->
            prefs[LAUNCH_COUNT] = (prefs[LAUNCH_COUNT] ?: 0) + 1
        }
    }

    /** User tapped "Rate" — never auto-prompt again. */
    suspend fun markRated() {
        context.dataStore.edit { it[RATE_COMPLETED] = true }
    }

    /** User tapped "Never ask again". */
    suspend fun setNeverAskRate() {
        context.dataStore.edit { it[RATE_NEVER_ASK] = true }
    }

    /** User tapped "Not now" — re-eligible after [RATE_SNOOZE_LAUNCHES] more launches. */
    suspend fun snoozeRate() {
        context.dataStore.edit { prefs ->
            val count = prefs[LAUNCH_COUNT] ?: 0
            prefs[RATE_NEXT_ELIGIBLE] = count + RATE_SNOOZE_LAUNCHES
        }
    }

    val favoriteConversions: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[FAVORITE_CONVERSIONS] ?: emptySet()
    }

    suspend fun toggleConversionFavorite(conversionKey: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[FAVORITE_CONVERSIONS] ?: emptySet()
            prefs[FAVORITE_CONVERSIONS] = if (conversionKey in current) {
                current - conversionKey
            } else {
                current + conversionKey
            }
        }
    }
}
