package com.toolbox.core.sensor

object LowPassFilter {
    /**
     * Applies a low-pass filter to smooth sensor noise.
     * @param input new sensor values
     * @param output previous filtered values (modified in place)
     * @param alpha smoothing factor (0..1). Lower = smoother but laggier. 0.15 is a good default.
     */
    fun apply(input: FloatArray, output: FloatArray, alpha: Float = 0.15f): FloatArray {
        for (i in input.indices) {
            output[i] = output[i] + alpha * (input[i] - output[i])
        }
        return output
    }
}
