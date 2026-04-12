package com.toolbox.everyday.breathing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BreathingTechnique(
    val label: String,
    val inhaleSec: Int,
    val holdAfterInhaleSec: Int,
    val exhaleSec: Int,
    val holdAfterExhaleSec: Int,
) {
    BoxBreathing("Box Breathing", 4, 4, 4, 4),
    Breathing478("4-7-8", 4, 7, 8, 0),
    Relaxing("Relaxing", 4, 0, 6, 0),
}

enum class BreathingPhase(val label: String) {
    Inhale("Inhale"),
    HoldIn("Hold"),
    Exhale("Exhale"),
    HoldOut("Hold"),
    Idle("Ready"),
}

data class BreathingUiState(
    val technique: BreathingTechnique = BreathingTechnique.BoxBreathing,
    val isRunning: Boolean = false,
    val phase: BreathingPhase = BreathingPhase.Idle,
    val secondsRemaining: Int = 0,
    val phaseDuration: Int = 0,
    val currentCycle: Int = 0,
    val totalCycles: Int = 5,
    val soundEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val isComplete: Boolean = false,
)

class BreathingExerciseViewModel : ViewModel() {

    private val _state = MutableStateFlow(BreathingUiState())
    val state: StateFlow<BreathingUiState> = _state.asStateFlow()

    private var breathingJob: Job? = null

    fun setTechnique(technique: BreathingTechnique) {
        if (_state.value.isRunning) return
        _state.update { it.copy(technique = technique) }
    }

    fun setCycles(cycles: Int) {
        if (_state.value.isRunning) return
        _state.update { it.copy(totalCycles = cycles.coerceIn(1, 20)) }
    }

    fun toggleSound() {
        _state.update { it.copy(soundEnabled = !it.soundEnabled) }
    }

    fun toggleHaptic() {
        _state.update { it.copy(hapticEnabled = !it.hapticEnabled) }
    }

    fun toggleRunning() {
        if (_state.value.isRunning) {
            stop()
        } else {
            start()
        }
    }

    private fun start() {
        _state.update {
            it.copy(
                isRunning = true,
                isComplete = false,
                currentCycle = 1,
                phase = BreathingPhase.Idle,
            )
        }
        breathingJob = viewModelScope.launch {
            val technique = _state.value.technique
            val totalCycles = _state.value.totalCycles

            for (cycle in 1..totalCycles) {
                _state.update { it.copy(currentCycle = cycle) }

                // Inhale
                runPhase(BreathingPhase.Inhale, technique.inhaleSec)
                if (!_state.value.isRunning) return@launch

                // Hold after inhale
                if (technique.holdAfterInhaleSec > 0) {
                    runPhase(BreathingPhase.HoldIn, technique.holdAfterInhaleSec)
                    if (!_state.value.isRunning) return@launch
                }

                // Exhale
                runPhase(BreathingPhase.Exhale, technique.exhaleSec)
                if (!_state.value.isRunning) return@launch

                // Hold after exhale
                if (technique.holdAfterExhaleSec > 0) {
                    runPhase(BreathingPhase.HoldOut, technique.holdAfterExhaleSec)
                    if (!_state.value.isRunning) return@launch
                }
            }

            _state.update {
                it.copy(
                    isRunning = false,
                    isComplete = true,
                    phase = BreathingPhase.Idle,
                )
            }
        }
    }

    private suspend fun runPhase(phase: BreathingPhase, durationSec: Int) {
        _state.update {
            it.copy(
                phase = phase,
                phaseDuration = durationSec,
                secondsRemaining = durationSec,
            )
        }
        for (s in durationSec downTo 1) {
            _state.update { it.copy(secondsRemaining = s) }
            delay(1000L)
            if (!_state.value.isRunning) return
        }
    }

    private fun stop() {
        breathingJob?.cancel()
        breathingJob = null
        _state.update {
            it.copy(
                isRunning = false,
                phase = BreathingPhase.Idle,
                secondsRemaining = 0,
                currentCycle = 0,
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        breathingJob?.cancel()
    }
}
