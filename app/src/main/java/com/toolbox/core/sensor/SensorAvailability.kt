package com.toolbox.core.sensor

import android.content.Context
import android.hardware.SensorManager

class SensorAvailability(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val availableSensors: Set<Int> by lazy {
        buildSet {
            for (sensorType in CHECKED_SENSOR_TYPES) {
                if (sensorManager.getDefaultSensor(sensorType) != null) {
                    add(sensorType)
                }
            }
        }
    }

    fun isSensorAvailable(sensorType: Int): Boolean = sensorType in availableSensors

    fun hasCamera(context: Context): Boolean =
        context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_ANY)

    companion object {
        private val CHECKED_SENSOR_TYPES = intArrayOf(
            android.hardware.Sensor.TYPE_ACCELEROMETER,
            android.hardware.Sensor.TYPE_MAGNETIC_FIELD,
            android.hardware.Sensor.TYPE_LIGHT,
            android.hardware.Sensor.TYPE_PRESSURE,
            android.hardware.Sensor.TYPE_RELATIVE_HUMIDITY,
            android.hardware.Sensor.TYPE_STEP_COUNTER,
            android.hardware.Sensor.TYPE_GYROSCOPE,
        )

        fun sensorName(sensorType: Int): String = when (sensorType) {
            android.hardware.Sensor.TYPE_ACCELEROMETER -> "accelerometer"
            android.hardware.Sensor.TYPE_MAGNETIC_FIELD -> "magnetic field"
            android.hardware.Sensor.TYPE_LIGHT -> "light"
            android.hardware.Sensor.TYPE_PRESSURE -> "pressure"
            android.hardware.Sensor.TYPE_RELATIVE_HUMIDITY -> "humidity"
            android.hardware.Sensor.TYPE_STEP_COUNTER -> "step counter"
            android.hardware.Sensor.TYPE_GYROSCOPE -> "gyroscope"
            else -> "required"
        }
    }
}
