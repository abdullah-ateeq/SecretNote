package com.example.secretnote.data.repository

import com.example.secretnote.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getAllNotes(): Flow<List<Note>>
    fun getNoteById(id: Long): Flow<Note?>
    suspend fun getNoteByIdDirect(id: Long): Note?
    suspend fun saveNote(note: Note): Long
    suspend fun updateNote(note: Note)
    suspend fun deleteNote(id: Long): Boolean
    suspend fun togglePin(id: Long, isPinned: Boolean): Boolean
    suspend fun getNoteCount(): Int
}
