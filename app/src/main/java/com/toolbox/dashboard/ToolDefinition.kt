package com.toolbox.dashboard

import android.hardware.Sensor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.ExposurePlus1
import androidx.compose.material.icons.filled.Flare
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Functions
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class ToolCategory(
    val label: String,
    val tileColor: Color,
    val iconTint: Color,
) {
    MeasurementSensors(
        label = "Measurement & Sensors",
        tileColor = Color(0xFFDCEEFD),
        iconTint = Color(0xFF3B7DD8),
    ),
    ConversionCalculation(
        label = "Conversion & Calculation",
        tileColor = Color(0xFFE5E0F6),
        iconTint = Color(0xFF6B5BAE),
    ),
    Lighting(
        label = "Lighting",
        tileColor = Color(0xFFFFF3D0),
        iconTint = Color(0xFFA67C00),
    ),
    EverydayTools(
        label = "Everyday Tools",
        tileColor = Color(0xFFD4F5E0),
        iconTint = Color(0xFF2E8B57),
    ),
}

data class ToolDefinition(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val category: ToolCategory,
    val description: String = "",
    val requiredSensorType: Int? = null,
    val requiresCamera: Boolean = false,
    val searchKeywords: List<String> = emptyList(),
)

val allTools = listOf(
    // Measurement & Sensors
    ToolDefinition(
        id = "level",
        name = "Bubble Level",
        icon = Icons.Default.Flare,
        category = ToolCategory.MeasurementSensors,
        description = "Check if surfaces are perfectly horizontal or vertical. Place your phone on any surface to see real-time tilt angles. Great for hanging pictures, leveling shelves, or aligning furniture.",
        requiredSensorType = Sensor.TYPE_ACCELEROMETER,
        searchKeywords = listOf("bubble", "spirit", "horizontal", "tilt", "level"),
    ),
    ToolDefinition(
        id = "compass",
        name = "Compass",
        icon = Icons.Default.Explore,
        category = ToolCategory.MeasurementSensors,
        description = "Find cardinal directions using your phone's magnetic sensor. Shows heading in degrees, cardinal direction, and a rotating compass dial. Useful for navigation, hiking, or orienting yourself outdoors.",
        requiredSensorType = Sensor.TYPE_MAGNETIC_FIELD,
        searchKeywords = listOf("direction", "north", "bearing", "heading"),
    ),
    ToolDefinition(
        id = "protractor",
        name = "Protractor",
        icon = Icons.Default.Architecture,
        category = ToolCategory.MeasurementSensors,
        description = "Measure angles by tilting your phone or using the on-screen protractor. Perfect for measuring slopes, roof pitches, or any angle you need to check.",
        searchKeywords = listOf("angle", "degree", "measure"),
    ),
    ToolDefinition(
        id = "sound_meter",
        name = "Sound Meter",
        icon = Icons.Default.GraphicEq,
        category = ToolCategory.MeasurementSensors,
        description = "Measure ambient noise levels in decibels (dB) using your phone's microphone. Shows real-time sound level, min/max readings, and a noise level gauge with reference labels.",
        searchKeywords = listOf("decibel", "db", "noise", "volume", "microphone"),
    ),
    ToolDefinition(
        id = "ruler",
        name = "Ruler",
        icon = Icons.Default.Straighten,
        category = ToolCategory.MeasurementSensors,
        description = "Measure lengths directly on your phone screen in centimeters or inches. Drag markers to measure objects placed against your screen. Calibrated to your device's display.",
        searchKeywords = listOf("measure", "inch", "centimeter", "cm", "length"),
    ),
    ToolDefinition(
        id = "vibrometer",
        name = "Vibrometer",
        icon = Icons.Default.Vibration,
        category = ToolCategory.MeasurementSensors,
        description = "Detect and measure vibrations using the accelerometer. Shows real-time vibration frequency and amplitude with a live waveform display. Useful for checking appliance vibrations or surface stability.",
        requiredSensorType = Sensor.TYPE_ACCELEROMETER,
        searchKeywords = listOf("vibration", "frequency", "waveform", "surface"),
    ),
    ToolDefinition(
        id = "light_meter",
        name = "Light Meter",
        icon = Icons.Default.LightMode,
        category = ToolCategory.MeasurementSensors,
        description = "Measure ambient light intensity in lux using your phone's light sensor. Helpful for photography, checking workplace lighting conditions, or comparing light levels in different rooms.",
        requiredSensorType = Sensor.TYPE_LIGHT,
        searchKeywords = listOf("lux", "light", "ambient", "brightness", "illuminance"),
    ),
    ToolDefinition(
        id = "barometer",
        name = "Barometer",
        icon = Icons.Default.Speed,
        category = ToolCategory.MeasurementSensors,
        description = "Measure atmospheric pressure in hPa using the built-in pressure sensor. Track pressure trends to predict weather changes. Also estimates altitude based on barometric readings.",
        requiredSensorType = Sensor.TYPE_PRESSURE,
        searchKeywords = listOf("barometer", "pressure", "altitude", "weather", "hpa"),
    ),
    ToolDefinition(
        id = "humidity",
        name = "Humidity",
        icon = Icons.Default.WaterDrop,
        category = ToolCategory.MeasurementSensors,
        description = "Monitor relative humidity percentage using the device's humidity sensor. Tracks comfort level with dew point and heat index calculations. Useful for monitoring indoor climate.",
        requiredSensorType = Sensor.TYPE_RELATIVE_HUMIDITY,
        searchKeywords = listOf("humidity", "moisture", "dew", "relative", "comfort"),
    ),
    ToolDefinition(
        id = "pedometer",
        name = "Pedometer",
        icon = Icons.Default.DirectionsWalk,
        category = ToolCategory.MeasurementSensors,
        description = "Count your steps and estimate distance walked using the step counter sensor. Track daily walking activity with a simple start/stop interface. Shows steps, distance, and calories.",
        requiredSensorType = Sensor.TYPE_STEP_COUNTER,
        searchKeywords = listOf("step", "walk", "pedometer", "counter", "fitness", "distance"),
    ),
    ToolDefinition(
        id = "gyroscope",
        name = "Gyroscope",
        icon = Icons.Default.ScreenRotation,
        category = ToolCategory.MeasurementSensors,
        description = "Visualize rotational motion in real time across pitch, roll, and yaw axes. Shows angular velocity data with a 3D orientation display. Useful for testing device sensors or understanding rotation.",
        requiredSensorType = Sensor.TYPE_GYROSCOPE,
        searchKeywords = listOf("gyroscope", "rotation", "pitch", "roll", "yaw", "angular"),
    ),
    ToolDefinition(
        id = "metal_detector",
        name = "Metal Detector",
        icon = Icons.Default.Search,
        category = ToolCategory.MeasurementSensors,
        description = "Detect nearby metal objects using the magnetic field sensor. Shows magnetic field strength and alerts when metal is detected. Can help find studs in walls, lost metal objects, or magnetic sources.",
        requiredSensorType = Sensor.TYPE_MAGNETIC_FIELD,
        searchKeywords = listOf("metal", "magnet", "magnetic", "field", "stud", "ferrous"),
    ),

    // Conversion & Calculation
    ToolDefinition(
        id = "unit_converter",
        name = "Unit Converter",
        icon = Icons.Default.SyncAlt,
        category = ToolCategory.ConversionCalculation,
        description = "Convert between units across 17+ categories including length, weight, temperature, volume, speed, area, and more. Supports all common units with instant real-time conversion.",
        searchKeywords = listOf("convert", "length", "weight", "temperature", "volume"),
    ),
    ToolDefinition(
        id = "percentage",
        name = "Percentage",
        icon = Icons.Default.Percent,
        category = ToolCategory.ConversionCalculation,
        description = "Quickly calculate percentages, discounts, markups, and percentage changes. Enter any two values to find the third. Perfect for shopping discounts or calculating tips.",
        searchKeywords = listOf("percent", "discount", "markup", "ratio"),
    ),
    ToolDefinition(
        id = "tip_calculator",
        name = "Tip Calculator",
        icon = Icons.Default.Payments,
        category = ToolCategory.ConversionCalculation,
        description = "Calculate tips and split bills easily. Enter the bill amount, choose a tip percentage, and split among any number of people. Shows per-person totals with tip included.",
        searchKeywords = listOf("tip", "bill", "split", "restaurant"),
    ),
    ToolDefinition(
        id = "number_base",
        name = "Number Base",
        icon = Icons.Default.Pin,
        category = ToolCategory.ConversionCalculation,
        description = "Convert numbers between decimal, binary, hexadecimal, and octal systems. Useful for programmers, students, and anyone working with different number formats.",
        searchKeywords = listOf("binary", "hex", "octal", "decimal", "base"),
    ),
    ToolDefinition(
        id = "scientific_calculator",
        name = "Calculator",
        icon = Icons.Default.Calculate,
        category = ToolCategory.ConversionCalculation,
        description = "Full scientific calculator with trigonometric functions, logarithms, exponents, factorial, and more. Supports DEG/RAD modes and expression-based input.",
        searchKeywords = listOf("calculator", "scientific", "math", "sin", "cos", "tan", "log", "sqrt"),
    ),

    ToolDefinition(
        id = "date_calculator",
        name = "Date/Age",
        icon = Icons.Default.DateRange,
        category = ToolCategory.ConversionCalculation,
        description = "Calculate days between dates, exact age in years/months/days, and add or subtract days from any date. Includes countdown to next birthday.",
        searchKeywords = listOf("date", "age", "birthday", "difference", "days", "calendar"),
    ),
    ToolDefinition(
        id = "bmi_calculator",
        name = "BMI",
        icon = Icons.Default.FitnessCenter,
        category = ToolCategory.ConversionCalculation,
        description = "Calculate Body Mass Index from height and weight. Supports metric and imperial units with health category display and healthy weight range.",
        searchKeywords = listOf("bmi", "body", "mass", "index", "weight", "health", "obesity"),
    ),
    ToolDefinition(
        id = "aspect_ratio",
        name = "Aspect Ratio",
        icon = Icons.Default.AspectRatio,
        category = ToolCategory.ConversionCalculation,
        description = "Calculate and simplify aspect ratios. Resize dimensions proportionally with ratio lock. Includes common presets like 16:9, 4:3, and A4.",
        searchKeywords = listOf("aspect", "ratio", "resize", "dimension", "proportion", "16:9", "4:3"),
    ),

    // Lighting
    ToolDefinition(
        id = "flashlight",
        name = "Flashlight",
        icon = Icons.Default.FlashlightOn,
        category = ToolCategory.Lighting,
        description = "Turn your phone into a bright flashlight using the camera flash LED. Features adjustable brightness, SOS mode for emergencies, and strobe mode.",
        searchKeywords = listOf("torch", "light", "sos", "strobe"),
    ),

    // Everyday Tools
    ToolDefinition(
        id = "qr_scanner",
        name = "QR & Barcode",
        icon = Icons.Default.QrCodeScanner,
        category = ToolCategory.EverydayTools,
        description = "Scan QR codes and barcodes (EAN, UPC, Code 128, etc.) using your camera. Instantly opens links, copies text, or shows encoded data.",
        requiresCamera = true,
        searchKeywords = listOf("qr", "barcode", "scan", "generate", "code"),
    ),
    ToolDefinition(
        id = "counter",
        name = "Counter",
        icon = Icons.Default.ExposurePlus1,
        category = ToolCategory.EverydayTools,
        description = "A simple tally counter for counting anything. Tap to increment, with undo support. Great for counting people, inventory items, repetitions, or anything else.",
        searchKeywords = listOf("count", "tally", "increment", "clicker"),
    ),
    ToolDefinition(
        id = "stopwatch_timer",
        name = "Stopwatch",
        icon = Icons.Default.Timer,
        category = ToolCategory.EverydayTools,
        description = "Stopwatch with lap timing and countdown timer in one tool. Start, stop, and record laps with precision. The timer supports custom durations with alarm notification.",
        searchKeywords = listOf("stopwatch", "timer", "countdown", "lap"),
    ),
    ToolDefinition(
        id = "random_generator",
        name = "Random/Dice",
        icon = Icons.Default.Casino,
        category = ToolCategory.EverydayTools,
        description = "Generate random numbers, flip coins, and roll dice. Set custom ranges for number generation. Features animated coin flip and dice roll for games and decision making.",
        searchKeywords = listOf("random", "coin", "flip", "dice", "number"),
    ),
    ToolDefinition(
        id = "color_picker",
        name = "Color Picker",
        icon = Icons.Default.Palette,
        category = ToolCategory.EverydayTools,
        description = "Pick colors from the real world using your camera. Point at any object to capture its color with HEX, RGB, and HSL values. Save colors to a palette for design reference.",
        requiresCamera = true,
        searchKeywords = listOf("color", "hex", "rgb", "eyedropper", "palette"),
    ),
    ToolDefinition(
        id = "magnifier",
        name = "Magnifier",
        icon = Icons.Default.ZoomIn,
        category = ToolCategory.EverydayTools,
        description = "Use your camera as a magnifying glass to zoom in on small text, labels, or objects. Supports pinch-to-zoom and flashlight toggle for better visibility.",
        requiresCamera = true,
        searchKeywords = listOf("magnify", "zoom", "magnifying glass", "enlarge"),
    ),
    ToolDefinition(
        id = "mirror",
        name = "Mirror",
        icon = Icons.Default.Person,
        category = ToolCategory.EverydayTools,
        description = "Use the front camera as a mirror. Includes a true mirror mode that shows your un-flipped reflection, so you see yourself as others see you.",
        requiresCamera = true,
        searchKeywords = listOf("mirror", "selfie", "front camera", "reflection", "true mirror"),
    ),
    ToolDefinition(
        id = "heart_rate",
        name = "Heart Rate",
        icon = Icons.Default.Favorite,
        category = ToolCategory.EverydayTools,
        description = "Measure your heart rate by placing your finger over the camera lens. Uses photoplethysmography (PPG) to detect pulse from blood flow changes. For reference only, not a medical device.",
        requiresCamera = true,
        searchKeywords = listOf("heart", "pulse", "bpm", "heartbeat", "ppg", "monitor"),
    ),
    ToolDefinition(
        id = "breathing_exercise",
        name = "Breathing",
        icon = Icons.Default.Air,
        category = ToolCategory.EverydayTools,
        description = "Follow guided breathing patterns to relax and reduce stress. Choose from Box Breathing, 4-7-8, or Relaxing techniques with visual animation, sound cues, and haptic feedback.",
        searchKeywords = listOf("breathe", "breathing", "relax", "meditation", "calm", "stress", "wellness"),
    ),
    ToolDefinition(
        id = "morse_code",
        name = "Morse Code",
        icon = Icons.Default.GraphicEq,
        category = ToolCategory.EverydayTools,
        description = "Translate text to Morse code and back. Play Morse output as sound, haptic vibration, or flashlight blinks. Adjustable speed from 5 to 25 WPM.",
        searchKeywords = listOf("morse", "code", "dots", "dashes", "sos", "translate", "signal"),
    ),
    ToolDefinition(
        id = "password_generator",
        name = "Password",
        icon = Icons.Default.Lock,
        category = ToolCategory.EverydayTools,
        description = "Generate secure random passwords with configurable length and character sets. Features strength indicator, copy to clipboard, and recent history.",
        searchKeywords = listOf("password", "generate", "secure", "random", "key", "strong"),
    ),
    ToolDefinition(
        id = "white_noise",
        name = "Ambient Sounds",
        icon = Icons.Default.Nightlight,
        category = ToolCategory.EverydayTools,
        description = "Ambient sound generator with white, pink, and brown noise plus nature sounds. Features sleep timer for focus, sleep, and relaxation.",
        searchKeywords = listOf("noise", "white", "pink", "brown", "rain", "ocean", "sleep", "ambient", "focus", "fan"),
    ),
    ToolDefinition(
        id = "metronome",
        name = "Metronome",
        icon = Icons.Default.MusicNote,
        category = ToolCategory.EverydayTools,
        description = "Adjustable-tempo metronome with audible click, visual beat indicator, and haptic pulse. Supports tap tempo and time signatures from 2/4 to 6/8.",
        searchKeywords = listOf("metronome", "bpm", "tempo", "beat", "rhythm", "music", "click"),
    ),

    // Phase 11 (Pitch Tuner)
    ToolDefinition(
        id = "pitch_tuner",
        name = "Pitch Tuner",
        icon = Icons.Default.Tune,
        category = ToolCategory.MeasurementSensors,
        description = "Detect musical pitch from microphone input. Shows nearest note, frequency, and cents deviation with a visual tuning gauge. Includes reference tone generator.",
        searchKeywords = listOf("pitch", "tuner", "note", "music", "frequency", "tune", "guitar"),
    ),

    // Phase 13 (Visual Tools)
    ToolDefinition(
        id = "screen_flash",
        name = "Screen Flash",
        icon = Icons.Default.FlashOn,
        category = ToolCategory.Lighting,
        description = "Full-screen color flash for concerts, emergencies, or visual signaling. Supports solid color, strobe mode, and SOS pattern with adjustable frequency.",
        searchKeywords = listOf("flash", "strobe", "screen", "color", "sos", "signal", "emergency"),
    ),
    ToolDefinition(
        id = "plumb_bob",
        name = "Plumb Bob",
        icon = Icons.Default.Plumbing,
        category = ToolCategory.MeasurementSensors,
        description = "Camera overlay with a vertical plumb line for checking vertical alignment. Color-coded deviation indicator shows how far from true vertical.",
        requiresCamera = true,
        requiredSensorType = Sensor.TYPE_ACCELEROMETER,
        searchKeywords = listOf("plumb", "vertical", "level", "alignment", "construction"),
    ),

    // Phase 14 (Connectivity)
    ToolDefinition(
        id = "wifi_signal",
        name = "WiFi Signal",
        icon = Icons.Default.Wifi,
        category = ToolCategory.MeasurementSensors,
        description = "Measure WiFi signal strength in dBm with quality rating. Log readings at different locations and export as CSV for network troubleshooting.",
        searchKeywords = listOf("wifi", "signal", "strength", "rssi", "network", "dbm", "wireless"),
    ),
    ToolDefinition(
        id = "bluetooth_finder",
        name = "Bluetooth Finder",
        icon = Icons.Default.BluetoothSearching,
        category = ToolCategory.MeasurementSensors,
        description = "Find lost Bluetooth devices like earbuds, trackers, and speakers by signal strength. Pick a nearby device and walk around — a playful radar gets warmer, faster, and buzzes as you close in.",
        searchKeywords = listOf("bluetooth", "ble", "finder", "find", "lost", "earbuds", "headphones", "tracker", "signal", "rssi", "proximity"),
    ),

    // Measurement & Sensors (Phase 9)
    ToolDefinition(
        id = "spectrum_analyzer",
        name = "Spectrum",
        icon = Icons.Default.Equalizer,
        category = ToolCategory.MeasurementSensors,
        description = "Visualize audio frequencies in real time using FFT analysis. Shows frequency bands from sub-bass to brilliance with bar and waterfall (spectrogram) views. Useful for music, acoustics, and audio troubleshooting.",
        searchKeywords = listOf("spectrum", "fft", "frequency", "audio", "analyzer", "equalizer"),
    ),

    // Measurement & Sensors (Phase 10)
    ToolDefinition(
        id = "speedometer",
        name = "Speedometer",
        icon = Icons.Default.Speed,
        category = ToolCategory.MeasurementSensors,
        description = "Measure your speed using GPS with a real-time gauge display. Supports km/h, mph, m/s, and knots. Track trips with distance, max speed, and average speed statistics.",
        searchKeywords = listOf("speed", "velocity", "gps", "kmh", "mph", "trip", "distance"),
    ),
    ToolDefinition(
        id = "altitude",
        name = "Altitude",
        icon = Icons.Default.Height,
        category = ToolCategory.MeasurementSensors,
        description = "Measure your altitude above sea level using GPS. Track elevation changes with total ascent and descent. Shows altitude chart, min/max readings, and GPS accuracy.",
        searchKeywords = listOf("altitude", "elevation", "gps", "height", "ascent", "descent"),
    ),

    // Batch 2 — Conversion & Calculation
    ToolDefinition(
        id = "emi_calculator",
        name = "Loan/EMI",
        icon = Icons.Default.AccountBalance,
        category = ToolCategory.ConversionCalculation,
        description = "Calculate monthly EMI, total interest, and total payment for loans. View principal vs. interest breakdown with pie chart and full amortization schedule. Supports reducing balance and flat-rate methods.",
        searchKeywords = listOf("loan", "emi", "mortgage", "interest", "amortization", "calculator", "principal"),
    ),
    ToolDefinition(
        id = "formula_reference",
        name = "Formulas",
        icon = Icons.Default.Functions,
        category = ToolCategory.ConversionCalculation,
        description = "Browse and solve formulas from Math, Physics, Chemistry, and Finance. Enter known values to solve for any unknown with step-by-step substitution. Search, browse by subject, and favorite formulas.",
        searchKeywords = listOf("formula", "solver", "math", "physics", "chemistry", "finance", "equation", "reference"),
    ),

    // Batch 2 — Measurement & Sensors
    ToolDefinition(
        id = "device_info",
        name = "Device Info",
        icon = Icons.Default.PhoneAndroid,
        category = ToolCategory.MeasurementSensors,
        description = "View detailed device information: model, Android version, display specs, storage, RAM, CPU, battery health, and sensor list. All hardware details in one place.",
        searchKeywords = listOf("device", "info", "phone", "specs", "hardware", "android", "battery", "cpu", "ram", "storage"),
    ),
    ToolDefinition(
        id = "network_info",
        name = "Network Info",
        icon = Icons.Default.NetworkCheck,
        category = ToolCategory.MeasurementSensors,
        description = "View network connection details: Wi-Fi SSID, BSSID, signal strength, frequency, channel, IP addresses (local and public), and mobile carrier info. Copy any value to clipboard.",
        searchKeywords = listOf("network", "ip", "address", "carrier", "wifi", "ssid", "bssid", "4g", "5g"),
    ),

    // Batch 2 — Everyday Tools
    ToolDefinition(
        id = "nfc_toolkit",
        name = "NFC Toolkit",
        icon = Icons.Default.Nfc,
        category = ToolCategory.EverydayTools,
        description = "Read, write, format, and erase NFC tags. View tag details including type, UID, memory size, and hex dump of raw data. Supports NDEF text, URL, and contact records.",
        searchKeywords = listOf("nfc", "tag", "ndef", "read", "write", "rfid", "mifare", "contactless"),
    ),
    ToolDefinition(
        id = "tone_generator",
        name = "Tone Generator",
        icon = Icons.Default.GraphicEq,
        category = ToolCategory.EverydayTools,
        description = "Generate continuous tones from 20 Hz to 20 kHz with sine, square, triangle, and sawtooth waveforms. Includes presets for standard tuning (440 Hz), musical notes, and high frequency. Visual waveform preview.",
        searchKeywords = listOf("tone", "frequency", "generator", "sine", "wave", "hertz", "hz", "audio", "sound"),
    ),

    // Batch 3 — Everyday Tools
    ToolDefinition(
        id = "wifi_qr_share",
        name = "WiFi QR Share",
        icon = Icons.Default.Wifi,
        category = ToolCategory.EverydayTools,
        description = "Generate a QR code from a WiFi network name and password so guests can scan and connect. Everything stays on device — nothing is sent anywhere.",
        searchKeywords = listOf("wifi", "qr", "share", "guest", "network", "password", "ssid", "hotspot"),
    ),
    ToolDefinition(
        id = "photo_cleanup",
        name = "Photo Cleanup",
        icon = Icons.Default.Photo,
        category = ToolCategory.EverydayTools,
        description = "Compress photos to save space, or strip EXIF metadata (location, device, timestamps) before sharing. Originals are never touched — results save to Pictures/Toolbox.",
        searchKeywords = listOf("photo", "image", "compress", "resize", "exif", "metadata", "strip", "privacy", "shrink", "size"),
    ),

    // Phase 3 backlog — Conversion & Calculation
    ToolDefinition(
        id = "wire_gauge",
        name = "Wire Gauge",
        icon = Icons.Default.ElectricalServices,
        category = ToolCategory.ConversionCalculation,
        description = "AWG wire gauge reference table with metric (mm², diameter) equivalents and approximate ampacity for chassis and power-transmission use.",
        searchKeywords = listOf("wire", "gauge", "awg", "ampacity", "amp", "current", "electrical", "cable", "mm2"),
    ),
    ToolDefinition(
        id = "paint_calculator",
        name = "Paint Calculator",
        icon = Icons.Default.Brush,
        category = ToolCategory.ConversionCalculation,
        description = "Estimate paint needed for a room. Enter dimensions, doors, windows, and number of coats — get liters or gallons required.",
        searchKeywords = listOf("paint", "wall", "coverage", "room", "gallon", "liter", "renovation"),
    ),
    ToolDefinition(
        id = "screw_bolt",
        name = "Screw & Bolt",
        icon = Icons.Default.Hardware,
        category = ToolCategory.ConversionCalculation,
        description = "Reference table of metric (M3-M20) and imperial (UNC) fasteners with thread pitch, tap drill, clearance hole, and hex wrench size.",
        searchKeywords = listOf("screw", "bolt", "thread", "metric", "imperial", "tap", "drill", "hex", "wrench", "fastener", "unc"),
    ),

    // Phase 3 backlog — Lighting
    ToolDefinition(
        id = "screen_grid",
        name = "Screen Test Grid",
        icon = Icons.Default.GridOn,
        category = ToolCategory.Lighting,
        description = "Full-screen color and grid pattern at maximum brightness. Inspect screen protectors for dust and scratches, or test for stuck pixels.",
        searchKeywords = listOf("screen", "test", "grid", "dead", "pixel", "protector", "dust", "scratch", "display"),
    ),

    // Phase 3 backlog — Everyday Tools
    ToolDefinition(
        id = "tts_reader",
        name = "TTS Reader",
        icon = Icons.Default.RecordVoiceOver,
        category = ToolCategory.EverydayTools,
        description = "Paste any text and have your phone read it aloud. Adjust speech rate and pitch. Uses the device's built-in text-to-speech engine — no network needed.",
        searchKeywords = listOf("tts", "text", "speech", "read", "voice", "speak", "accessibility", "narrate"),
    ),
    ToolDefinition(
        id = "vibration_patterns",
        name = "Vibration Patterns",
        icon = Icons.Default.Waves,
        category = ToolCategory.EverydayTools,
        description = "Play predefined vibration patterns: tap, double-tap, heartbeat, pulse, wave, and SOS. Useful for testing the haptic motor or designing custom alerts.",
        searchKeywords = listOf("vibration", "vibrate", "haptic", "pattern", "buzz", "pulse", "sos", "heartbeat"),
    ),
    ToolDefinition(
        id = "ocr",
        name = "Text Scanner",
        icon = Icons.Default.TextFields,
        category = ToolCategory.EverydayTools,
        description = "Extract text from any photo using on-device ML Kit recognition. Pick a photo of a sign, receipt, business card, or document — copy or share the recognized text. No image leaves your device.",
        searchKeywords = listOf("ocr", "text", "scanner", "recognize", "extract", "card", "receipt", "ml", "kit", "mlkit"),
    ),
    ToolDefinition(
        id = "unit_circle",
        name = "Unit Circle",
        icon = Icons.Default.PieChart,
        category = ToolCategory.ConversionCalculation,
        description = "Interactive unit circle for visualizing trigonometric functions. Drag to set the angle and see sin, cos, and tan values along with degree and radian readouts.",
        searchKeywords = listOf("unit", "circle", "trigonometry", "trig", "sin", "cos", "tan", "angle", "degree", "radian", "math"),
    ),

    // Batch 4 Tools
    ToolDefinition(
        id = "qr_generator",
        name = "QR Generator",
        icon = Icons.Default.QrCode2,
        category = ToolCategory.EverydayTools,
        description = "Generate QR codes for text, links, contacts, email, phone, SMS, and locations. Live preview, share as an image, or save to your gallery. Fully offline.",
        searchKeywords = listOf("qr", "generate", "generator", "vcard", "contact", "url", "link", "code", "barcode"),
    ),
    ToolDefinition(
        id = "notepad",
        name = "Notepad",
        icon = Icons.Default.EditNote,
        category = ToolCategory.EverydayTools,
        description = "A quick local scratchpad for notes. Autosaves as you type, works offline, and never leaves your device.",
        searchKeywords = listOf("notepad", "notes", "note", "memo", "scratchpad", "text", "write"),
    ),
    ToolDefinition(
        id = "focus_timer",
        name = "Focus Timer",
        icon = Icons.Default.Timelapse,
        category = ToolCategory.EverydayTools,
        description = "A Pomodoro focus timer with work and break cycles. Runs in the background so your session keeps counting with the screen off.",
        searchKeywords = listOf("focus", "pomodoro", "timer", "productivity", "study", "work", "break"),
    ),

    // Batch 5 Tools
    ToolDefinition(
        id = "text_utilities",
        name = "Text Utilities",
        icon = Icons.Default.TextFields,
        category = ToolCategory.EverydayTools,
        description = "Transform and inspect text: change case, count words and characters, trim spaces, remove line breaks, slugify, and reverse.",
        searchKeywords = listOf("text", "case", "uppercase", "lowercase", "word count", "character count", "slug", "trim"),
    ),
    ToolDefinition(
        id = "hash_encode",
        name = "Hash & Encode",
        icon = Icons.Default.Tag,
        category = ToolCategory.EverydayTools,
        description = "Generate MD5, SHA-1 and SHA-256 hashes, plus Base64 and URL encoding for any text. Tap a result to copy.",
        searchKeywords = listOf("hash", "md5", "sha", "sha256", "base64", "url encode", "checksum", "encode"),
    ),
    ToolDefinition(
        id = "unit_price",
        name = "Unit Price",
        icon = Icons.Default.ShoppingCart,
        category = ToolCategory.ConversionCalculation,
        description = "Compare two products by price per unit to find the better value while shopping.",
        searchKeywords = listOf("unit price", "compare", "cheaper", "per unit", "shopping", "grocery", "value"),
    ),
    ToolDefinition(
        id = "bill_split",
        name = "Bill Split",
        icon = Icons.Default.Groups,
        category = ToolCategory.ConversionCalculation,
        description = "Split a bill across people with an optional tip, and see the per-person amount.",
        searchKeywords = listOf("bill", "split", "tip", "restaurant", "share", "per person", "divide"),
    ),
    ToolDefinition(
        id = "world_clock",
        name = "World Clock",
        icon = Icons.Default.Public,
        category = ToolCategory.EverydayTools,
        description = "See the current time in cities around the world at a glance. Add and remove cities; everything stays on your device.",
        searchKeywords = listOf("world", "clock", "time", "timezone", "zone", "city", "utc", "gmt"),
    ),
    ToolDefinition(
        id = "sleep_timer",
        name = "Sleep Timer",
        icon = Icons.Default.Bedtime,
        category = ToolCategory.EverydayTools,
        description = "Play gentle ambient sound for a set time to fall asleep to; it stops automatically when the timer ends.",
        searchKeywords = listOf("sleep", "timer", "nap", "white noise", "ambient", "bedtime", "relax"),
    ),
    ToolDefinition(
        id = "habit_tracker",
        name = "Habit Tracker",
        icon = Icons.Default.CheckCircle,
        category = ToolCategory.EverydayTools,
        description = "Track daily habits and build streaks. Mark each habit done every day and watch your streak grow.",
        searchKeywords = listOf("habit", "tracker", "streak", "daily", "routine", "goal", "check"),
    ),
    ToolDefinition(
        id = "eye_test",
        name = "Eye Test",
        icon = Icons.Default.Visibility,
        category = ToolCategory.EverydayTools,
        description = "Quick self-checks: a visual-acuity chart, an astigmatism fan, and a color-discrimination game. Not a medical diagnosis.",
        searchKeywords = listOf("eye", "test", "vision", "acuity", "snellen", "astigmatism", "color blind", "sight"),
    ),
    ToolDefinition(
        id = "voice_recorder",
        name = "Voice Recorder",
        icon = Icons.Default.Mic,
        category = ToolCategory.EverydayTools,
        description = "Record voice memos, play them back, share, and delete. Recordings stay on your device.",
        searchKeywords = listOf("voice", "recorder", "record", "audio", "memo", "dictaphone", "sound"),
    ),
    ToolDefinition(
        id = "pdf_toolkit",
        name = "PDF Toolkit",
        icon = Icons.Default.PictureAsPdf,
        category = ToolCategory.EverydayTools,
        description = "Merge, rotate, extract pages, and add your signature to PDF files. Runs fully offline on your device.",
        searchKeywords = listOf("pdf", "merge", "combine", "rotate", "split", "extract", "sign", "signature", "document"),
    ),

    // Batch 6 Tools
    ToolDefinition(
        id = "document_scanner",
        name = "Document Scanner",
        icon = Icons.Default.DocumentScanner,
        category = ToolCategory.EverydayTools,
        description = "Capture or pick photos of documents and combine them into a single PDF. Fully offline.",
        searchKeywords = listOf("scan", "scanner", "document", "pdf", "camera", "photo to pdf"),
    ),
    ToolDefinition(
        id = "screenshot_stitcher",
        name = "Screenshot Stitcher",
        icon = Icons.Default.Dashboard,
        category = ToolCategory.EverydayTools,
        description = "Combine several screenshots into one long image, then save or share it.",
        searchKeywords = listOf("screenshot", "stitch", "combine", "long", "image", "merge"),
    ),
    ToolDefinition(
        id = "screen_recorder",
        name = "Screen Recorder",
        icon = Icons.Default.Videocam,
        category = ToolCategory.EverydayTools,
        description = "Record your screen to a video file, then play back or share it. Keeps recording while you use other apps.",
        searchKeywords = listOf("screen", "recorder", "record", "video", "capture", "screencast"),
    ),
    ToolDefinition(
        id = "status_saver",
        name = "Status Saver",
        icon = Icons.Default.Download,
        category = ToolCategory.EverydayTools,
        description = "Save photos and videos from WhatsApp statuses you've viewed, before they disappear. Grant access to the statuses folder to begin.",
        searchKeywords = listOf("status", "saver", "whatsapp", "download", "save", "story"),
    ),
    ToolDefinition(
        id = "duplicate_photos",
        name = "Duplicate Photos",
        icon = Icons.Default.Photo,
        category = ToolCategory.EverydayTools,
        description = "Find and delete exact duplicate photos to free up space. All comparison happens on-device — nothing is uploaded.",
        searchKeywords = listOf("duplicate", "photos", "dupes", "clean", "space", "gallery", "storage"),
    ),
    ToolDefinition(
        id = "storage_analyzer",
        name = "Storage Analyzer",
        icon = Icons.Default.PieChart,
        category = ToolCategory.EverydayTools,
        description = "See what's using your storage — largest files, biggest folders, and a breakdown by file type. Delete space hogs with confirmation.",
        searchKeywords = listOf("storage", "analyzer", "space", "cleaner", "files", "folders", "disk", "usage"),
    ),
)
