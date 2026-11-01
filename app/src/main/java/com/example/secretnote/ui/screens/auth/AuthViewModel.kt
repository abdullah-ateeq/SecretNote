package com.example.secretnote.ui.screens.auth

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.secretnote.security.AppLockManager
import com.example.secretnote.security.BiometricAuthHelper
import com.example.secretnote.security.BiometricAuthState
import com.example.secretnote.security.DeviceSecurityType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel(
    private val biometricAuthHelper: BiometricAuthHelper,
    private val appLockManager: AppLockManager
) : ViewModel() {

    private val _authState = MutableStateFlow<BiometricAuthState>(BiometricAuthState.Locked)
    val authState: StateFlow<BiometricAuthState> = _authState.asStateFlow()

    private val _deviceSecurityType = MutableStateFlow(DeviceSecurityType.CREDENTIAL_ONLY)
    val deviceSecurityType: StateFlow<DeviceSecurityType> = _deviceSecurityType.asStateFlow()

    init {
        checkSecurityStatus()
    }

    fun checkSecurityStatus() {
        val securityType = biometricAuthHelper.getDeviceSecurityType()
        _deviceSecurityType.value = securityType

        if (securityType == DeviceSecurityType.NO_SECURITY) {
            _authState.value = BiometricAuthState.NoDeviceSecurity
        } else if (_authState.value is BiometricAuthState.NoDeviceSecurity) {
            _authState.value = BiometricAuthState.Locked
        }
    }

    fun authenticate(activity: FragmentActivity, onAuthenticated: () -> Unit) {
        val securityType = biometricAuthHelper.getDeviceSecurityType()
        if (securityType == DeviceSecurityType.NO_SECURITY) {
            _authState.value = BiometricAuthState.NoDeviceSecurity
            return
        }

        biometricAuthHelper.promptAuthenticate(
            activity = activity,
            title = "Unlock SecretNote",
            subtitle = "Verify your fingerprint, face, or device PIN/pattern to access your private notes",
            allowDeviceCredential = true,
            onSuccess = {
                appLockManager.unlock()
                _authState.value = BiometricAuthState.Authenticated
                onAuthenticated()
            },
            onError = { errorCode, errString ->
                // Error 10 is user canceled / back pressed; keep state clean
                val isCanceled = errorCode == 10 || errorCode == 13
                _authState.value = if (isCanceled) {
                    BiometricAuthState.Locked
                } else {
                    BiometricAuthState.Error(
                        message = errString.toString().ifBlank { "Authentication failed. Please try again." },
                        isPermanent = errorCode == 7 // Lockout
                    )
                }
            },
            onFailed = {
                _authState.value = BiometricAuthState.Error("Authentication not recognized. Please try again.")
            }
        )
    }

    class Factory(
        private val biometricAuthHelper: BiometricAuthHelper,
        private val appLockManager: AppLockManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(biometricAuthHelper, appLockManager) as T
        }
    }
}
