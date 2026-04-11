package com.toolbox.conversion.unitconverter

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class UnitConverterState(
    val category: UnitCategory = UnitCategory.Length,
    val fromUnit: UnitDef = unitsByCategory[UnitCategory.Length]!!.first(),
    val toUnit: UnitDef = unitsByCategory[UnitCategory.Length]!![2], // meter
    val fromValue: String = "",
    val toValue: String = "",
)

class UnitConverterViewModel : ViewModel() {

    private val _state = MutableStateFlow(UnitConverterState())
    val state: StateFlow<UnitConverterState> = _state.asStateFlow()

    fun onCategoryChanged(category: UnitCategory) {
        val units = unitsByCategory[category] ?: return
        _state.update {
            UnitConverterState(
                category = category,
                fromUnit = units[0],
                toUnit = units.getOrElse(1) { units[0] },
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
