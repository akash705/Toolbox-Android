package com.toolbox.conversion.formula

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.formulaPrefs by preferencesDataStore("formula_prefs")
private val FAVORITES_KEY = stringSetPreferencesKey("favorite_formula_ids")

class FormulaViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.formulaPrefs

    val favoriteIds: StateFlow<Set<String>> = prefs.data
        .map { it[FAVORITES_KEY] ?: emptySet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val searchQuery = MutableStateFlow("")
    val selectedSubject = MutableStateFlow<Subject?>(null)
    val selectedFormula = MutableStateFlow<Formula?>(null)

    val filteredFormulas: StateFlow<List<Formula>> = combine(
        searchQuery, selectedSubject,
    ) { query, subject ->
        formulaCatalog.filter { formula ->
            val matchesSubject = subject == null || formula.subject == subject
            val matchesQuery = query.isBlank() ||
                formula.name.contains(query, ignoreCase = true) ||
                formula.expression.contains(query, ignoreCase = true) ||
                formula.category.contains(query, ignoreCase = true)
            matchesSubject && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), formulaCatalog)

    fun toggleFavorite(formulaId: String) {
        viewModelScope.launch {
            prefs.edit { prefs ->
                val current = prefs[FAVORITES_KEY] ?: emptySet()
                prefs[FAVORITES_KEY] = if (formulaId in current) current - formulaId else current + formulaId
            }
        }
    }

    fun selectFormula(formula: Formula?) {
        selectedFormula.value = formula
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setSubject(subject: Subject?) {
        selectedSubject.value = subject
    }
}
