package com.toolbox

import android.app.Application
import com.toolbox.core.persistence.UserPreferencesRepository
import com.toolbox.core.shortcuts.FavoriteShortcutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ToolboxApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        val repository = UserPreferencesRepository(this)
        appScope.launch {
            repository.favoriteToolIds.collect { favoriteIds ->
                FavoriteShortcutManager.updateShortcuts(this@ToolboxApplication, favoriteIds)
            }
        }
    }
}
