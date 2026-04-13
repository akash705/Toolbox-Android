package com.toolbox.conversion.formula

import kotlin.math.*

enum class Subject(val label: String) {
    Math("Math"),
    Physics("Physics"),
    Chemistry("Chemistry"),
    Finance("Finance"),
}

data class Variable(
    val symbol: String,
    val name: String,
    val unit: String = "",
)

data class Formula(
    val id: String,
    val name: String,
    val subject: Subject,
    val category: String,
    val expression: String,
    val variables: List<Variable>,
    /** Map from target variable symbol to a solver function: (known values map) -> result */
    val solvers: Map<String, (Map<String, Double>) -> Double>,
) {
    fun solveFor(targetSymbol: String, knownValues: Map<String, Double>): Double? {
        return solvers[targetSymbol]?.invoke(knownValues)
    }

    fun generateSteps(targetSymbol: String, knownValues: Map<String, Double>, result: Double): List<String> {
        val steps = mutableListOf<String>()
        steps.add("Formula: $expression")
        val substitution = knownValues.entries.joinToString(", ") { "${it.key} = ${formatNumber(it.value)}" }
        steps.add("Given: $substitution")
        steps.add("Solving for: $targetSymbol")
        steps.add("$targetSymbol = ${formatNumber(result)}")
        return steps
    }
}

private fun formatNumber(d: Double): String {
    return if (d == d.toLong().toDouble()) d.toLong().toString()
    else "%.4f".format(d).trimEnd('0').trimEnd('.')
}

