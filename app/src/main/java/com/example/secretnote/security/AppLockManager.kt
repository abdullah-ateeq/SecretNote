package com.example.secretnote.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.secretnote.data.preferences.AutoLockTimeout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppLockManager(
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) : DefaultLifecycleObserver {

    private val _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var autoLockTimeout: AutoLockTimeout = AutoLockTimeout.IMMEDIATELY
    private var requireAuthOnLaunch: Boolean = true
    private var backgroundTimestamp: Long = 0L
    private var isFirstLaunch: Boolean = true

    fun updateConfig(timeout: AutoLockTimeout, requireOnLaunch: Boolean) {
        this.autoLockTimeout = timeout
        this.requireAuthOnLaunch = requireOnLaunch

        if (isFirstLaunch) {
            _isLocked.value = requireOnLaunch
            isFirstLaunch = false
        }
    }

    fun lockNow() {
        _isLocked.value = true
        backgroundTimestamp = 0L
    }

    fun unlock() {
        _isLocked.value = false
        backgroundTimestamp = 0L
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        backgroundTimestamp = timeProvider()
        if (autoLockTimeout == AutoLockTimeout.IMMEDIATELY) {
            _isLocked.value = true
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if (backgroundTimestamp > 0L) {
            val elapsedMillis = timeProvider() - backgroundTimestamp
            val thresholdMillis = autoLockTimeout.seconds * 1000L

            if (elapsedMillis >= thresholdMillis) {
                _isLocked.value = true
            }
        }
    }
}
