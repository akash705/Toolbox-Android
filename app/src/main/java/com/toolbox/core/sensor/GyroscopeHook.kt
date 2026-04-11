package com.toolbox.core.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

data class GyroscopeData(
    val x: Float = 0f, // rad/s around X axis (pitch)
    val y: Float = 0f, // rad/s around Y axis (roll)
    val z: Float = 0f, // rad/s around Z axis (yaw)
)

@Composable
fun rememberGyroscopeData(): State<GyroscopeData> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sensorManager = remember { context.getSystemService(SensorManager::class.java) }
    val gyroSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) }
    val data = remember { mutableStateOf(GyroscopeData()) }

    DisposableEffect(lifecycleOwner) {
        if (gyroSensor == null) return@DisposableEffect onDispose {}

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                data.value = GyroscopeData(
                    x = event.values[0],
                    y = event.values[1],
                    z = event.values[2],
                )
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME ->
                    sensorManager.registerListener(listener, gyroSensor, SensorManager.SENSOR_DELAY_UI)
                Lifecycle.Event.ON_PAUSE ->
                    sensorManager.unregisterListener(listener)
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            sensorManager.unregisterListener(listener)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return data
}
