package com.toolbox.conversion.formula

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FormulaScreen(viewModel: FormulaViewModel = viewModel()) {
    val formulas by viewModel.filteredFormulas.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedSubject by viewModel.selectedSubject.collectAsState()
    val selectedFormula by viewModel.selectedFormula.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            label = { Text("Search formulas") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subject chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selectedSubject == null,
                onClick = { viewModel.setSubject(null) },
                label = { Text("All") },
            )
            Subject.entries.forEach { subject ->
                FilterChip(
                    selected = selectedSubject == subject,
                    onClick = {
                        viewModel.setSubject(if (selectedSubject == subject) null else subject)
                    },
                    label = { Text(subject.label) },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (formulas.isEmpty()) {
            Text(
                "No formulas found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }

        // Formula list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Group by category
            val grouped = formulas.groupBy { "${it.subject.label} — ${it.category}" }
            grouped.forEach { (group, groupFormulas) ->
                item(key = "header_$group") {
                    Text(
                        group,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                }
                items(groupFormulas, key = { it.id }) { formula ->
                    FormulaCard(
                        formula = formula,
                        isFavorite = formula.id in favoriteIds,
                        isExpanded = selectedFormula?.id == formula.id,
                        onToggleFavorite = { viewModel.toggleFavorite(formula.id) },
                        onClick = {
                            viewModel.selectFormula(
                                if (selectedFormula?.id == formula.id) null else formula,
                            )
                        },
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun FormulaCard(
    formula: Formula,
    isFavorite: Boolean,
    isExpanded: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        formula.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        formula.expression,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                SolverSection(formula)
            }
        }
    }
}

@Composable
private fun SolverSection(formula: Formula) {
    val inputValues = remember(formula.id) { mutableStateMapOf<String, String>() }
    var solveResult by remember(formula.id) { mutableStateOf<Pair<String, List<String>>?>(null) }
    var errorMessage by remember(formula.id) { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.padding(top = 12.dp)) {
        HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

        Text(
            "Solver — enter known values, leave one empty",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Input fields for each variable
        formula.variables.forEach { variable ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${variable.symbol} (${variable.name})",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(120.dp),
                )
                OutlinedTextField(
                    value = inputValues[variable.symbol] ?: "",
                    onValueChange = {
                        inputValues[variable.symbol] = it
                        solveResult = null
                        errorMessage = null
                    },
                    placeholder = {
                        Text(
                            if (variable.unit.isNotEmpty()) variable.unit else "value",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                errorMessage = null
                solveResult = null

                val known = mutableMapOf<String, Double>()
                var unknownSymbol: String? = null
                var multipleUnknowns = false

                for (variable in formula.variables) {
                    val text = inputValues[variable.symbol]?.trim() ?: ""
                    if (text.isEmpty()) {
                        if (unknownSymbol != null) {
                            multipleUnknowns = true
                            break
                        }
                        unknownSymbol = variable.symbol
                    } else {
                        val value = text.toDoubleOrNull()
                        if (value == null) {
                            errorMessage = "Invalid number for ${variable.name}"
                            return@OutlinedButton
                        }
                        known[variable.symbol] = value
                    }
                }

                if (multipleUnknowns) {
                    errorMessage = "Leave exactly one field empty to solve"
                    return@OutlinedButton
                }

                if (unknownSymbol == null) {
                    errorMessage = "Leave one field empty to solve for it"
                    return@OutlinedButton
                }

                if (formula.solvers[unknownSymbol] == null) {
                    errorMessage = "Cannot solve for $unknownSymbol in this formula"
                    return@OutlinedButton
                }

                try {
                    val result = formula.solveFor(unknownSymbol, known)
                    if (result == null || result.isNaN() || result.isInfinite()) {
                        errorMessage = "Cannot compute — check for division by zero or invalid input"
                    } else {
                        val steps = formula.generateSteps(unknownSymbol, known, result)
                        solveResult = unknownSymbol to steps
                        inputValues[unknownSymbol] = formatResult(result)
                    }
                } catch (_: Exception) {
                    errorMessage = "Calculation error — check your inputs"
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Solve")
        }

        // Error
        errorMessage?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        // Steps
        solveResult?.let { (target, steps) ->
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Solution Steps",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    steps.forEach { step ->
                        Text(
                            step,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun formatResult(d: Double): String {
    return if (d == d.toLong().toDouble() && d < 1e15) d.toLong().toString()
    else "%.6f".format(d).trimEnd('0').trimEnd('.')
}
