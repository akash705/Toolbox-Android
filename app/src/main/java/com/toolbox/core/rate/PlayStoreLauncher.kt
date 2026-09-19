package com.toolbox.core.rate

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Opens this app's Play Store listing, preferring the Play app and falling back to the browser. */
object PlayStoreLauncher {
    fun open(context: Context) {
        val pkg = context.packageName
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(marketIntent)
        } catch (_: ActivityNotFoundException) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$pkg"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(webIntent)
            } catch (_: ActivityNotFoundException) {
                // No browser or Play Store available; nothing else we can do.
            }
        }
    }
}
