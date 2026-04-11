package com.toolbox.everyday.heartrate

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.util.LinkedList

/**
 * PPG (photoplethysmography) analyzer — extracts average red channel intensity
 * from camera frames to detect blood pulse when fingertip covers the lens.
 *
 * Call [reset] before each measurement session.
 */
class HeartRateAnalyzer(
    private val onRedIntensity: (Float) -> Unit,
    private val onBpmResult: (Int) -> Unit,
    private val onFingerDetected: (Boolean) -> Unit,
) : ImageAnalysis.Analyzer {

    private val redSamples = LinkedList<Pair<Long, Float>>() // timestamp to red intensity
    private val windowMs = 15_000L // 15-second measurement window
    private var measuring = false

    fun startMeasuring() {
        measuring = true
        redSamples.clear()
    }

    fun reset() {
        measuring = false
        redSamples.clear()
    }

    override fun analyze(image: ImageProxy) {
        val buffer = image.planes[0].buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)

        // Average brightness of the frame (Y plane in YUV)
        var sum = 0L
        for (byte in data) {
            sum += (byte.toInt() and 0xFF)
        }
        val avgBrightness = sum.toFloat() / data.size

        // Finger is detected when the camera is mostly covered (red-ish, medium brightness)
        // With flash on and finger covering, brightness is typically 50-200
        val fingerDetected = avgBrightness in 30f..220f
        onFingerDetected(fingerDetected)

        if (measuring && fingerDetected) {
            val now = System.currentTimeMillis()
            onRedIntensity(avgBrightness)
            redSamples.add(now to avgBrightness)

            // Remove samples older than window
            while (redSamples.isNotEmpty() && now - redSamples.first.first > windowMs) {
                redSamples.removeFirst()
            }

            // Need at least 5 seconds of data to attempt BPM calculation
            if (redSamples.size > 60) {
                val bpm = calculateBpm()
                if (bpm in 40..200) {
                    onBpmResult(bpm)
                }
            }
        }

        image.close()
    }

    private fun calculateBpm(): Int {
        if (redSamples.size < 30) return 0

        val values = redSamples.map { it.second }
        val timestamps = redSamples.map { it.first }

        // Simple moving average to smooth signal
        val smoothed = values.windowed(5) { it.average().toFloat() }

        if (smoothed.size < 10) return 0

        // Find peaks (local maxima above mean)
        val mean = smoothed.average().toFloat()
        val peaks = mutableListOf<Int>()

        for (i in 1 until smoothed.size - 1) {
            if (smoothed[i] > smoothed[i - 1] &&
                smoothed[i] > smoothed[i + 1] &&
                smoothed[i] > mean
            ) {
                // Minimum distance between peaks (~0.3s at ~30fps)
                if (peaks.isEmpty() || i - peaks.last() > 8) {
                    peaks.add(i)
                }
            }
        }

        if (peaks.size < 2) return 0

        // Calculate average interval between peaks
        val intervals = mutableListOf<Long>()
        val offset = 2 // windowed smoothing offset
        for (i in 1 until peaks.size) {
            val idx1 = (peaks[i - 1] + offset).coerceAtMost(timestamps.size - 1)
            val idx2 = (peaks[i] + offset).coerceAtMost(timestamps.size - 1)
            intervals.add(timestamps[idx2] - timestamps[idx1])
        }

        val avgIntervalMs = intervals.average()
        return if (avgIntervalMs > 0) (60_000.0 / avgIntervalMs).toInt() else 0
    }
}
