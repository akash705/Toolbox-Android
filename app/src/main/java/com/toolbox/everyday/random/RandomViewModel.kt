package com.toolbox.everyday.random

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

enum class RandomMode { Number, Coin, Dice }

data class RandomUiState(
    val mode: RandomMode = RandomMode.Number,
    val min: String = "1",
    val max: String = "100",
    val numberResult: Int? = null,
    val coinResult: Boolean? = null, // true = heads
    val diceCount: Int = 1,
    val diceResults: List<Int> = emptyList(),
    val isAnimating: Boolean = false,
)

class RandomViewModel : ViewModel() {

    private val _state = MutableStateFlow(RandomUiState())
    val state: StateFlow<RandomUiState> = _state.asStateFlow()

    fun setMode(mode: RandomMode) {
        _state.update { it.copy(mode = mode) }
    }

    fun setMin(value: String) { _state.update { it.copy(min = value) } }
    fun setMax(value: String) { _state.update { it.copy(max = value) } }
    fun setDiceCount(count: Int) {
        _state.update { it.copy(diceCount = count.coerceIn(1, 6)) }
    }

    fun generateNumber() {
        val min = _state.value.min.toIntOrNull() ?: 1
        val max = _state.value.max.toIntOrNull() ?: 100
        val (lo, hi) = if (min <= max) min to max else max to min
        _state.update { it.copy(numberResult = Random.nextInt(lo, hi + 1)) }
    }

    fun flipCoin() {
        _state.update { it.copy(coinResult = Random.nextBoolean()) }
    }

    fun rollDice() {
        val results = List(_state.value.diceCount) { Random.nextInt(1, 7) }
        _state.update { it.copy(diceResults = results) }
    }
}
