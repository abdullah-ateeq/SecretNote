package com.example.secretnote.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Auth : Screen("auth")
    data object Home : Screen("home")
    data object AddNote : Screen("note_editor/0")
    data object Settings : Screen("settings")
    data object About : Screen("about")

    companion object {
        const val NOTE_EDITOR_ROUTE = "note_editor/{noteId}"
        fun noteEditor(noteId: Long): String = "note_editor/$noteId"
    }
}
