package com.toolbox.tiles

import android.content.Intent
import android.service.quicksettings.TileService
import com.toolbox.MainActivity

/**
 * Base tile service that deep-links to a specific tool screen.
 * Subclasses just specify the tool ID.
 */
abstract class ToolLaunchTileService(private val toolId: String) : TileService() {

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("tool_id", toolId)
        }
        startActivityAndCollapse(intent)
    }
}

class SoundMeterTileService : ToolLaunchTileService("sound_meter")
class LevelTileService : ToolLaunchTileService("level")
class TimerTileService : ToolLaunchTileService("stopwatch_timer")
