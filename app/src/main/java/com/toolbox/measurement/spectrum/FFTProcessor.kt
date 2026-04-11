package com.toolbox.measurement.spectrum

import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Simple radix-2 Cooley-Tukey FFT processor.
 * Operates on 1024-point buffers at 44100 Hz sample rate.
 */
class FFTProcessor(private val fftSize: Int = 1024) {

    private val hannWindow = FloatArray(fftSize) { i ->
        (0.5 * (1.0 - cos(2.0 * Math.PI * i / (fftSize - 1)))).toFloat()
    }

    /**
     * Frequency band definitions for spectrum display.
     */
    enum class FrequencyBand(val label: String, val minFreq: Float, val maxFreq: Float) {
        SUB_BASS("Sub-bass", 20f, 60f),
        BASS("Bass", 60f, 250f),
        LOW_MID("Low-mid", 250f, 500f),
        MID("Mid", 500f, 2000f),
        HIGH_MID("High-mid", 2000f, 4000f),
        PRESENCE("Presence", 4000f, 6000f),
        BRILLIANCE("Brilliance", 6000f, 20000f),
    }

    data class SpectrumResult(
        val bandMagnitudes: FloatArray, // dB value per band (7 bands)
        val peakFrequency: Float,       // Hz
        val peakLevel: Float,           // dB
        val averageLevel: Float,        // dB
    )

    /**
     * Process a buffer of PCM 16-bit samples and return spectrum data.
     * @param samples Raw PCM samples (Short values as Float)
     * @param sampleRate Sample rate in Hz
     */
    fun process(samples: FloatArray, sampleRate: Int): SpectrumResult {
        val n = fftSize.coerceAtMost(samples.size)

        // Apply Hann window
        val windowed = FloatArray(fftSize)
        for (i in 0 until n) {
            windowed[i] = samples[i] * hannWindow[i]
        }

        // Compute FFT
        val real = windowed.copyOf()
        val imag = FloatArray(fftSize)
        fft(real, imag)

        // Calculate magnitudes (only first half — Nyquist)
        val halfSize = fftSize / 2
        val magnitudes = FloatArray(halfSize)
        val freqResolution = sampleRate.toFloat() / fftSize

        var peakMag = 0f
        var peakBin = 0

        for (i in 0 until halfSize) {
            val mag = sqrt(real[i] * real[i] + imag[i] * imag[i]) / halfSize
            magnitudes[i] = mag
            if (mag > peakMag && i > 0) { // skip DC
                peakMag = mag
                peakBin = i
            }
        }

        // Calculate band magnitudes in dB
        val bandMagnitudes = FloatArray(FrequencyBand.entries.size)
        for ((bandIdx, band) in FrequencyBand.entries.withIndex()) {
            val startBin = (band.minFreq / freqResolution).toInt().coerceIn(1, halfSize - 1)
            val endBin = (band.maxFreq / freqResolution).toInt().coerceIn(startBin + 1, halfSize)

            var bandSum = 0f
            var count = 0
            for (i in startBin until endBin) {
                bandSum += magnitudes[i] * magnitudes[i]
                count++
            }

            val rms = if (count > 0) sqrt(bandSum / count) else 0f
            bandMagnitudes[bandIdx] = if (rms > 0) (20 * log10(rms)).coerceAtLeast(-60f) else -60f
        }

        // Peak frequency
        val peakFrequency = peakBin * freqResolution

        // Peak level in dB
        val peakLevel = if (peakMag > 0) (20 * log10(peakMag)).coerceAtLeast(-60f) else -60f

        // Average level
        val avgMag = magnitudes.drop(1).average().toFloat()
        val averageLevel = if (avgMag > 0) (20 * log10(avgMag)).coerceAtLeast(-60f) else -60f

        return SpectrumResult(bandMagnitudes, peakFrequency, peakLevel, averageLevel)
    }

    /**
     * In-place radix-2 Cooley-Tukey FFT.
     */
    private fun fft(real: FloatArray, imag: FloatArray) {
        val n = real.size
        if (n == 1) return

        // Bit-reversal permutation
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit

            if (i < j) {
                var temp = real[i]; real[i] = real[j]; real[j] = temp
                temp = imag[i]; imag[i] = imag[j]; imag[j] = temp
            }
        }

        // FFT butterfly
        var len = 2
        while (len <= n) {
            val halfLen = len / 2
            val angle = -2.0 * Math.PI / len
            val wReal = cos(angle).toFloat()
            val wImag = kotlin.math.sin(angle).toFloat()

            var i = 0
            while (i < n) {
                var curReal = 1f
                var curImag = 0f
                for (k in 0 until halfLen) {
                    val tReal = curReal * real[i + k + halfLen] - curImag * imag[i + k + halfLen]
                    val tImag = curReal * imag[i + k + halfLen] + curImag * real[i + k + halfLen]

                    real[i + k + halfLen] = real[i + k] - tReal
                    imag[i + k + halfLen] = imag[i + k] - tImag
                    real[i + k] = real[i + k] + tReal
                    imag[i + k] = imag[i + k] + tImag

                    val newCurReal = curReal * wReal - curImag * wImag
                    curImag = curReal * wImag + curImag * wReal
                    curReal = newCurReal
                }
                i += len
            }
            len = len shl 1
        }
    }
}
