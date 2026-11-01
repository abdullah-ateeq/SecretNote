package com.example.secretnote.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.secretnote.SecretNoteApplication
import com.example.secretnote.ui.screens.about.AboutScreen
import com.example.secretnote.ui.screens.auth.AuthScreen
import com.example.secretnote.ui.screens.auth.AuthViewModel
import com.example.secretnote.ui.screens.editor.NoteEditorScreen
import com.example.secretnote.ui.screens.editor.NoteEditorViewModel
import com.example.secretnote.ui.screens.home.HomeScreen
import com.example.secretnote.ui.screens.home.HomeViewModel
import com.example.secretnote.ui.screens.settings.SettingsScreen
import com.example.secretnote.ui.screens.settings.SettingsViewModel
import com.example.secretnote.ui.screens.splash.SplashScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    app: SecretNoteApplication
) {
    val isLocked by app.appLockManager.isLocked.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Reactively lock navigation if lock manager indicates app is locked
    LaunchedEffect(isLocked, currentRoute) {
        if (isLocked && currentRoute != null &&
            currentRoute != Screen.Splash.route &&
            currentRoute != Screen.Auth.route
        ) {
            navController.navigate(Screen.Auth.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        }
    ) {
        // 1. Splash Screen
        composable(Screen.Splash.route) {
            SplashScreen(
                isLocked = isLocked,
                onNavigateNext = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // 2. Authentication / Unlock Screen
        composable(Screen.Auth.route) {
            val authViewModel: AuthViewModel = viewModel(
                factory = AuthViewModel.Factory(
                    biometricAuthHelper = app.biometricAuthHelper,
                    appLockManager = app.appLockManager
                )
            )
            AuthScreen(
                viewModel = authViewModel,
                onUnlockSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        // 3. Home Screen
        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(
                    noteRepository = app.noteRepository,
                    preferencesRepository = app.preferencesRepository,
                    appLockManager = app.appLockManager
                )
            )
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToAddNote = {
                    navController.navigate(Screen.noteEditor(0L))
                },
                onNavigateToEditNote = { noteId ->
                    navController.navigate(Screen.noteEditor(noteId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onLockApp = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // 4. Add / Edit Note Screen
        composable(
            route = Screen.NOTE_EDITOR_ROUTE,
            arguments = listOf(
                navArgument("noteId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
            val editorViewModel: NoteEditorViewModel = viewModel(
                key = "note_editor_$noteId",
                factory = NoteEditorViewModel.Factory(
                    noteId = noteId,
                    noteRepository = app.noteRepository
                )
            )
            NoteEditorScreen(
                viewModel = editorViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 5. Settings Screen
        composable(Screen.Settings.route) {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(
                    preferencesRepository = app.preferencesRepository,
                    noteRepository = app.noteRepository
                )
            )
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAbout = {
                    navController.navigate(Screen.About.route)
                }
            )
        }

        // 6. About Screen
        composable(Screen.About.route) {
            AboutScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
