package com.example.secretnote

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.example.secretnote.data.preferences.AppPreferences
import com.example.secretnote.ui.navigation.AppNavigation
import com.example.secretnote.ui.theme.SecretNoteTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SecretNoteApplication

        setContent {
            val preferences by app.preferencesRepository.preferencesFlow
                .collectAsState(initial = AppPreferences())

            // Apply FLAG_SECURE dynamically according to security settings
            LaunchedEffect(preferences.hideRecentsPreview) {
                if (preferences.hideRecentsPreview) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            SecretNoteTheme(themeMode = preferences.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    AppNavigation(
                        navController = navController,
                        app = app
                    )
                }
            }
        }
    }
}
