package com.toolbox.dashboard

import android.hardware.Sensor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.ExposurePlus1
import androidx.compose.material.icons.filled.Flare
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.ui.graphics.vector.ImageVector

enum class ToolCategory(val label: String) {
    MeasurementSensors("Measurement & Sensors"),
    ConversionCalculation("Conversion & Calculation"),
    Lighting("Lighting"),
    EverydayTools("Everyday Tools"),
}

data class ToolDefinition(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val category: ToolCategory,
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
        requiredSensorType = Sensor.TYPE_ACCELEROMETER,
        searchKeywords = listOf("bubble", "spirit", "horizontal", "tilt", "level"),
    ),
    ToolDefinition(
        id = "compass",
        name = "Compass",
        icon = Icons.Default.Explore,
        category = ToolCategory.MeasurementSensors,
        requiredSensorType = Sensor.TYPE_MAGNETIC_FIELD,
        searchKeywords = listOf("direction", "north", "bearing", "heading"),
    ),
    ToolDefinition(
        id = "protractor",
        name = "Protractor",
        icon = Icons.Default.Architecture,
        category = ToolCategory.MeasurementSensors,
        searchKeywords = listOf("angle", "degree", "measure"),
    ),
    ToolDefinition(
        id = "sound_meter",
        name = "Sound Meter",
        icon = Icons.Default.GraphicEq,
        category = ToolCategory.MeasurementSensors,
        searchKeywords = listOf("decibel", "db", "noise", "volume", "microphone"),
    ),
    ToolDefinition(
        id = "ruler",
        name = "Ruler",
        icon = Icons.Default.Straighten,
        category = ToolCategory.MeasurementSensors,
        searchKeywords = listOf("measure", "inch", "centimeter", "cm", "length"),
    ),

    // Conversion & Calculation
    ToolDefinition(
        id = "unit_converter",
        name = "Unit Converter",
        icon = Icons.Default.SyncAlt,
        category = ToolCategory.ConversionCalculation,
        searchKeywords = listOf("convert", "length", "weight", "temperature", "volume"),
    ),
    ToolDefinition(
        id = "percentage",
        name = "Percentage",
        icon = Icons.Default.Percent,
        category = ToolCategory.ConversionCalculation,
        searchKeywords = listOf("percent", "discount", "markup", "ratio"),
    ),
    ToolDefinition(
        id = "tip_calculator",
        name = "Tip Calculator",
        icon = Icons.Default.Payments,
        category = ToolCategory.ConversionCalculation,
        searchKeywords = listOf("tip", "bill", "split", "restaurant"),
    ),
    ToolDefinition(
        id = "number_base",
        name = "Number Base",
        icon = Icons.Default.Pin,
        category = ToolCategory.ConversionCalculation,
        searchKeywords = listOf("binary", "hex", "octal", "decimal", "base"),
    ),

    // Lighting
    ToolDefinition(
        id = "flashlight",
        name = "Flashlight",
        icon = Icons.Default.FlashlightOn,
        category = ToolCategory.Lighting,
        searchKeywords = listOf("torch", "light", "sos", "strobe"),
    ),

    // Everyday Tools
    ToolDefinition(
        id = "qr_scanner",
        name = "QR Scanner",
        icon = Icons.Default.QrCodeScanner,
        category = ToolCategory.EverydayTools,
        requiresCamera = true,
        searchKeywords = listOf("qr", "barcode", "scan", "generate", "code"),
    ),
    ToolDefinition(
        id = "counter",
        name = "Counter",
        icon = Icons.Default.ExposurePlus1,
        category = ToolCategory.EverydayTools,
        searchKeywords = listOf("count", "tally", "increment", "clicker"),
    ),
    ToolDefinition(
        id = "stopwatch_timer",
        name = "Stopwatch",
        icon = Icons.Default.Timer,
        category = ToolCategory.EverydayTools,
        searchKeywords = listOf("stopwatch", "timer", "countdown", "lap"),
    ),
    ToolDefinition(
        id = "random_generator",
        name = "Random/Dice",
        icon = Icons.Default.Casino,
        category = ToolCategory.EverydayTools,
        searchKeywords = listOf("random", "coin", "flip", "dice", "number"),
    ),
    ToolDefinition(
        id = "color_picker",
        name = "Color Picker",
        icon = Icons.Default.Palette,
        category = ToolCategory.EverydayTools,
        requiresCamera = true,
        searchKeywords = listOf("color", "hex", "rgb", "eyedropper", "palette"),
    ),
    ToolDefinition(
        id = "magnifier",
        name = "Magnifier",
        icon = Icons.Default.ZoomIn,
        category = ToolCategory.EverydayTools,
        requiresCamera = true,
        searchKeywords = listOf("magnify", "zoom", "magnifying glass", "enlarge"),
    ),
)
