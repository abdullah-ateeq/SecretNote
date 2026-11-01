package com.example.secretnote.ui.screens.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.secretnote.data.repository.NoteRepository
import com.example.secretnote.domain.model.Note
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditorUiState(
    val noteId: Long = 0L,
    val title: String = "",
    val content: String = "",
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val showDiscardDialog: Boolean = false
) {
    val canSave: Boolean
        get() = title.isNotBlank() || content.isNotBlank()

    val wordCount: Int
        get() = if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).size

    val charCount: Int
        get() = content.length
}

class NoteEditorViewModel(
    private val noteId: Long,
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState(noteId = noteId))
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _saveSuccessEvent = MutableSharedFlow<Unit>()
    val saveSuccessEvent: SharedFlow<Unit> = _saveSuccessEvent.asSharedFlow()

    private var initialNote: Note? = null

    init {
        if (noteId > 0) {
            loadNote(noteId)
        }
    }

    private fun loadNote(id: Long) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            val note = noteRepository.getNoteByIdDirect(id)
            if (note != null) {
                initialNote = note
                _uiState.value = _uiState.value.copy(
                    noteId = note.id,
                    title = note.title,
                    content = note.content,
                    isPinned = note.isPinned,
                    createdAt = note.createdAt,
                    updatedAt = note.updatedAt,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Note could not be loaded or was deleted."
                )
            }
        }
    }

    fun onTitleChange(newTitle: String) {
        _uiState.value = _uiState.value.copy(title = newTitle)
    }

    fun onContentChange(newContent: String) {
        _uiState.value = _uiState.value.copy(content = newContent)
    }

    fun togglePin() {
        val current = _uiState.value.isPinned
        _uiState.value = _uiState.value.copy(isPinned = !current)
    }

    fun hasUnsavedChanges(): Boolean {
        val current = _uiState.value
        val initial = initialNote
        return if (initial == null) {
            current.title.isNotBlank() || current.content.isNotBlank()
        } else {
            current.title != initial.title ||
                    current.content != initial.content ||
                    current.isPinned != initial.isPinned
        }
    }

    fun promptDiscard() {
        if (hasUnsavedChanges()) {
            _uiState.value = _uiState.value.copy(showDiscardDialog = true)
        }
    }

    fun dismissDiscardDialog() {
        _uiState.value = _uiState.value.copy(showDiscardDialog = false)
    }

    fun saveNote() {
        val current = _uiState.value
        if (!current.canSave) return

        _uiState.value = current.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                if (current.noteId > 0) {
                    val updatedNote = Note(
                        id = current.noteId,
                        title = current.title.trim(),
                        content = current.content,
                        isPinned = current.isPinned,
                        createdAt = current.createdAt,
                        updatedAt = now
                    )
                    noteRepository.updateNote(updatedNote)
                } else {
                    val newNote = Note(
                        title = current.title.trim(),
                        content = current.content,
                        isPinned = current.isPinned,
                        createdAt = now,
                        updatedAt = now
                    )
                    noteRepository.saveNote(newNote)
                }
                _uiState.value = _uiState.value.copy(isSaving = false)
                _saveSuccessEvent.emit(Unit)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "Failed to securely save note: ${e.localizedMessage}"
                )
            }
        }
    }

    class Factory(
        private val noteId: Long,
        private val noteRepository: NoteRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NoteEditorViewModel(noteId, noteRepository) as T
        }
    }
}
