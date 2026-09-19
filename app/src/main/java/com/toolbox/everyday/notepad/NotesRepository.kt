package com.toolbox.everyday.notepad

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Local, offline notes storage. Notes are serialized to a single JSON blob in a Preferences
 * DataStore, mirrored after every successful write to a separate "last-good" file. Because a
 * single-file DataStore corruption would take a same-file backup key with it, the backup lives
 * in its own file and a [ReplaceFileCorruptionHandler] plus a read-time fallback restore from it,
 * so a bad write or a corrupt store never silently wipes the user's notes.
 */
private val Context.notesDataStore by preferencesDataStore(
    name = "notepad",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

class NotesRepository(private val context: Context) {

    private companion object {
        val NOTES_JSON = stringPreferencesKey("notes_json")
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val backupFile: File by lazy { File(context.filesDir, "notepad_lastgood.json") }

    val notes: Flow<List<Note>> = context.notesDataStore.data.map { prefs ->
        val raw = prefs[NOTES_JSON]
        parseOrRestore(raw)
    }

    suspend fun upsert(note: Note) {
        val current = readOnce().toMutableList()
        val idx = current.indexOfFirst { it.id == note.id }
        if (idx >= 0) current[idx] = note else current.add(note)
        writeAll(current)
    }

    suspend fun delete(id: String) {
        writeAll(readOnce().filterNot { it.id == id })
    }

    private suspend fun readOnce(): List<Note> {
        val prefs = context.notesDataStore.data.first()
        return parseOrRestore(prefs[NOTES_JSON])
    }

    private suspend fun writeAll(notes: List<Note>) {
        val sorted = notes.sortedByDescending { it.updatedAt }
        val encoded = json.encodeToString(sorted)
        context.notesDataStore.edit { it[NOTES_JSON] = encoded }
        // Mirror to the separate last-good file only after a successful primary write.
        runCatching { backupFile.writeText(encoded) }
    }

    /** Parse the primary blob; on any failure fall back to the last-good file, never to empty-that-overwrites. */
    private fun parseOrRestore(raw: String?): List<Note> {
        if (!raw.isNullOrBlank()) {
            runCatching { json.decodeFromString<List<Note>>(raw) }.getOrNull()?.let { return it }
        }
        // Primary missing or unparseable: try the separate backup.
        if (backupFile.exists()) {
            runCatching { json.decodeFromString<List<Note>>(backupFile.readText()) }
                .getOrNull()?.let { return it }
        }
        return emptyList()
    }
}
