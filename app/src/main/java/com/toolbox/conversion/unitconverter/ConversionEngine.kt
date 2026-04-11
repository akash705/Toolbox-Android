package com.toolbox.conversion.unitconverter

object ConversionEngine {
    fun convert(value: Double, from: UnitDef, to: UnitDef): Double {
        val baseValue = from.toBase(value)
        return to.fromBase(baseValue)
    }
}
