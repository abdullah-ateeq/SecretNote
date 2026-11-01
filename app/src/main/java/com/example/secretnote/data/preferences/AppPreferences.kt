package com.example.secretnote.data.preferences

enum class ThemeMode(val title: String) {
    SYSTEM("System default"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class AutoLockTimeout(val seconds: Long, val displayName: String) {
    IMMEDIATELY(0L, "Immediately"),
    SECONDS_30(30L, "30 seconds"),
    MINUTES_1(60L, "1 minute"),
    MINUTES_5(300L, "5 minutes");

    companion object {
        fun fromSeconds(seconds: Long): AutoLockTimeout {
            return entries.firstOrNull { it.seconds == seconds } ?: IMMEDIATELY
        }
    }
}

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val requireAuthOnLaunch: Boolean = true,
    val autoLockTimeout: AutoLockTimeout = AutoLockTimeout.IMMEDIATELY,
    val useBiometrics: Boolean = true,
    val hideRecentsPreview: Boolean = true,
    val searchHistory: List<String> = emptyList()
)
