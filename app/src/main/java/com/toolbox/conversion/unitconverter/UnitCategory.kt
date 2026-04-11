package com.toolbox.conversion.unitconverter

enum class UnitCategory(val label: String) {
    Length("Length"),
    Weight("Weight"),
    Volume("Volume"),
    Temperature("Temperature"),
    Speed("Speed"),
    Area("Area"),
    Time("Time"),
    Data("Data"),
    Pressure("Pressure"),
    Energy("Energy"),
}

data class UnitDef(
    val name: String,
    val symbol: String,
    val category: UnitCategory,
    val toBase: (Double) -> Double,
    val fromBase: (Double) -> Double,
)

private fun linearUnit(name: String, symbol: String, category: UnitCategory, factor: Double) = UnitDef(
    name = name,
    symbol = symbol,
    category = category,
    toBase = { it * factor },
    fromBase = { it / factor },
)

val allUnits: List<UnitDef> = buildList {
    // Length (base: meter)
    add(linearUnit("Millimeter", "mm", UnitCategory.Length, 0.001))
    add(linearUnit("Centimeter", "cm", UnitCategory.Length, 0.01))
    add(linearUnit("Meter", "m", UnitCategory.Length, 1.0))
    add(linearUnit("Kilometer", "km", UnitCategory.Length, 1000.0))
    add(linearUnit("Inch", "in", UnitCategory.Length, 0.0254))
    add(linearUnit("Foot", "ft", UnitCategory.Length, 0.3048))
    add(linearUnit("Yard", "yd", UnitCategory.Length, 0.9144))
    add(linearUnit("Mile", "mi", UnitCategory.Length, 1609.344))

    // Weight (base: kilogram)
    add(linearUnit("Milligram", "mg", UnitCategory.Weight, 0.000001))
    add(linearUnit("Gram", "g", UnitCategory.Weight, 0.001))
    add(linearUnit("Kilogram", "kg", UnitCategory.Weight, 1.0))
    add(linearUnit("Metric Ton", "t", UnitCategory.Weight, 1000.0))
    add(linearUnit("Ounce", "oz", UnitCategory.Weight, 0.0283495))
    add(linearUnit("Pound", "lb", UnitCategory.Weight, 0.453592))

    // Volume (base: liter)
    add(linearUnit("Milliliter", "mL", UnitCategory.Volume, 0.001))
    add(linearUnit("Liter", "L", UnitCategory.Volume, 1.0))
    add(linearUnit("US Gallon", "gal", UnitCategory.Volume, 3.78541))
    add(linearUnit("US Quart", "qt", UnitCategory.Volume, 0.946353))
    add(linearUnit("US Cup", "cup", UnitCategory.Volume, 0.236588))
    add(linearUnit("US Fl Oz", "fl oz", UnitCategory.Volume, 0.0295735))
    add(linearUnit("Tablespoon", "tbsp", UnitCategory.Volume, 0.0147868))
    add(linearUnit("Teaspoon", "tsp", UnitCategory.Volume, 0.00492892))

    // Temperature (base: Celsius — formula-based)
    add(UnitDef("Celsius", "°C", UnitCategory.Temperature, { it }, { it }))
    add(UnitDef("Fahrenheit", "°F", UnitCategory.Temperature, { (it - 32) * 5.0 / 9.0 }, { it * 9.0 / 5.0 + 32 }))
    add(UnitDef("Kelvin", "K", UnitCategory.Temperature, { it - 273.15 }, { it + 273.15 }))

    // Speed (base: m/s)
    add(linearUnit("m/s", "m/s", UnitCategory.Speed, 1.0))
    add(linearUnit("km/h", "km/h", UnitCategory.Speed, 1.0 / 3.6))
    add(linearUnit("mph", "mph", UnitCategory.Speed, 0.44704))
    add(linearUnit("Knot", "kn", UnitCategory.Speed, 0.514444))

    // Area (base: m²)
    add(linearUnit("sq mm", "mm²", UnitCategory.Area, 0.000001))
    add(linearUnit("sq cm", "cm²", UnitCategory.Area, 0.0001))
    add(linearUnit("sq m", "m²", UnitCategory.Area, 1.0))
    add(linearUnit("Hectare", "ha", UnitCategory.Area, 10000.0))
    add(linearUnit("sq km", "km²", UnitCategory.Area, 1_000_000.0))
    add(linearUnit("sq in", "in²", UnitCategory.Area, 0.00064516))
    add(linearUnit("sq ft", "ft²", UnitCategory.Area, 0.092903))
    add(linearUnit("Acre", "ac", UnitCategory.Area, 4046.86))
    add(linearUnit("sq mi", "mi²", UnitCategory.Area, 2_589_988.0))

    // Time (base: second)
    add(linearUnit("Millisecond", "ms", UnitCategory.Time, 0.001))
    add(linearUnit("Second", "s", UnitCategory.Time, 1.0))
    add(linearUnit("Minute", "min", UnitCategory.Time, 60.0))
    add(linearUnit("Hour", "h", UnitCategory.Time, 3600.0))
    add(linearUnit("Day", "d", UnitCategory.Time, 86400.0))
    add(linearUnit("Week", "wk", UnitCategory.Time, 604800.0))

    // Data (base: byte)
    add(linearUnit("Bit", "b", UnitCategory.Data, 0.125))
    add(linearUnit("Byte", "B", UnitCategory.Data, 1.0))
    add(linearUnit("Kilobyte", "KB", UnitCategory.Data, 1024.0))
    add(linearUnit("Megabyte", "MB", UnitCategory.Data, 1_048_576.0))
    add(linearUnit("Gigabyte", "GB", UnitCategory.Data, 1_073_741_824.0))
    add(linearUnit("Terabyte", "TB", UnitCategory.Data, 1_099_511_627_776.0))

    // Pressure (base: Pascal)
    add(linearUnit("Pascal", "Pa", UnitCategory.Pressure, 1.0))
    add(linearUnit("Kilopascal", "kPa", UnitCategory.Pressure, 1000.0))
    add(linearUnit("Bar", "bar", UnitCategory.Pressure, 100_000.0))
    add(linearUnit("PSI", "psi", UnitCategory.Pressure, 6894.76))
    add(linearUnit("Atmosphere", "atm", UnitCategory.Pressure, 101_325.0))
    add(linearUnit("mmHg", "mmHg", UnitCategory.Pressure, 133.322))

    // Energy (base: Joule)
    add(linearUnit("Joule", "J", UnitCategory.Energy, 1.0))
    add(linearUnit("Kilojoule", "kJ", UnitCategory.Energy, 1000.0))
    add(linearUnit("Calorie", "cal", UnitCategory.Energy, 4.184))
    add(linearUnit("Kilocalorie", "kcal", UnitCategory.Energy, 4184.0))
    add(linearUnit("Watt-hour", "Wh", UnitCategory.Energy, 3600.0))
    add(linearUnit("kWh", "kWh", UnitCategory.Energy, 3_600_000.0))
}

val unitsByCategory: Map<UnitCategory, List<UnitDef>> = allUnits.groupBy { it.category }
