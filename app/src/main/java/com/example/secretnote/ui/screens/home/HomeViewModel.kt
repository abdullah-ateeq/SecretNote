package com.example.secretnote.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.secretnote.data.preferences.AppPreferencesRepository
import com.example.secretnote.data.repository.NoteRepository
import com.example.secretnote.domain.model.Note
import com.example.secretnote.domain.model.NoteSortOrder
import com.example.secretnote.security.AppLockManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val notes: List<Note> = emptyList(),
    val pinnedNotes: List<Note> = emptyList(),
    val otherNotes: List<Note> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val sortOrder: NoteSortOrder = NoteSortOrder.DATE_MODIFIED_DESC,
    val isSearchVisible: Boolean = false,
    val searchHistory: List<String> = emptyList(),
    val noteToDelete: Note? = null
)

class HomeViewModel(
    private val noteRepository: NoteRepository,
    private val preferencesRepository: AppPreferencesRepository,
    private val appLockManager: AppLockManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(NoteSortOrder.DATE_MODIFIED_DESC)
    val sortOrder: StateFlow<NoteSortOrder> = _sortOrder.asStateFlow()

    private val _isSearchVisible = MutableStateFlow(false)
    val isSearchVisible: StateFlow<Boolean> = _isSearchVisible.asStateFlow()

    private val _noteToDelete = MutableStateFlow<Note?>(null)
    val noteToDelete: StateFlow<Note?> = _noteToDelete.asStateFlow()

    private data class FilterState(
        val query: String,
        val sortOrder: NoteSortOrder,
        val isSearchVisible: Boolean,
        val noteToDelete: Note?
    )

    private val filterStateFlow = combine(
        _searchQuery,
        _sortOrder,
        _isSearchVisible,
        _noteToDelete
    ) { query, sort, isSearchOpen, deleteCandidate ->
        FilterState(query, sort, isSearchOpen, deleteCandidate)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        noteRepository.getAllNotes(),
        filterStateFlow,
        preferencesRepository.preferencesFlow
    ) { allNotes, filter, prefs ->
        val filtered = if (filter.query.isBlank()) {
            allNotes
        } else {
            allNotes.filter { note ->
                note.title.contains(filter.query, ignoreCase = true) ||
                        note.content.contains(filter.query, ignoreCase = true)
            }
        }

        val sorted = when (filter.sortOrder) {
            NoteSortOrder.DATE_MODIFIED_DESC -> filtered.sortedByDescending { it.updatedAt }
            NoteSortOrder.DATE_MODIFIED_ASC -> filtered.sortedBy { it.updatedAt }
            NoteSortOrder.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
            NoteSortOrder.TITLE_DESC -> filtered.sortedByDescending { it.title.lowercase() }
        }

        val pinned = sorted.filter { it.isPinned }
        val others = sorted.filter { !it.isPinned }

        HomeUiState(
            notes = sorted,
            pinnedNotes = pinned,
            otherNotes = others,
            isLoading = false,
            searchQuery = filter.query,
            sortOrder = filter.sortOrder,
            isSearchVisible = filter.isSearchVisible,
            searchHistory = prefs.searchHistory,
            noteToDelete = filter.noteToDelete
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearchVisibility() {
        val next = !_isSearchVisible.value
        _isSearchVisible.value = next
        if (!next) {
            _searchQuery.value = ""
        }
    }

    fun submitSearch(query: String) {
        if (query.isNotBlank()) {
            viewModelScope.launch {
                preferencesRepository.addSearchQuery(query)
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun setSortOrder(sortOrder: NoteSortOrder) {
        _sortOrder.value = sortOrder
    }

    fun togglePin(note: Note) {
        viewModelScope.launch {
            noteRepository.togglePin(note.id, !note.isPinned)
        }
    }

    fun promptDelete(note: Note) {
        _noteToDelete.value = note
    }

    fun dismissDelete() {
        _noteToDelete.value = null
    }

    fun confirmDelete() {
        val note = _noteToDelete.value ?: return
        viewModelScope.launch {
            noteRepository.deleteNote(note.id)
            _noteToDelete.value = null
        }
    }

    fun lockApp() {
        appLockManager.lockNow()
    }

    class Factory(
        private val noteRepository: NoteRepository,
        private val preferencesRepository: AppPreferencesRepository,
        private val appLockManager: AppLockManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(noteRepository, preferencesRepository, appLockManager) as T
        }
    }
}
