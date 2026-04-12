package com.toolbox.everyday.morse

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.sin

private val CHAR_TO_MORSE = mapOf(
    'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".",
    'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---",
    'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---",
    'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
    'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--",
    'Z' to "--..",
    '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--",
    '4' to "....-", '5' to ".....", '6' to "-....", '7' to "--...",
    '8' to "---..", '9' to "----.",
    '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '!' to "-.-.--",
    '/' to "-..-.", '(' to "-.--.", ')' to "-.--.-", '&' to ".-...",
    ':' to "---...", ';' to "-.-.-.", '=' to "-...-", '+' to ".-.-.",
    '-' to "-....-", '"' to ".-..-.", '\'' to ".----.", '@' to ".--.-.",
)

private val MORSE_TO_CHAR = CHAR_TO_MORSE.entries.associate { (k, v) -> v to k }

enum class MorseTab { TextToMorse, MorseToText }

data class MorseUiState(
    val tab: MorseTab = MorseTab.TextToMorse,
    val textInput: String = "",
    val morseOutput: String = "",
    val morseInput: String = "",
    val textOutput: String = "",
    val soundEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val flashEnabled: Boolean = false,
    val wpm: Int = 15,
    val isPlaying: Boolean = false,
)

class MorseCodeViewModel : ViewModel() {

    private val _state = MutableStateFlow(MorseUiState())
    val state: StateFlow<MorseUiState> = _state.asStateFlow()

    private var playJob: Job? = null

    fun setTab(tab: MorseTab) {
        _state.update { it.copy(tab = tab) }
    }

    fun setTextInput(text: String) {
        val morse = textToMorse(text)
        _state.update { it.copy(textInput = text, morseOutput = morse) }
    }

    fun setMorseInput(morse: String) {
        val decoded = morseToText(morse)
        _state.update { it.copy(morseInput = morse, textOutput = decoded) }
    }

    fun appendMorseSymbol(symbol: String) {
        val newMorse = _state.value.morseInput + symbol
        val decoded = morseToText(newMorse)
        _state.update { it.copy(morseInput = newMorse, textOutput = decoded) }
    }

    fun clearMorseInput() {
        _state.update { it.copy(morseInput = "", textOutput = "") }
    }

    fun backspaceMorseInput() {
        val current = _state.value.morseInput
        if (current.isEmpty()) return
        val newMorse = current.dropLast(1)
        val decoded = morseToText(newMorse)
        _state.update { it.copy(morseInput = newMorse, textOutput = decoded) }
    }

    fun toggleSound() { _state.update { it.copy(soundEnabled = !it.soundEnabled) } }
    fun toggleHaptic() { _state.update { it.copy(hapticEnabled = !it.hapticEnabled) } }
    fun toggleFlash() { _state.update { it.copy(flashEnabled = !it.flashEnabled) } }

    fun setWpm(wpm: Int) {
        _state.update { it.copy(wpm = wpm.coerceIn(5, 25)) }
    }

    fun togglePlayback() {
        if (_state.value.isPlaying) {
            stopPlayback()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        val morse = _state.value.morseOutput
        if (morse.isBlank()) return

        _state.update { it.copy(isPlaying = true) }

        playJob = viewModelScope.launch {
            val unitMs = 1200L / _state.value.wpm // PARIS standard

            for (char in morse) {
                if (!_state.value.isPlaying) break
                when (char) {
                    '.' -> {
                        if (_state.value.soundEnabled) playTone(unitMs)
                        else delay(unitMs)
                        delay(unitMs) // inter-element gap
                    }
                    '-' -> {
                        if (_state.value.soundEnabled) playTone(unitMs * 3)
                        else delay(unitMs * 3)
                        delay(unitMs) // inter-element gap
                    }
                    ' ' -> {
                        // Could be letter gap (3 units) or word gap (7 units)
                        // In our encoding, single space = letter gap, double handled by next space
                        delay(unitMs * 2) // additional 2 units (1 already from element gap = 3 total)
                    }
                    '/' -> {
                        delay(unitMs * 4) // word gap remainder (3 from spaces + 4 = 7)
                    }
                }
            }

            _state.update { it.copy(isPlaying = false) }
        }
    }

    private fun stopPlayback() {
        playJob?.cancel()
        playJob = null
        _state.update { it.copy(isPlaying = false) }
    }

    private fun playTone(durationMs: Long) {
        val sampleRate = 44100
        val numSamples = (sampleRate * durationMs / 1000).toInt()
        val buffer = ShortArray(numSamples)
        val frequency = 600.0 // Hz

        for (i in 0 until numSamples) {
            val angle = 2.0 * Math.PI * i * frequency / sampleRate
            buffer[i] = (sin(angle) * Short.MAX_VALUE * 0.8).toInt().toShort()
        }

        // Apply fade in/out to avoid clicks
        val fadeLen = minOf(numSamples / 10, 200)
        for (i in 0 until fadeLen) {
            val factor = i.toFloat() / fadeLen
            buffer[i] = (buffer[i] * factor).toInt().toShort()
            buffer[numSamples - 1 - i] = (buffer[numSamples - 1 - i] * factor).toInt().toShort()
        }

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(buffer, 0, buffer.size)
        audioTrack.play()

        // Wait for playback to complete
        Thread.sleep(durationMs)
        audioTrack.stop()
        audioTrack.release()
    }

    override fun onCleared() {
        super.onCleared()
        playJob?.cancel()
    }

    companion object {
        fun textToMorse(text: String): String {
            return text.uppercase().map { char ->
                when {
                    char == ' ' -> "/"
                    CHAR_TO_MORSE.containsKey(char) -> CHAR_TO_MORSE[char]!!
                    else -> ""
                }
            }.filter { it.isNotEmpty() }.joinToString(" ")
        }

        fun morseToText(morse: String): String {
            return morse.split(" / ", "/").joinToString(" ") { word ->
                word.trim().split(" ").mapNotNull { code ->
                    if (code.isBlank()) null
                    else MORSE_TO_CHAR[code]?.toString() ?: "?"
                }.joinToString("")
            }
        }
    }
}
