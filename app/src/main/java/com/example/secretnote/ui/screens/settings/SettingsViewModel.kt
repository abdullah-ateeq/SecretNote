package com.example.secretnote.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.secretnote.data.preferences.AppPreferences
import com.example.secretnote.data.preferences.AppPreferencesRepository
import com.example.secretnote.data.preferences.AutoLockTimeout
import com.example.secretnote.data.preferences.ThemeMode
import com.example.secretnote.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: AppPreferencesRepository,
    private val noteRepository: NoteRepository
) : ViewModel() {

    val preferences: StateFlow<AppPreferences> = preferencesRepository.preferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppPreferences()
    )

    private val _noteCount = MutableStateFlow(0)
    val noteCount: StateFlow<Int> = _noteCount.asStateFlow()

    init {
        loadNoteCount()
    }

    fun loadNoteCount() {
        viewModelScope.launch {
            _noteCount.value = noteRepository.getNoteCount()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setRequireAuthOnLaunch(require: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setRequireAuthOnLaunch(require)
        }
    }

    fun setAutoLockTimeout(timeout: AutoLockTimeout) {
        viewModelScope.launch {
            preferencesRepository.setAutoLockTimeout(timeout)
        }
    }

    fun setUseBiometrics(use: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setUseBiometrics(use)
        }
    }

    fun setHideRecentsPreview(hide: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setHideRecentsPreview(hide)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            preferencesRepository.clearSearchHistory()
        }
    }

    fun resetDefaults() {
        viewModelScope.launch {
            preferencesRepository.resetToDefaults()
        }
    }

    class Factory(
        private val preferencesRepository: AppPreferencesRepository,
        private val noteRepository: NoteRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(preferencesRepository, noteRepository) as T
        }
    }
}
