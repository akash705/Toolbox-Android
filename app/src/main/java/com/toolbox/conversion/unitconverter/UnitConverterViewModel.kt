package com.toolbox.conversion.unitconverter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.toolbox.core.persistence.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UnitConverterState(
    val category: UnitCategory = UnitCategory.Length,
    val fromUnit: UnitDef = unitsByCategory[UnitCategory.Length]!!.first(),
    val toUnit: UnitDef = unitsByCategory[UnitCategory.Length]!![2], // meter
    val fromValue: String = "",
    val toValue: String = "",
    val favoriteConversions: Set<String> = emptySet(),
) {
    val isCurrentFavorite: Boolean
        get() = conversionKey(category, fromUnit.symbol, toUnit.symbol) in favoriteConversions
}

fun conversionKey(category: UnitCategory, fromSymbol: String, toSymbol: String): String =
    "${category.name}:$fromSymbol→$toSymbol"

class UnitConverterViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = UserPreferencesRepository(application)
    private val _state = MutableStateFlow(UnitConverterState())
    val state: StateFlow<UnitConverterState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.favoriteConversions.collect { favorites ->
                _state.update { it.copy(favoriteConversions = favorites) }
            }
        }
    }

    fun onCategoryChanged(category: UnitCategory) {
        val units = unitsByCategory[category] ?: return
        _state.update {
            it.copy(
                category = category,
                fromUnit = units[0],
                toUnit = units.getOrElse(1) { units[0] },
                fromValue = "",
                toValue = "",
            )
        }
    }

    fun onFromUnitChanged(unit: UnitDef) {
        _state.update { s ->
            val newTo = recalculate(s.fromValue, unit, s.toUnit)
            s.copy(fromUnit = unit, toValue = newTo)
        }
    }

    fun onToUnitChanged(unit: UnitDef) {
        _state.update { s ->
            val newTo = recalculate(s.fromValue, s.fromUnit, unit)
            s.copy(toUnit = unit, toValue = newTo)
        }
    }

    fun onFromValueChanged(value: String) {
        _state.update { s ->
            val result = recalculate(value, s.fromUnit, s.toUnit)
            s.copy(fromValue = value, toValue = result)
        }
    }

    fun onToValueChanged(value: String) {
        _state.update { s ->
            val result = recalculate(value, s.toUnit, s.fromUnit)
            s.copy(toValue = value, fromValue = result)
        }
    }

    fun swapUnits() {
        _state.update { s ->
            val result = recalculate(s.fromValue, s.toUnit, s.fromUnit)
            s.copy(fromUnit = s.toUnit, toUnit = s.fromUnit, toValue = result)
        }
    }

    fun toggleCurrentFavorite() {
        val s = _state.value
        val key = conversionKey(s.category, s.fromUnit.symbol, s.toUnit.symbol)
        viewModelScope.launch {
            preferencesRepository.toggleConversionFavorite(key)
        }
    }

    fun applyFavorite(key: String) {
        val parts = key.split(":")
        if (parts.size != 2) return
        val categoryName = parts[0]
        val pair = parts[1].split("→")
        if (pair.size != 2) return
        val fromSymbol = pair[0]
        val toSymbol = pair[1]
        val category = UnitCategory.entries.find { it.name == categoryName } ?: return
        val units = unitsByCategory[category] ?: return
        val from = units.find { it.symbol == fromSymbol } ?: return
        val to = units.find { it.symbol == toSymbol } ?: return
        _state.update { s ->
            s.copy(
                category = category,
                fromUnit = from,
                toUnit = to,
                fromValue = s.fromValue,
                toValue = recalculate(s.fromValue, from, to),
            )
        }
    }

    private fun recalculate(input: String, from: UnitDef, to: UnitDef): String {
        if (input.isBlank()) return ""
        val value = input.toDoubleOrNull() ?: return ""
        val result = ConversionEngine.convert(value, from, to)
        return if (result == result.toLong().toDouble() && result < 1e15) {
            result.toLong().toString()
        } else {
            "%.6g".format(result)
        }
    }
}
