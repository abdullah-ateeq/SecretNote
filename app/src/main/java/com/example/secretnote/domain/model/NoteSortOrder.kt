package com.example.secretnote.domain.model

enum class NoteSortOrder(val displayName: String) {
    DATE_MODIFIED_DESC("Recently updated"),
    DATE_MODIFIED_ASC("Oldest updated"),
    TITLE_ASC("Title (A to Z)"),
    TITLE_DESC("Title (Z to A)")
}
