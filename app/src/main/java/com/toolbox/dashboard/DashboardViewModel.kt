package com.toolbox.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.toolbox.core.sensor.SensorAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

data class DashboardUiState(
    val query: String = "",
    val tools: List<ToolDefinition> = allTools,
    val disabledToolIds: Set<String> = emptySet(),
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val sensorAvailability = SensorAvailability(application)

    private val _query = MutableStateFlow("")

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            disabledToolIds = computeDisabledTools(),
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun onQueryChanged(query: String) {
        _query.value = query
        _uiState.update { state ->
            state.copy(
                query = query,
                tools = filterTools(query),
            )
        }
    }

    private fun filterTools(query: String): List<ToolDefinition> {
        if (query.isBlank()) return allTools
        val lower = query.lowercase()
        return allTools.filter { tool ->
            tool.name.lowercase().contains(lower) ||
                tool.searchKeywords.any { it.contains(lower) }
        }
    }

    private fun computeDisabledTools(): Set<String> {
        val context = getApplication<Application>()
        return allTools.filter { tool ->
            val sensorMissing = tool.requiredSensorType != null &&
                !sensorAvailability.isSensorAvailable(tool.requiredSensorType)
            val cameraMissing = tool.requiresCamera &&
                !sensorAvailability.hasCamera(context)
            sensorMissing || cameraMissing
        }.map { it.id }.toSet()
    }
}
