package com.toolbox.core.persistence

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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

    val favoriteToolIds: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[FAVORITE_TOOLS] ?: emptySet()
    }

    suspend fun toggleFavorite(toolId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[FAVORITE_TOOLS] ?: emptySet()
            prefs[FAVORITE_TOOLS] = if (toolId in current) {
                current - toolId
            } else {
                current + toolId
            }
        }
    }
}
