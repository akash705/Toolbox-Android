package com.toolbox.conversion.calculator

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.PI
import kotlin.math.E
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

data class CalculatorUiState(
    val expression: String = "",
    val result: String = "",
    val useDegrees: Boolean = true,
    val history: List<Pair<String, String>> = emptyList(),
)

class CalculatorViewModel : ViewModel() {

    private val _state = MutableStateFlow(CalculatorUiState())
    val state: StateFlow<CalculatorUiState> = _state.asStateFlow()

    fun onInput(token: String) {
        _state.update { it.copy(expression = it.expression + token) }
        tryEvaluate()
    }

    fun onClear() {
        _state.update { it.copy(expression = "", result = "") }
    }

    fun onBackspace() {
        _state.update {
            val expr = it.expression
            if (expr.isEmpty()) return@update it
            // Remove trailing function name if present
            val functions = listOf("sin(", "cos(", "tan(", "asin(", "acos(", "atan(", "ln(", "log(", "sqrt(", "abs(")
            val matchedFunc = functions.firstOrNull { f -> expr.endsWith(f) }
            val newExpr = if (matchedFunc != null) {
                expr.dropLast(matchedFunc.length)
            } else {
                expr.dropLast(1)
            }
            it.copy(expression = newExpr)
        }
        tryEvaluate()
    }

    fun onEquals() {
        val current = _state.value
        if (current.result.isNotEmpty() && !current.result.startsWith("Error")) {
            _state.update {
                it.copy(
                    history = (listOf(it.expression to it.result) + it.history).take(20),
                    expression = it.result,
                    result = "",
                )
            }
        }
    }

    fun onNegate() {
        _state.update {
            val expr = it.expression
            if (expr.isEmpty()) return@update it
            if (expr.startsWith("-")) {
                it.copy(expression = expr.drop(1))
            } else {
                it.copy(expression = "-$expr")
            }
        }
        tryEvaluate()
    }

    fun toggleAngleMode() {
        _state.update { it.copy(useDegrees = !it.useDegrees) }
        tryEvaluate()
    }

    fun onHistorySelect(expression: String) {
        _state.update { it.copy(expression = expression) }
        tryEvaluate()
    }

    private fun tryEvaluate() {
        val expr = _state.value.expression
        if (expr.isBlank()) {
            _state.update { it.copy(result = "") }
            return
        }
        try {
            val result = evaluate(expr, _state.value.useDegrees)
            val formatted = if (result == result.toLong().toDouble()) {
                result.toLong().toString()
            } else {
                // Show up to 10 decimal places, trim trailing zeros
                "%.10f".format(result).trimEnd('0').trimEnd('.')
            }
            _state.update { it.copy(result = formatted) }
        } catch (_: Exception) {
            _state.update { it.copy(result = "") }
        }
    }

    private fun evaluate(expr: String, useDegrees: Boolean): Double {
        val parser = ExpressionParser(expr, useDegrees)
        return parser.parse()
    }
}

/**
 * Recursive descent parser for mathematical expressions.
 * Supports: +, -, *, /, ^, %, parentheses, and scientific functions.
 */
private class ExpressionParser(private val input: String, private val useDegrees: Boolean) {
    private var pos = 0

    fun parse(): Double {
        val result = parseExpression()
        if (pos < input.length) throw IllegalArgumentException("Unexpected: ${input[pos]}")
        return result
    }

    private fun parseExpression(): Double {
        var result = parseTerm()
        while (pos < input.length) {
            when {
                peek('+') -> { advance(); result += parseTerm() }
                peek('-') -> { advance(); result -= parseTerm() }
                else -> break
            }
        }
        return result
    }

    private fun parseTerm(): Double {
        var result = parseExponent()
        while (pos < input.length) {
            when {
                peek('×') || peek('*') -> { advance(); result *= parseExponent() }
                peek('÷') || peek('/') -> { advance(); result /= parseExponent() }
                peek('%') -> { advance(); result %= parseExponent() }
                else -> break
            }
        }
        return result
    }

    private fun parseExponent(): Double {
        var result = parseUnary()
        while (pos < input.length && peek('^')) {
            advance()
            result = result.pow(parseUnary())
        }
        return result
    }

    private fun parseUnary(): Double {
        if (peek('-')) {
            advance()
            return -parsePrimary()
        }
        if (peek('+')) {
            advance()
        }
        return parsePrimary()
    }

    private fun parsePrimary(): Double {
        // Functions
        for (func in listOf("asin", "acos", "atan", "sin", "cos", "tan", "ln", "log", "sqrt", "abs")) {
            if (matchString(func)) {
                expect('(')
                val arg = parseExpression()
                expect(')')
                return applyFunction(func, arg)
            }
        }

        // Factorial suffix handled after number/constant parsing

        // Constants
        if (matchString("π") || matchString("pi")) return PI
        if (matchString("e") && !peekDigit()) return E

        // Parenthesized expression
        if (peek('(')) {
            advance()
            val result = parseExpression()
            expect(')')
            return maybeFactorial(result)
        }

        // Number
        val start = pos
        while (pos < input.length && (input[pos].isDigit() || input[pos] == '.')) pos++
        if (pos == start) throw IllegalArgumentException("Expected number at position $pos")
        val number = input.substring(start, pos).toDouble()
        return maybeFactorial(number)
    }

    private fun maybeFactorial(value: Double): Double {
        if (pos < input.length && input[pos] == '!') {
            advance()
            return factorial(value.toLong()).toDouble()
        }
        return value
    }

    private fun applyFunction(name: String, arg: Double): Double {
        return when (name) {
            "sin" -> sin(toRadians(arg))
            "cos" -> cos(toRadians(arg))
            "tan" -> tan(toRadians(arg))
            "asin" -> fromRadians(asin(arg))
            "acos" -> fromRadians(acos(arg))
            "atan" -> fromRadians(atan(arg))
            "ln" -> ln(arg)
            "log" -> log10(arg)
            "sqrt" -> sqrt(arg)
            "abs" -> abs(arg)
            else -> throw IllegalArgumentException("Unknown function: $name")
        }
    }

    private fun toRadians(value: Double): Double =
        if (useDegrees) Math.toRadians(value) else value

    private fun fromRadians(value: Double): Double =
        if (useDegrees) Math.toDegrees(value) else value

    private fun factorial(n: Long): Long {
        if (n < 0) throw IllegalArgumentException("Negative factorial")
        if (n <= 1) return 1
        var result = 1L
        for (i in 2..n.coerceAtMost(20)) result *= i
        return result
    }

    private fun peek(c: Char): Boolean = pos < input.length && input[pos] == c
    private fun peekDigit(): Boolean = pos < input.length && input[pos].isDigit()
    private fun advance() { pos++ }
    private fun expect(c: Char) {
        if (!peek(c)) throw IllegalArgumentException("Expected '$c'")
        advance()
    }

    private fun matchString(s: String): Boolean {
        if (input.startsWith(s, pos)) {
            pos += s.length
            return true
        }
        return false
    }
}
