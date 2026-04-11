package com.toolbox.everyday.counter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.toolbox.core.persistence.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

private val Application.counterDataStore: DataStore<Preferences> by preferencesDataStore(name = "counters")
private val COUNTERS_KEY = stringPreferencesKey("counters_json")

@Serializable
data class CounterData(val name: String, val value: Int = 0)

data class CounterUiState(
    val counters: List<CounterData> = listOf(CounterData("Counter 1")),
    val activeIndex: Int = 0,
    val showResetDialog: Boolean = false,
    val showAddDialog: Boolean = false,
) {
    val activeCounter: CounterData get() = counters[activeIndex]
}

class CounterViewModel(private val application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(CounterUiState())
    val state: StateFlow<CounterUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            application.counterDataStore.data.collect { prefs ->
                val json = prefs[COUNTERS_KEY]
                if (json != null) {
                    try {
                        val counters = Json.decodeFromString<List<CounterData>>(json)
                        if (counters.isNotEmpty()) {
                            _state.update { it.copy(counters = counters) }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    fun increment() {
        updateActiveCounter { it.copy(value = it.value + 1) }
    }

    fun decrement() {
        updateActiveCounter { it.copy(value = maxOf(0, it.value - 1)) }
    }

    fun showResetDialog() { _state.update { it.copy(showResetDialog = true) } }
    fun dismissResetDialog() { _state.update { it.copy(showResetDialog = false) } }

    fun confirmReset() {
        updateActiveCounter { it.copy(value = 0) }
        _state.update { it.copy(showResetDialog = false) }
    }

    fun showAddDialog() { _state.update { it.copy(showAddDialog = true) } }
    fun dismissAddDialog() { _state.update { it.copy(showAddDialog = false) } }

    fun addCounter(name: String) {
        _state.update { s ->
            val newCounters = s.counters + CounterData(name)
            s.copy(
                counters = newCounters,
                activeIndex = newCounters.lastIndex,
                showAddDialog = false,
            )
        }
        persist()
    }

    fun selectCounter(index: Int) {
        _state.update { it.copy(activeIndex = index.coerceIn(0, it.counters.lastIndex)) }
    }

    private fun updateActiveCounter(transform: (CounterData) -> CounterData) {
        _state.update { s ->
            val updated = s.counters.toMutableList()
            updated[s.activeIndex] = transform(updated[s.activeIndex])
            s.copy(counters = updated)
        }
        persist()
    }

    private fun persist() {
        viewModelScope.launch {
            application.counterDataStore.edit { prefs ->
                prefs[COUNTERS_KEY] = Json.encodeToString(_state.value.counters)
            }
        }
    }
}
