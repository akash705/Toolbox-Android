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
    Power("Power"),
    Force("Force"),
    Torque("Torque"),
    Density("Density"),
    FuelEconomy("Fuel Economy"),
    Angle("Angle"),
    Frequency("Frequency"),
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
    add(linearUnit("Nautical Mile", "nmi", UnitCategory.Length, 1852.0))
    add(linearUnit("Micrometer", "µm", UnitCategory.Length, 0.000001))

    // Weight (base: kilogram)
    add(linearUnit("Milligram", "mg", UnitCategory.Weight, 0.000001))
    add(linearUnit("Gram", "g", UnitCategory.Weight, 0.001))
    add(linearUnit("Kilogram", "kg", UnitCategory.Weight, 1.0))
    add(linearUnit("Metric Ton", "t", UnitCategory.Weight, 1000.0))
    add(linearUnit("Ounce", "oz", UnitCategory.Weight, 0.0283495))
    add(linearUnit("Pound", "lb", UnitCategory.Weight, 0.453592))
    add(linearUnit("Stone", "st", UnitCategory.Weight, 6.35029))
    add(linearUnit("US Ton", "ton", UnitCategory.Weight, 907.185))
    add(linearUnit("Imperial Ton", "long ton", UnitCategory.Weight, 1016.05))
    add(linearUnit("Carat", "ct", UnitCategory.Weight, 0.0002))

    // Volume (base: liter)
    add(linearUnit("Milliliter", "mL", UnitCategory.Volume, 0.001))
    add(linearUnit("Liter", "L", UnitCategory.Volume, 1.0))
    add(linearUnit("US Gallon", "gal", UnitCategory.Volume, 3.78541))
    add(linearUnit("US Quart", "qt", UnitCategory.Volume, 0.946353))
    add(linearUnit("US Cup", "cup", UnitCategory.Volume, 0.236588))
    add(linearUnit("US Fl Oz", "fl oz", UnitCategory.Volume, 0.0295735))
    add(linearUnit("Tablespoon", "tbsp", UnitCategory.Volume, 0.0147868))
    add(linearUnit("Teaspoon", "tsp", UnitCategory.Volume, 0.00492892))
    add(linearUnit("Imperial Gallon", "imp gal", UnitCategory.Volume, 4.54609))
    add(linearUnit("Imperial Pint", "imp pt", UnitCategory.Volume, 0.568261))
    add(linearUnit("Cubic Meter", "m³", UnitCategory.Volume, 1000.0))
    add(linearUnit("Cubic Foot", "ft³", UnitCategory.Volume, 28.3168))
    add(linearUnit("Cubic Inch", "in³", UnitCategory.Volume, 0.0163871))

    // Temperature (base: Celsius — formula-based)
    add(UnitDef("Celsius", "°C", UnitCategory.Temperature, { it }, { it }))
    add(UnitDef("Fahrenheit", "°F", UnitCategory.Temperature, { (it - 32) * 5.0 / 9.0 }, { it * 9.0 / 5.0 + 32 }))
    add(UnitDef("Kelvin", "K", UnitCategory.Temperature, { it - 273.15 }, { it + 273.15 }))

    // Speed (base: m/s)
    add(linearUnit("m/s", "m/s", UnitCategory.Speed, 1.0))
    add(linearUnit("km/h", "km/h", UnitCategory.Speed, 1.0 / 3.6))
    add(linearUnit("mph", "mph", UnitCategory.Speed, 0.44704))
    add(linearUnit("Knot", "kn", UnitCategory.Speed, 0.514444))
    add(linearUnit("ft/s", "ft/s", UnitCategory.Speed, 0.3048))
    add(linearUnit("cm/s", "cm/s", UnitCategory.Speed, 0.01))

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

    // Power (base: Watt)
    add(linearUnit("Watt", "W", UnitCategory.Power, 1.0))
    add(linearUnit("Kilowatt", "kW", UnitCategory.Power, 1000.0))
    add(linearUnit("Megawatt", "MW", UnitCategory.Power, 1_000_000.0))
    add(linearUnit("Horsepower", "hp", UnitCategory.Power, 745.7))
    add(linearUnit("BTU/hr", "BTU/hr", UnitCategory.Power, 0.293071))

    // Force (base: Newton)
    add(linearUnit("Newton", "N", UnitCategory.Force, 1.0))
    add(linearUnit("Kilonewton", "kN", UnitCategory.Force, 1000.0))
    add(linearUnit("Pound-force", "lbf", UnitCategory.Force, 4.44822))
    add(linearUnit("Dyne", "dyn", UnitCategory.Force, 0.00001))
    add(linearUnit("Kilogram-force", "kgf", UnitCategory.Force, 9.80665))

    // Torque (base: N·m)
    add(linearUnit("Newton-meter", "N·m", UnitCategory.Torque, 1.0))
    add(linearUnit("Foot-pound", "ft·lb", UnitCategory.Torque, 1.35582))
    add(linearUnit("Inch-pound", "in·lb", UnitCategory.Torque, 0.112985))
    add(linearUnit("Kilogram-meter", "kg·m", UnitCategory.Torque, 9.80665))

    // Density (base: kg/m³)
    add(linearUnit("kg/m³", "kg/m³", UnitCategory.Density, 1.0))
    add(linearUnit("g/cm³", "g/cm³", UnitCategory.Density, 1000.0))
    add(linearUnit("lb/ft³", "lb/ft³", UnitCategory.Density, 16.0185))
    add(linearUnit("lb/gal", "lb/gal", UnitCategory.Density, 119.826))

    // Fuel Economy (base: km/L — inverse relationship for L/100km)
    add(UnitDef("km/L", "km/L", UnitCategory.FuelEconomy, { it }, { it }))
    add(UnitDef("mpg (US)", "mpg", UnitCategory.FuelEconomy, { it * 0.425144 }, { it / 0.425144 }))
    add(UnitDef("mpg (UK)", "mpg UK", UnitCategory.FuelEconomy, { it * 0.354006 }, { it / 0.354006 }))
    add(UnitDef("L/100km", "L/100km", UnitCategory.FuelEconomy,
        toBase = { if (it > 0) 100.0 / it else 0.0 },
        fromBase = { if (it > 0) 100.0 / it else 0.0 },
    ))

    // Angle (base: degree)
    add(linearUnit("Degree", "°", UnitCategory.Angle, 1.0))
    add(UnitDef("Radian", "rad", UnitCategory.Angle,
        toBase = { Math.toDegrees(it) },
        fromBase = { Math.toRadians(it) },
    ))
    add(linearUnit("Gradian", "grad", UnitCategory.Angle, 0.9))
    add(linearUnit("Arcminute", "′", UnitCategory.Angle, 1.0 / 60.0))
    add(linearUnit("Arcsecond", "″", UnitCategory.Angle, 1.0 / 3600.0))

    // Frequency (base: Hertz)
    add(linearUnit("Hertz", "Hz", UnitCategory.Frequency, 1.0))
    add(linearUnit("Kilohertz", "kHz", UnitCategory.Frequency, 1000.0))
    add(linearUnit("Megahertz", "MHz", UnitCategory.Frequency, 1_000_000.0))
    add(linearUnit("Gigahertz", "GHz", UnitCategory.Frequency, 1_000_000_000.0))
    add(linearUnit("RPM", "rpm", UnitCategory.Frequency, 1.0 / 60.0))
}

val unitsByCategory: Map<UnitCategory, List<UnitDef>> = allUnits.groupBy { it.category }
