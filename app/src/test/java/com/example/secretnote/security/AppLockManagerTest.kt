package com.example.secretnote.security

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.example.secretnote.data.preferences.AutoLockTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MockLifecycleOwner : LifecycleOwner {
    val registry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = registry
}

class AppLockManagerTest {

    private var currentTime = 100_000L
    private lateinit var appLockManager: AppLockManager
    private lateinit var lifecycleOwner: MockLifecycleOwner

    @Before
    fun setUp() {
        currentTime = 100_000L
        appLockManager = AppLockManager(timeProvider = { currentTime })
        lifecycleOwner = MockLifecycleOwner()
    }

    @Test
    fun `initial state is locked by default`() {
        assertTrue("App should start locked by default", appLockManager.isLocked.value)
    }

    @Test
    fun `unlock sets isLocked to false and lockNow sets it back to true`() {
        appLockManager.unlock()
        assertFalse("App should be unlocked after unlock()", appLockManager.isLocked.value)

        appLockManager.lockNow()
        assertTrue("App should be locked after lockNow()", appLockManager.isLocked.value)
    }

    @Test
    fun `immediate auto-lock locks app when moving to background`() {
        appLockManager.updateConfig(
            timeout = AutoLockTimeout.IMMEDIATELY,
            requireOnLaunch = true
        )
        appLockManager.unlock()
        assertFalse(appLockManager.isLocked.value)

        // Simulate app entering background
        appLockManager.onStop(lifecycleOwner)
        assertTrue("App should immediately lock on stop", appLockManager.isLocked.value)
    }

    @Test
    fun `delayed auto-lock does not lock if elapsed time is below threshold`() {
        appLockManager.updateConfig(
            timeout = AutoLockTimeout.SECONDS_30,
            requireOnLaunch = true
        )
        appLockManager.unlock()
        assertFalse(appLockManager.isLocked.value)

        // App goes to background at t = 100_000
        appLockManager.onStop(lifecycleOwner)
        assertFalse("Should not lock immediately when timeout is 30 seconds", appLockManager.isLocked.value)

        // Return to foreground after 10 seconds (t = 110_000)
        currentTime += 10_000L
        appLockManager.onStart(lifecycleOwner)
        assertFalse("Should still be unlocked after only 10s inactivity", appLockManager.isLocked.value)
    }

    @Test
    fun `delayed auto-lock locks when elapsed time exceeds threshold`() {
        appLockManager.updateConfig(
            timeout = AutoLockTimeout.SECONDS_30,
            requireOnLaunch = true
        )
        appLockManager.unlock()
        assertFalse(appLockManager.isLocked.value)

        // App goes to background at t = 100_000
        appLockManager.onStop(lifecycleOwner)

        // Return to foreground after 35 seconds (t = 135_000)
        currentTime += 35_000L
        appLockManager.onStart(lifecycleOwner)
        assertTrue("Should be locked after 35s inactivity exceeding 30s timeout", appLockManager.isLocked.value)
    }

    @Test
    fun `requireAuthOnLaunch false unlocks app if configured on first launch`() {
        val manager = AppLockManager(timeProvider = { currentTime })
        manager.updateConfig(
            timeout = AutoLockTimeout.MINUTES_1,
            requireOnLaunch = false
        )
        assertFalse("If requireAuthOnLaunch is false, app can start unlocked", manager.isLocked.value)
    }
}
