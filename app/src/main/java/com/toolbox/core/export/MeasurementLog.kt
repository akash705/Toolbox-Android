package com.toolbox.core.export

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MeasurementEntry(
    val toolName: String,
    val value: String,
    val unit: String,
    val label: String,
    val timestamp: Long = System.currentTimeMillis(),
) {
    val formattedTime: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

/**
 * Singleton in-memory measurement log. Entries persist for the current session only.
 */
object MeasurementLog {
    private val entries = mutableListOf<MeasurementEntry>()

    fun log(toolName: String, value: String, unit: String, label: String = "") {
        entries.add(MeasurementEntry(toolName, value, unit, label))
    }

    fun getAll(): List<MeasurementEntry> = entries.toList()

    fun clear() {
        entries.clear()
    }

    fun isEmpty(): Boolean = entries.isEmpty()

    fun toPlainText(): String = buildString {
        appendLine("Toolbox Measurement Log")
        appendLine("=" .repeat(40))
        appendLine()
        entries.forEach { e ->
            appendLine("${e.formattedTime}  |  ${e.toolName}")
            appendLine("  ${e.value} ${e.unit}")
            if (e.label.isNotBlank()) appendLine("  Label: ${e.label}")
            appendLine()
        }
        appendLine("Total: ${entries.size} readings")
    }

    fun toCsv(): String = buildString {
        appendLine("Timestamp,Tool,Value,Unit,Label")
        entries.forEach { e ->
            appendLine("${e.formattedTime},${e.toolName},${e.value},${e.unit},${e.label}")
        }
    }
}
