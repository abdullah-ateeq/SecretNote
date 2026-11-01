package com.example.secretnote.domain.model

data class Note(
    val id: Long = 0,
    val title: String,
    val content: String,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val previewSnippet: String
        get() {
            val trimmed = content.trim()
            return if (trimmed.length > 120) {
                trimmed.take(120) + "..."
            } else {
                trimmed
            }
        }

    val displayTitle: String
        get() = title.ifBlank { "Untitled Note" }
}
