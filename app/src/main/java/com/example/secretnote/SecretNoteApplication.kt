package com.example.secretnote

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.secretnote.data.crypto.CryptoManager
import com.example.secretnote.data.crypto.KeystoreCryptoManager
import com.example.secretnote.data.local.NoteDatabase
import com.example.secretnote.data.preferences.AppPreferencesRepository
import com.example.secretnote.data.repository.NoteRepository
import com.example.secretnote.data.repository.NoteRepositoryImpl
import com.example.secretnote.security.AppLockManager
import com.example.secretnote.security.BiometricAuthHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SecretNoteApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val cryptoManager: CryptoManager by lazy {
        KeystoreCryptoManager()
    }

    val database: NoteDatabase by lazy {
        NoteDatabase.getDatabase(this)
    }

    val noteRepository: NoteRepository by lazy {
        NoteRepositoryImpl(database.noteDao(), cryptoManager)
    }

    val preferencesRepository: AppPreferencesRepository by lazy {
        AppPreferencesRepository(this)
    }

    val biometricAuthHelper: BiometricAuthHelper by lazy {
        BiometricAuthHelper(this)
    }

    val appLockManager: AppLockManager by lazy {
        AppLockManager()
    }

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLockManager)

        applicationScope.launch {
            preferencesRepository.preferencesFlow.collectLatest { prefs ->
                appLockManager.updateConfig(
                    timeout = prefs.autoLockTimeout,
                    requireOnLaunch = prefs.requireAuthOnLaunch
                )
            }
        }
    }
}
