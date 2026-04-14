package com.toolbox.widgets

import android.content.Context
import android.graphics.Color
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
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
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.flashlightDataStore by preferencesDataStore(name = "flashlight_widget")
private val TORCH_ON_KEY = booleanPreferencesKey("torch_on")

class FlashlightWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val isOn = loadTorchState(context)

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(if (isOn) Color.parseColor("#FFF9C4") else Color.WHITE)
                        .cornerRadius(16)
                        .padding(8)
                        .clickable(actionRunCallback<ToggleFlashlightAction>()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = if (isOn) "\uD83D\uDD26" else "\uD83D\uDD26",
                        style = TextStyle(fontSize = 32.sp),
                    )
                    Spacer(modifier = GlanceModifier.height(4))
                    Text(
                        text = if (isOn) "ON" else "OFF",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(
                                if (isOn) Color.parseColor("#F57F17") else Color.GRAY
                            ),
                            textAlign = TextAlign.Center,
                        ),
                    )
                }
            }
        }
    }
}

private suspend fun loadTorchState(context: Context): Boolean {
    return try {
        context.flashlightDataStore.data.map { prefs ->
            prefs[TORCH_ON_KEY] ?: false
        }.first()
    } catch (_: Exception) {
        false
    }
}

private suspend fun saveTorchState(context: Context, isOn: Boolean) {
    context.flashlightDataStore.edit { prefs ->
        prefs[TORCH_ON_KEY] = isOn
    }
}

class ToggleFlashlightAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val currentState = loadTorchState(context)
        val newState = !currentState

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return

        try {
            cameraManager.setTorchMode(cameraId, newState)
            saveTorchState(context, newState)
        } catch (_: Exception) {
            saveTorchState(context, false)
        }

        FlashlightWidget().updateAll(context)
    }
}

class FlashlightWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FlashlightWidget()
}
