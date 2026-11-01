package com.example.secretnote.security

sealed interface BiometricAuthState {
    data object Authenticated : BiometricAuthState
    data object Locked : BiometricAuthState
    data class Error(val message: String, val isPermanent: Boolean = false) : BiometricAuthState
    data object NoDeviceSecurity : BiometricAuthState
}

enum class DeviceSecurityType {
    BIOMETRIC_AND_CREDENTIAL,
    CREDENTIAL_ONLY,
    NO_SECURITY
}
