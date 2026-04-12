package com.toolbox.widgets

import android.content.Context
import android.graphics.Color
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.sp
import androidx.glance.unit.ColorProvider
import com.toolbox.MainActivity

class QuickLaunchWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(Color.WHITE)
                        .cornerRadius(16)
                        .padding(8),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Toolbox",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(Color.parseColor("#1B5E20")),
                            textAlign = TextAlign.Center,
                        ),
                    )
                    Spacer(modifier = GlanceModifier.height(8))
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ToolButton("\uD83D\uDD26", "flashlight")
                        Spacer(modifier = GlanceModifier.width(6))
                        ToolButton("\uD83D\uDCD0", "level")
                        Spacer(modifier = GlanceModifier.width(6))
                        ToolButton("\uD83E\uDDED", "compass")
                        Spacer(modifier = GlanceModifier.width(6))
                        ToolButton("\u23F1\uFE0F", "stopwatch_timer")
                    }
                    Spacer(modifier = GlanceModifier.height(6))
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ToolButton("\uD83D\uDCCF", "ruler")
                        Spacer(modifier = GlanceModifier.width(6))
                        ToolButton("\uD83D\uDD0A", "sound_meter")
                        Spacer(modifier = GlanceModifier.width(6))
                        ToolButton("\uD83C\uDFB2", "random")
                        Spacer(modifier = GlanceModifier.width(6))
                        ToolButton("\uD83D\uDCF7", "qr_scanner")
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolButton(emoji: String, toolId: String) {
    Text(
        text = emoji,
        modifier = GlanceModifier
            .background(Color.parseColor("#F5F5F5"))
            .cornerRadius(12)
            .padding(10)
            .clickable(
                actionStartActivity<MainActivity>(
                    actionParametersOf(ActionParameters.Key<String>("tool_id") to toolId)
                )
            ),
        style = TextStyle(fontSize = 20.sp),
    )
}

class QuickLaunchWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickLaunchWidget()
}
