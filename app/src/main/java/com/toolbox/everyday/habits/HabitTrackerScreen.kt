package com.toolbox.everyday.habits

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.UUID

@Serializable
data class Habit(val id: String, val name: String, val done: Set<String> = emptySet())

private val json = Json { ignoreUnknownKeys = true }

@Composable
fun HabitTrackerScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("habits", Context.MODE_PRIVATE) }
    var habits by remember { mutableStateOf(load(prefs)) }
    var newName by remember { mutableStateOf("") }
    val today = remember { LocalDate.now().toString() }

    fun persist(list: List<Habit>) {
        habits = list
        prefs.edit().putString("habits", json.encodeToString(list)).apply()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("New habit") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = {
                if (newName.isNotBlank()) {
                    persist(habits + Habit(UUID.randomUUID().toString(), newName.trim()))
                    newName = ""
                }
            }) { Icon(Icons.Default.Add, contentDescription = "Add habit") }
        }
        Spacer(Modifier.height(16.dp))

        if (habits.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Add a habit to start a streak.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(habits, key = { it.id }) { habit ->
                    val doneToday = today in habit.done
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = {
                                val newDone = if (doneToday) habit.done - today else habit.done + today
                                persist(habits.map { if (it.id == habit.id) it.copy(done = newDone) else it })
                            }) {
                                Icon(
                                    if (doneToday) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = "Toggle today",
                                    tint = if (doneToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(habit.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("🔥 ${streak(habit.done)} day streak", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { persist(habits.filterNot { it.id == habit.id }) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete habit")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun streak(done: Set<String>): Int {
    var day = LocalDate.now()
    if (day.toString() !in done) day = day.minusDays(1) // not done today yet: count through yesterday
    var count = 0
    while (day.toString() in done) {
        count++
        day = day.minusDays(1)
    }
    return count
}

private fun load(prefs: android.content.SharedPreferences): List<Habit> {
    val raw = prefs.getString("habits", null) ?: return emptyList()
    return runCatching { json.decodeFromString<List<Habit>>(raw) }.getOrDefault(emptyList())
}
