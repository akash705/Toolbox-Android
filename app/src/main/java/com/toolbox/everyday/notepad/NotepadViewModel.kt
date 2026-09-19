package com.toolbox.everyday.notepad

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class NotepadViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = NotesRepository(application)

    /** The note currently open in the editor, or null when viewing the list. */
    data class Editor(val id: String, val body: String, val isNew: Boolean)

    val notes: StateFlow<List<Note>> = repo.notes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _editor = MutableStateFlow<Editor?>(null)
    val editor: StateFlow<Editor?> = _editor.asStateFlow()

    private var saveJob: Job? = null

    fun openNew() {
        _editor.value = Editor(id = UUID.randomUUID().toString(), body = "", isNew = true)
    }

    fun openExisting(note: Note) {
        _editor.value = Editor(id = note.id, body = note.body, isNew = false)
    }

    fun onBodyChange(text: String) {
        val e = _editor.value ?: return
        _editor.value = e.copy(body = text)
        scheduleAutosave()
    }

    private fun scheduleAutosave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            kotlinx.coroutines.delay(400)
            val e = _editor.value ?: return@launch
            if (e.body.isNotBlank()) {
                repo.upsert(Note(id = e.id, body = e.body, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    /** Closing the editor: persist non-empty content; discard a note whose body is blank. */
    fun closeEditor() {
        saveJob?.cancel()
        val e = _editor.value
        _editor.value = null
        if (e == null) return
        viewModelScope.launch {
            if (e.body.isBlank()) {
                // Never leave an empty ghost note (removes it if it previously existed).
                repo.delete(e.id)
            } else {
                repo.upsert(Note(id = e.id, body = e.body, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { repo.delete(id) }
        if (_editor.value?.id == id) _editor.value = null
    }
}
