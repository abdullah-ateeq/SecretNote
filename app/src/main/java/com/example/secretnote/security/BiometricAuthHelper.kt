package com.example.secretnote.security

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricAuthHelper(private val context: Context) {

    private val biometricManager = BiometricManager.from(context)
    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager

    fun getDeviceSecurityType(): DeviceSecurityType {
        val isSecure = keyguardManager?.isDeviceSecure == true
        if (!isSecure) {
            return DeviceSecurityType.NO_SECURITY
        }

        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        }

        val biometricStatus = biometricManager.canAuthenticate(authenticators)
        return if (biometricStatus == BiometricManager.BIOMETRIC_SUCCESS) {
            DeviceSecurityType.BIOMETRIC_AND_CREDENTIAL
        } else {
            DeviceSecurityType.CREDENTIAL_ONLY
        }
    }

    fun canAuthenticate(allowDeviceCredential: Boolean = true): Boolean {
        val isSecure = keyguardManager?.isDeviceSecure == true
        if (!isSecure) return false

        val authenticators = getAuthenticators(allowDeviceCredential)
        val canAuth = biometricManager.canAuthenticate(authenticators)
        return canAuth == BiometricManager.BIOMETRIC_SUCCESS || isSecure
    }

    private fun getAuthenticators(allowDeviceCredential: Boolean): Int {
        return if (allowDeviceCredential) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        }
    }

    fun promptAuthenticate(
        activity: FragmentActivity,
        title: String = "Unlock SecretNote",
        subtitle: String = "Confirm your identity to view your private notes",
        allowDeviceCredential: Boolean = true,
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit,
        onFailed: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errorCode, errString)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onFailed()
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)

        val authenticators = getAuthenticators(allowDeviceCredential)
        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setConfirmationRequired(false)

        if (allowDeviceCredential) {
            promptInfoBuilder.setAllowedAuthenticators(authenticators)
        } else {
            promptInfoBuilder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            promptInfoBuilder.setNegativeButtonText("Cancel")
        }

        try {
            prompt.authenticate(promptInfoBuilder.build())
        } catch (e: Exception) {
            onError(BiometricPrompt.ERROR_UNABLE_TO_PROCESS, e.localizedMessage ?: "Authentication failed to start")
        }
    }
}
