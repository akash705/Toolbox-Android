package com.toolbox.nav

import kotlinx.serialization.Serializable

@Serializable object Dashboard

// Measurement & Sensors
@Serializable object Level
@Serializable object Compass
@Serializable object Protractor
@Serializable object SoundMeter
@Serializable object Ruler

// Conversion & Calculation
@Serializable object UnitConverter
@Serializable object PercentageCalculator
@Serializable object TipCalculator
@Serializable object NumberBase

// Lighting & Display
@Serializable object Flashlight

// Everyday Tools
@Serializable object QrScanner
@Serializable object Counter
@Serializable object StopwatchTimer
@Serializable object RandomGenerator
@Serializable object ColorPicker
@Serializable object Magnifier
@Serializable object Mirror

// Phase 6 Tools
@Serializable object Vibrometer
@Serializable object LightMeter
@Serializable object MetalDetector
@Serializable object Barometer
@Serializable object Humidity

// Phase 8 Tools
@Serializable object Pedometer
@Serializable object Gyroscope

// Phase 9 Tools
@Serializable object HeartRate
@Serializable object SpectrumAnalyzer

// Phase 10 Tools
@Serializable object Speedometer
@Serializable object Altitude
