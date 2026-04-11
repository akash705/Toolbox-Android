package com.toolbox.core.sensor

import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember

/**
 * Returns orientation as [azimuth, pitch, roll] in radians.
 * azimuth: rotation around Z axis (0=north, π/2=east)
 * pitch: rotation around X axis (-π/2 to π/2)
 * roll: rotation around Y axis (-π to π)
 */
@Composable
fun rememberOrientationData(): State<FloatArray> {
    val accel = rememberAccelerometerData()
    val mag = rememberMagnetometerData()

    val rotationMatrix = remember { FloatArray(9) }
    val orientationAngles = remember { FloatArray(3) }

    return remember {
        derivedStateOf {
            val success = SensorManager.getRotationMatrix(rotationMatrix, null, accel.value, mag.value)
            if (success) {
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                orientationAngles.copyOf()
            } else {
                floatArrayOf(0f, 0f, 0f)
            }
        }
    }
}
