package com.toolbox.core.shortcuts

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import com.toolbox.MainActivity
import com.toolbox.dashboard.allTools

object FavoriteShortcutManager {

    fun updateShortcuts(context: Context, favoriteIds: Set<String>) {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return

        val maxShortcuts = shortcutManager.maxShortcutCountPerActivity.coerceAtMost(4)

        val shortcuts = allTools
            .filter { it.id in favoriteIds }
            .take(maxShortcuts)
            .mapIndexed { index, tool ->
                val intent = Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("tool_id", tool.id)
                }
                ShortcutInfo.Builder(context, "fav_${tool.id}")
                    .setShortLabel(tool.name)
                    .setLongLabel(tool.name)
                    .setIcon(Icon.createWithResource(context, iconResForTool(tool.id)))
                    .setIntent(intent)
                    .setRank(index)
                    .build()
            }

        shortcutManager.dynamicShortcuts = shortcuts
    }

    private fun iconResForTool(toolId: String): Int = when (toolId) {
        "flashlight" -> android.R.drawable.ic_menu_gallery
        "sound_meter" -> android.R.drawable.ic_btn_speak_now
        "level" -> android.R.drawable.ic_menu_compass
        "stopwatch_timer" -> android.R.drawable.ic_menu_recent_history
        else -> android.R.drawable.ic_menu_manage
    }
}