val formulaCatalog: List<Formula> = listOf(
    // Math — Geometry
    Formula(
        id = "area_circle", name = "Area of Circle", subject = Subject.Math, category = "Geometry",
        expression = "A = π × r²",
        variables = listOf(Variable("A", "Area", "units²"), Variable("r", "Radius", "units")),
        solvers = mapOf(
            "A" to { v -> PI * v["r"]!!.pow(2) },
            "r" to { v -> sqrt(v["A"]!! / PI) },
        ),
    ),
    Formula(
        id = "circumference", name = "Circumference", subject = Subject.Math, category = "Geometry",
        expression = "C = 2πr",
        variables = listOf(Variable("C", "Circumference", "units"), Variable("r", "Radius", "units")),
        solvers = mapOf(
            "C" to { v -> 2 * PI * v["r"]!! },
            "r" to { v -> v["C"]!! / (2 * PI) },
        ),
    ),
    Formula(
        id = "area_rectangle", name = "Area of Rectangle", subject = Subject.Math, category = "Geometry",
        expression = "A = l × w",
        variables = listOf(Variable("A", "Area", "units²"), Variable("l", "Length", "units"), Variable("w", "Width", "units")),
        solvers = mapOf(
            "A" to { v -> v["l"]!! * v["w"]!! },
            "l" to { v -> v["A"]!! / v["w"]!! },
            "w" to { v -> v["A"]!! / v["l"]!! },
        ),
    ),
    Formula(
        id = "area_triangle", name = "Area of Triangle", subject = Subject.Math, category = "Geometry",
        expression = "A = ½ × b × h",
        variables = listOf(Variable("A", "Area", "units²"), Variable("b", "Base", "units"), Variable("h", "Height", "units")),
        solvers = mapOf(
            "A" to { v -> 0.5 * v["b"]!! * v["h"]!! },
            "b" to { v -> 2 * v["A"]!! / v["h"]!! },
            "h" to { v -> 2 * v["A"]!! / v["b"]!! },
        ),
    ),
    Formula(
        id = "pythagorean", name = "Pythagorean Theorem", subject = Subject.Math, category = "Geometry",
        expression = "c² = a² + b²",
        variables = listOf(Variable("c", "Hypotenuse"), Variable("a", "Side a"), Variable("b", "Side b")),
        solvers = mapOf(
            "c" to { v -> sqrt(v["a"]!!.pow(2) + v["b"]!!.pow(2)) },
            "a" to { v -> sqrt(v["c"]!!.pow(2) - v["b"]!!.pow(2)) },
            "b" to { v -> sqrt(v["c"]!!.pow(2) - v["a"]!!.pow(2)) },
        ),
    ),
    Formula(
        id = "vol_sphere", name = "Volume of Sphere", subject = Subject.Math, category = "Geometry",
        expression = "V = (4/3)πr³",
        variables = listOf(Variable("V", "Volume", "units³"), Variable("r", "Radius", "units")),
        solvers = mapOf(
            "V" to { v -> (4.0 / 3.0) * PI * v["r"]!!.pow(3) },
            "r" to { v -> (v["V"]!! * 3.0 / (4.0 * PI)).pow(1.0 / 3.0) },
        ),
    ),
    Formula(
        id = "vol_cylinder", name = "Volume of Cylinder", subject = Subject.Math, category = "Geometry",
        expression = "V = πr²h",
        variables = listOf(Variable("V", "Volume", "units³"), Variable("r", "Radius", "units"), Variable("h", "Height", "units")),
        solvers = mapOf(
            "V" to { v -> PI * v["r"]!!.pow(2) * v["h"]!! },
            "r" to { v -> sqrt(v["V"]!! / (PI * v["h"]!!)) },
            "h" to { v -> v["V"]!! / (PI * v["r"]!!.pow(2)) },
        ),
    ),

    // Math — Algebra
    Formula(
        id = "quadratic_disc", name = "Quadratic Discriminant", subject = Subject.Math, category = "Algebra",
        expression = "D = b² − 4ac",
        variables = listOf(Variable("D", "Discriminant"), Variable("a", "Coefficient a"), Variable("b", "Coefficient b"), Variable("c", "Coefficient c")),
        solvers = mapOf(
            "D" to { v -> v["b"]!!.pow(2) - 4 * v["a"]!! * v["c"]!! },
        ),
    ),
    Formula(
        id = "simple_interest", name = "Simple Interest", subject = Subject.Math, category = "Algebra",
        expression = "I = P × R × T / 100",
        variables = listOf(Variable("I", "Interest"), Variable("P", "Principal"), Variable("R", "Rate %"), Variable("T", "Time (years)")),
        solvers = mapOf(
            "I" to { v -> v["P"]!! * v["R"]!! * v["T"]!! / 100 },
            "P" to { v -> v["I"]!! * 100 / (v["R"]!! * v["T"]!!) },
            "R" to { v -> v["I"]!! * 100 / (v["P"]!! * v["T"]!!) },
            "T" to { v -> v["I"]!! * 100 / (v["P"]!! * v["R"]!!) },
        ),
    ),
    Formula(
        id = "percentage", name = "Percentage", subject = Subject.Math, category = "Algebra",
        expression = "P = (Part / Whole) × 100",
        variables = listOf(Variable("P", "Percentage", "%"), Variable("Part", "Part"), Variable("Whole", "Whole")),
        solvers = mapOf(
            "P" to { v -> (v["Part"]!! / v["Whole"]!!) * 100 },
            "Part" to { v -> v["P"]!! * v["Whole"]!! / 100 },
            "Whole" to { v -> v["Part"]!! * 100 / v["P"]!! },
        ),
    ),

    // Math — Trigonometry
    Formula(
        id = "sin_rule", name = "Sine (SOH)", subject = Subject.Math, category = "Trigonometry",
        expression = "sin(θ) = opposite / hypotenuse",
        variables = listOf(Variable("θ", "Angle", "°"), Variable("opp", "Opposite"), Variable("hyp", "Hypotenuse")),
        solvers = mapOf(
            "θ" to { v -> Math.toDegrees(asin(v["opp"]!! / v["hyp"]!!)) },
            "opp" to { v -> v["hyp"]!! * sin(Math.toRadians(v["θ"]!!)) },
            "hyp" to { v -> v["opp"]!! / sin(Math.toRadians(v["θ"]!!)) },
        ),
    ),
    Formula(
        id = "cos_rule", name = "Cosine (CAH)", subject = Subject.Math, category = "Trigonometry",
        expression = "cos(θ) = adjacent / hypotenuse",
        variables = listOf(Variable("θ", "Angle", "°"), Variable("adj", "Adjacent"), Variable("hyp", "Hypotenuse")),
        solvers = mapOf(
            "θ" to { v -> Math.toDegrees(acos(v["adj"]!! / v["hyp"]!!)) },
            "adj" to { v -> v["hyp"]!! * cos(Math.toRadians(v["θ"]!!)) },
            "hyp" to { v -> v["adj"]!! / cos(Math.toRadians(v["θ"]!!)) },
        ),
    ),

    // Physics — Mechanics
    Formula(
        id = "velocity", name = "Velocity", subject = Subject.Physics, category = "Mechanics",
        expression = "v = d / t",
        variables = listOf(Variable("v", "Velocity", "m/s"), Variable("d", "Distance", "m"), Variable("t", "Time", "s")),
        solvers = mapOf(
            "v" to { v -> v["d"]!! / v["t"]!! },
            "d" to { v -> v["v"]!! * v["t"]!! },
            "t" to { v -> v["d"]!! / v["v"]!! },
        ),
    ),
    Formula(
        id = "force", name = "Newton's Second Law", subject = Subject.Physics, category = "Mechanics",
        expression = "F = m × a",
        variables = listOf(Variable("F", "Force", "N"), Variable("m", "Mass", "kg"), Variable("a", "Acceleration", "m/s²")),
        solvers = mapOf(
            "F" to { v -> v["m"]!! * v["a"]!! },
            "m" to { v -> v["F"]!! / v["a"]!! },
            "a" to { v -> v["F"]!! / v["m"]!! },
        ),
    ),
    Formula(
        id = "momentum", name = "Momentum", subject = Subject.Physics, category = "Mechanics",
        expression = "p = m × v",
        variables = listOf(Variable("p", "Momentum", "kg·m/s"), Variable("m", "Mass", "kg"), Variable("v", "Velocity", "m/s")),
        solvers = mapOf(
            "p" to { v -> v["m"]!! * v["v"]!! },
            "m" to { v -> v["p"]!! / v["v"]!! },
            "v" to { v -> v["p"]!! / v["m"]!! },
        ),
    ),
    Formula(
        id = "kinetic_energy", name = "Kinetic Energy", subject = Subject.Physics, category = "Mechanics",
        expression = "KE = ½mv²",
        variables = listOf(Variable("KE", "Kinetic Energy", "J"), Variable("m", "Mass", "kg"), Variable("v", "Velocity", "m/s")),
        solvers = mapOf(
            "KE" to { v -> 0.5 * v["m"]!! * v["v"]!!.pow(2) },
            "m" to { v -> 2 * v["KE"]!! / v["v"]!!.pow(2) },
            "v" to { v -> sqrt(2 * v["KE"]!! / v["m"]!!) },
        ),
    ),
    Formula(
        id = "potential_energy", name = "Potential Energy", subject = Subject.Physics, category = "Mechanics",
        expression = "PE = mgh",
        variables = listOf(Variable("PE", "Potential Energy", "J"), Variable("m", "Mass", "kg"), Variable("g", "Gravity", "m/s²"), Variable("h", "Height", "m")),
        solvers = mapOf(
            "PE" to { v -> v["m"]!! * v["g"]!! * v["h"]!! },
            "m" to { v -> v["PE"]!! / (v["g"]!! * v["h"]!!) },
            "h" to { v -> v["PE"]!! / (v["m"]!! * v["g"]!!) },
        ),
    ),
    Formula(
        id = "work", name = "Work", subject = Subject.Physics, category = "Mechanics",
        expression = "W = F × d",
        variables = listOf(Variable("W", "Work", "J"), Variable("F", "Force", "N"), Variable("d", "Distance", "m")),
        solvers = mapOf(
            "W" to { v -> v["F"]!! * v["d"]!! },
            "F" to { v -> v["W"]!! / v["d"]!! },
            "d" to { v -> v["W"]!! / v["F"]!! },
        ),
    ),
    Formula(
        id = "power", name = "Power", subject = Subject.Physics, category = "Mechanics",
        expression = "P = W / t",
        variables = listOf(Variable("P", "Power", "W"), Variable("W", "Work", "J"), Variable("t", "Time", "s")),
        solvers = mapOf(
            "P" to { v -> v["W"]!! / v["t"]!! },
            "W" to { v -> v["P"]!! * v["t"]!! },
            "t" to { v -> v["W"]!! / v["P"]!! },
        ),
    ),
    Formula(
        id = "density", name = "Density", subject = Subject.Physics, category = "Mechanics",
        expression = "ρ = m / V",
        variables = listOf(Variable("ρ", "Density", "kg/m³"), Variable("m", "Mass", "kg"), Variable("V", "Volume", "m³")),
        solvers = mapOf(
            "ρ" to { v -> v["m"]!! / v["V"]!! },
            "m" to { v -> v["ρ"]!! * v["V"]!! },
            "V" to { v -> v["m"]!! / v["ρ"]!! },
        ),
    ),
    Formula(
        id = "pressure", name = "Pressure", subject = Subject.Physics, category = "Mechanics",
        expression = "P = F / A",
        variables = listOf(Variable("P", "Pressure", "Pa"), Variable("F", "Force", "N"), Variable("A", "Area", "m²")),
        solvers = mapOf(
            "P" to { v -> v["F"]!! / v["A"]!! },
            "F" to { v -> v["P"]!! * v["A"]!! },
            "A" to { v -> v["F"]!! / v["P"]!! },
        ),
    ),

    // Physics — Electricity
    Formula(
        id = "ohms_law", name = "Ohm's Law", subject = Subject.Physics, category = "Electricity",
        expression = "V = I × R",
        variables = listOf(Variable("V", "Voltage", "V"), Variable("I", "Current", "A"), Variable("R", "Resistance", "Ω")),
        solvers = mapOf(
            "V" to { v -> v["I"]!! * v["R"]!! },
            "I" to { v -> v["V"]!! / v["R"]!! },
            "R" to { v -> v["V"]!! / v["I"]!! },
        ),
    ),
    Formula(
        id = "electrical_power", name = "Electrical Power", subject = Subject.Physics, category = "Electricity",
        expression = "P = V × I",
        variables = listOf(Variable("P", "Power", "W"), Variable("V", "Voltage", "V"), Variable("I", "Current", "A")),
        solvers = mapOf(
            "P" to { v -> v["V"]!! * v["I"]!! },
            "V" to { v -> v["P"]!! / v["I"]!! },
            "I" to { v -> v["P"]!! / v["V"]!! },
        ),
    ),

    // Physics — Optics
    Formula(
        id = "snells_law", name = "Snell's Law", subject = Subject.Physics, category = "Optics",
        expression = "n₁ × sin(θ₁) = n₂ × sin(θ₂)",
        variables = listOf(Variable("n₁", "Refractive index 1"), Variable("θ₁", "Angle 1", "°"), Variable("n₂", "Refractive index 2"), Variable("θ₂", "Angle 2", "°")),
        solvers = mapOf(
            "θ₂" to { v -> Math.toDegrees(asin(v["n₁"]!! * sin(Math.toRadians(v["θ₁"]!!)) / v["n₂"]!!)) },
            "θ₁" to { v -> Math.toDegrees(asin(v["n₂"]!! * sin(Math.toRadians(v["θ₂"]!!)) / v["n₁"]!!)) },
        ),
    ),
    Formula(
        id = "lens_eq", name = "Thin Lens Equation", subject = Subject.Physics, category = "Optics",
        expression = "1/f = 1/do + 1/di",
        variables = listOf(Variable("f", "Focal length", "cm"), Variable("do", "Object dist", "cm"), Variable("di", "Image dist", "cm")),
        solvers = mapOf(
            "f" to { v -> 1.0 / (1.0 / v["do"]!! + 1.0 / v["di"]!!) },
            "do" to { v -> 1.0 / (1.0 / v["f"]!! - 1.0 / v["di"]!!) },
            "di" to { v -> 1.0 / (1.0 / v["f"]!! - 1.0 / v["do"]!!) },
        ),
    ),

    // Physics — Thermodynamics
    Formula(
        id = "heat_transfer", name = "Heat Transfer", subject = Subject.Physics, category = "Thermodynamics",
        expression = "Q = mcΔT",
        variables = listOf(Variable("Q", "Heat", "J"), Variable("m", "Mass", "kg"), Variable("c", "Specific heat", "J/(kg·K)"), Variable("ΔT", "Temp change", "K")),
        solvers = mapOf(
            "Q" to { v -> v["m"]!! * v["c"]!! * v["ΔT"]!! },
            "m" to { v -> v["Q"]!! / (v["c"]!! * v["ΔT"]!!) },
            "c" to { v -> v["Q"]!! / (v["m"]!! * v["ΔT"]!!) },
            "ΔT" to { v -> v["Q"]!! / (v["m"]!! * v["c"]!!) },
        ),
    ),

    // Chemistry — Gas Laws
    Formula(
        id = "ideal_gas", name = "Ideal Gas Law", subject = Subject.Chemistry, category = "Gas Laws",
        expression = "PV = nRT",
        variables = listOf(Variable("P", "Pressure", "Pa"), Variable("V", "Volume", "m³"), Variable("n", "Moles", "mol"), Variable("R", "Gas constant (8.314)", "J/(mol·K)"), Variable("T", "Temperature", "K")),
        solvers = mapOf(
            "P" to { v -> v["n"]!! * v["R"]!! * v["T"]!! / v["V"]!! },
            "V" to { v -> v["n"]!! * v["R"]!! * v["T"]!! / v["P"]!! },
            "n" to { v -> v["P"]!! * v["V"]!! / (v["R"]!! * v["T"]!!) },
            "T" to { v -> v["P"]!! * v["V"]!! / (v["n"]!! * v["R"]!!) },
        ),
    ),
    Formula(
        id = "boyles_law", name = "Boyle's Law", subject = Subject.Chemistry, category = "Gas Laws",
        expression = "P₁V₁ = P₂V₂",
        variables = listOf(Variable("P₁", "Pressure 1", "Pa"), Variable("V₁", "Volume 1", "m³"), Variable("P₂", "Pressure 2", "Pa"), Variable("V₂", "Volume 2", "m³")),
        solvers = mapOf(
            "P₂" to { v -> v["P₁"]!! * v["V₁"]!! / v["V₂"]!! },
            "V₂" to { v -> v["P₁"]!! * v["V₁"]!! / v["P₂"]!! },
            "P₁" to { v -> v["P₂"]!! * v["V₂"]!! / v["V₁"]!! },
            "V₁" to { v -> v["P₂"]!! * v["V₂"]!! / v["P₁"]!! },
        ),
    ),

    // Chemistry — Solutions
    Formula(
        id = "molarity", name = "Molarity", subject = Subject.Chemistry, category = "Solutions",
        expression = "M = n / V",
        variables = listOf(Variable("M", "Molarity", "mol/L"), Variable("n", "Moles", "mol"), Variable("V", "Volume", "L")),
        solvers = mapOf(
            "M" to { v -> v["n"]!! / v["V"]!! },
            "n" to { v -> v["M"]!! * v["V"]!! },
            "V" to { v -> v["n"]!! / v["M"]!! },
        ),
    ),
    Formula(
        id = "dilution", name = "Dilution", subject = Subject.Chemistry, category = "Solutions",
        expression = "M₁V₁ = M₂V₂",
        variables = listOf(Variable("M₁", "Conc. 1", "mol/L"), Variable("V₁", "Volume 1", "L"), Variable("M₂", "Conc. 2", "mol/L"), Variable("V₂", "Volume 2", "L")),
        solvers = mapOf(
            "M₂" to { v -> v["M₁"]!! * v["V₁"]!! / v["V₂"]!! },
            "V₂" to { v -> v["M₁"]!! * v["V₁"]!! / v["M₂"]!! },
            "M₁" to { v -> v["M₂"]!! * v["V₂"]!! / v["V₁"]!! },
            "V₁" to { v -> v["M₂"]!! * v["V₂"]!! / v["M₁"]!! },
        ),
    ),

    // Finance
    Formula(
        id = "compound_interest", name = "Compound Interest", subject = Subject.Finance, category = "Interest",
        expression = "A = P(1 + r/n)^(nt)",
        variables = listOf(Variable("A", "Final Amount"), Variable("P", "Principal"), Variable("r", "Annual Rate (decimal)"), Variable("n", "Compounds/year"), Variable("t", "Time (years)")),
        solvers = mapOf(
            "A" to { v -> v["P"]!! * (1 + v["r"]!! / v["n"]!!).pow(v["n"]!! * v["t"]!!) },
            "P" to { v -> v["A"]!! / (1 + v["r"]!! / v["n"]!!).pow(v["n"]!! * v["t"]!!) },
        ),
    ),
    Formula(
        id = "roi", name = "Return on Investment", subject = Subject.Finance, category = "Investment",
        expression = "ROI = (Gain − Cost) / Cost × 100",
        variables = listOf(Variable("ROI", "ROI", "%"), Variable("Gain", "Gain"), Variable("Cost", "Cost")),
        solvers = mapOf(
            "ROI" to { v -> (v["Gain"]!! - v["Cost"]!!) / v["Cost"]!! * 100 },
            "Gain" to { v -> v["Cost"]!! * (1 + v["ROI"]!! / 100) },
            "Cost" to { v -> v["Gain"]!! / (1 + v["ROI"]!! / 100) },
        ),
    ),
    Formula(
        id = "break_even", name = "Break-Even Point", subject = Subject.Finance, category = "Business",
        expression = "BEP = Fixed Costs / (Price − Variable Cost)",
        variables = listOf(Variable("BEP", "Break-Even Units"), Variable("FC", "Fixed Costs"), Variable("Price", "Price per Unit"), Variable("VC", "Variable Cost per Unit")),
        solvers = mapOf(
            "BEP" to { v -> v["FC"]!! / (v["Price"]!! - v["VC"]!!) },
            "FC" to { v -> v["BEP"]!! * (v["Price"]!! - v["VC"]!!) },
        ),
    ),
    Formula(
        id = "profit_margin", name = "Profit Margin", subject = Subject.Finance, category = "Business",
        expression = "Margin = (Revenue − Cost) / Revenue × 100",
        variables = listOf(Variable("Margin", "Profit Margin", "%"), Variable("Revenue", "Revenue"), Variable("Cost", "Cost")),
        solvers = mapOf(
            "Margin" to { v -> (v["Revenue"]!! - v["Cost"]!!) / v["Revenue"]!! * 100 },
            "Revenue" to { v -> v["Cost"]!! / (1 - v["Margin"]!! / 100) },
            "Cost" to { v -> v["Revenue"]!! * (1 - v["Margin"]!! / 100) },
        ),
    ),
)
