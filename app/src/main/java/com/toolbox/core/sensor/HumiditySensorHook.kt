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

@Composable
fun rememberHumidityData(): State<Float?> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sensorManager = remember { context.getSystemService(SensorManager::class.java) }
    val humiditySensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY) }
    val data = remember { mutableStateOf<Float?>(null) }

    DisposableEffect(lifecycleOwner) {
        if (humiditySensor == null) return@DisposableEffect onDispose {}

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                data.value = event.values[0]
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME ->
                    sensorManager.registerListener(listener, humiditySensor, SensorManager.SENSOR_DELAY_UI)
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

@Composable
fun rememberTemperatureData(): State<Float?> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sensorManager = remember { context.getSystemService(SensorManager::class.java) }
    val tempSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) }
    val data = remember { mutableStateOf<Float?>(null) }

    DisposableEffect(lifecycleOwner) {
        if (tempSensor == null) return@DisposableEffect onDispose {}

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                data.value = event.values[0]
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME ->
                    sensorManager.registerListener(listener, tempSensor, SensorManager.SENSOR_DELAY_UI)
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
