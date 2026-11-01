package com.example.secretnote.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "secret_note_preferences")

class AppPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val REQUIRE_AUTH_ON_LAUNCH = booleanPreferencesKey("require_auth_on_launch")
        val AUTO_LOCK_TIMEOUT_SECONDS = longPreferencesKey("auto_lock_timeout_seconds")
        val USE_BIOMETRICS = booleanPreferencesKey("use_biometrics")
        val HIDE_RECENTS_PREVIEW = booleanPreferencesKey("hide_recents_preview")
        val SEARCH_HISTORY = stringSetPreferencesKey("search_history")
    }

    val preferencesFlow: Flow<AppPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val themeModeString = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
            val themeMode = try {
                ThemeMode.valueOf(themeModeString)
            } catch (e: IllegalArgumentException) {
                ThemeMode.SYSTEM
            }

            val requireAuth = preferences[PreferencesKeys.REQUIRE_AUTH_ON_LAUNCH] ?: true
            val timeoutSecs = preferences[PreferencesKeys.AUTO_LOCK_TIMEOUT_SECONDS] ?: AutoLockTimeout.IMMEDIATELY.seconds
            val autoLockTimeout = AutoLockTimeout.fromSeconds(timeoutSecs)
            val useBiometrics = preferences[PreferencesKeys.USE_BIOMETRICS] ?: true
            val hideRecents = preferences[PreferencesKeys.HIDE_RECENTS_PREVIEW] ?: true
            val searchHistory = preferences[PreferencesKeys.SEARCH_HISTORY]?.toList() ?: emptyList()

            AppPreferences(
                themeMode = themeMode,
                requireAuthOnLaunch = requireAuth,
                autoLockTimeout = autoLockTimeout,
                useBiometrics = useBiometrics,
                hideRecentsPreview = hideRecents,
                searchHistory = searchHistory
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun setRequireAuthOnLaunch(require: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REQUIRE_AUTH_ON_LAUNCH] = require
        }
    }

    suspend fun setAutoLockTimeout(timeout: AutoLockTimeout) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_LOCK_TIMEOUT_SECONDS] = timeout.seconds
        }
    }

    suspend fun setUseBiometrics(use: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_BIOMETRICS] = use
        }
    }

    suspend fun setHideRecentsPreview(hide: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HIDE_RECENTS_PREVIEW] = hide
        }
    }

    suspend fun addSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.SEARCH_HISTORY] ?: emptySet()
            val updated = (setOf(trimmed) + current).take(10).toSet()
            preferences[PreferencesKeys.SEARCH_HISTORY] = updated
        }
    }

    suspend fun clearSearchHistory() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.SEARCH_HISTORY)
        }
    }

    suspend fun resetToDefaults() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
