package com.example.secretnote.home

import com.example.secretnote.domain.model.Note
import com.example.secretnote.domain.model.NoteSortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteSortingAndFilteringTest {

    private val notes = listOf(
        Note(id = 1, title = "Banana Recipe", content = "Mix bananas and flour", isPinned = false, updatedAt = 1000L),
        Note(id = 2, title = "Apple Secrets", content = "Hidden in the orchard", isPinned = true, updatedAt = 2000L),
        Note(id = 3, title = "Cherry Pie", content = "Bake at 350 degrees", isPinned = false, updatedAt = 3000L),
        Note(id = 4, title = "Daily Journal", content = "Had an apple today", isPinned = false, updatedAt = 4000L)
    )

    @Test
    fun `search filters notes across title and content`() {
        val query = "apple"
        val filtered = notes.filter {
            it.title.contains(query, ignoreCase = true) || it.content.contains(query, ignoreCase = true)
        }

        assertEquals(2, filtered.size)
        assertTrue(filtered.any { it.id == 2L }) // Title matches "Apple Secrets"
        assertTrue(filtered.any { it.id == 4L }) // Content matches "Had an apple today"
    }

    @Test
    fun `search with no match returns empty list`() {
        val query = "nonexistent phrase"
        val filtered = notes.filter {
            it.title.contains(query, ignoreCase = true) || it.content.contains(query, ignoreCase = true)
        }
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun `sorting by recently updated orders descending`() {
        val sorted = notes.sortedByDescending { it.updatedAt }
        assertEquals(4L, sorted[0].id)
        assertEquals(3L, sorted[1].id)
        assertEquals(2L, sorted[2].id)
        assertEquals(1L, sorted[3].id)
    }

    @Test
    fun `sorting by title A to Z orders alphabetically`() {
        val sorted = notes.sortedBy { it.title.lowercase() }
        assertEquals("Apple Secrets", sorted[0].title)
        assertEquals("Banana Recipe", sorted[1].title)
        assertEquals("Cherry Pie", sorted[2].title)
        assertEquals("Daily Journal", sorted[3].title)
    }

    @Test
    fun `pinned notes partition ensures pinned are separated to top`() {
        val pinned = notes.filter { it.isPinned }
        val others = notes.filter { !it.isPinned }

        assertEquals(1, pinned.size)
        assertEquals(2L, pinned[0].id)
        assertEquals(3, others.size)
    }
}
