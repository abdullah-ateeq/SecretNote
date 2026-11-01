package com.example.secretnote.editor

import com.example.secretnote.domain.model.Note
import com.example.secretnote.ui.screens.editor.EditorUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteEditorValidationTest {

    @Test
    fun `empty note cannot be saved`() {
        val emptyState = EditorUiState(title = "", content = "")
        assertFalse("Cannot save a note with empty title and empty content", emptyState.canSave)

        val whitespaceState = EditorUiState(title = "   ", content = "   ")
        assertFalse("Cannot save a note with only whitespace", whitespaceState.canSave)
    }

    @Test
    fun `note with only title or only content can be saved`() {
        val titleOnly = EditorUiState(title = "Shopping List", content = "")
        assertTrue("Note with title only should be savable", titleOnly.canSave)

        val contentOnly = EditorUiState(title = "", content = "Remember to buy milk")
        assertTrue("Note with content only should be savable", contentOnly.canSave)
    }

    @Test
    fun `word count and character count compute accurately`() {
        val state = EditorUiState(
            title = "Test",
            content = "The quick brown fox jumps over the lazy dog."
        )

        assertEquals(9, state.wordCount)
        assertEquals(44, state.charCount)

        val emptyState = EditorUiState(content = "")
        assertEquals(0, emptyState.wordCount)
        assertEquals(0, emptyState.charCount)
    }

    @Test
    fun `domain note display title and preview snippet format properly`() {
        val blankTitleNote = Note(title = "", content = "Hello world")
        assertEquals("Untitled Note", blankTitleNote.displayTitle)

        val longContent = "A".repeat(200)
        val note = Note(title = "Long Note", content = longContent)
        assertEquals(123, note.previewSnippet.length) // 120 chars + "..."
        assertTrue(note.previewSnippet.endsWith("..."))
    }
}
