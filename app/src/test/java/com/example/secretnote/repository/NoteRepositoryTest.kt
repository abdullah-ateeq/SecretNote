package com.example.secretnote.repository

import com.example.secretnote.crypto.TestAesGcmCryptoManager
import com.example.secretnote.data.crypto.CryptoManager
import com.example.secretnote.data.local.EncryptedNoteEntity
import com.example.secretnote.data.local.NoteDao
import com.example.secretnote.data.repository.NoteRepository
import com.example.secretnote.data.repository.NoteRepositoryImpl
import com.example.secretnote.domain.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * In-memory Fake implementation of NoteDao for fast, deterministic unit tests.
 */
class FakeNoteDao : NoteDao {
    val entities = mutableListOf<EncryptedNoteEntity>()
    private val flow = MutableStateFlow<List<EncryptedNoteEntity>>(emptyList())
    private var nextId = 1L

    private fun updateFlow() {
        flow.value = entities.sortedWith(
            compareByDescending<EncryptedNoteEntity> { it.isPinned }
                .thenByDescending { it.updatedAt }
        )
    }

    override fun getAllNotes(): Flow<List<EncryptedNoteEntity>> = flow

    override fun getNoteById(id: Long): Flow<EncryptedNoteEntity?> = flow.map { list ->
        list.find { it.id == id }
    }

    override suspend fun getNoteByIdDirect(id: Long): EncryptedNoteEntity? {
        return entities.find { it.id == id }
    }

    override suspend fun insertNote(note: EncryptedNoteEntity): Long {
        val id = if (note.id == 0L) nextId++ else note.id
        val newEntity = note.copy(id = id)
        entities.removeAll { it.id == id }
        entities.add(newEntity)
        updateFlow()
        return id
    }

    override suspend fun updateNote(note: EncryptedNoteEntity) {
        val index = entities.indexOfFirst { it.id == note.id }
        if (index != -1) {
            entities[index] = note
            updateFlow()
        }
    }

    override suspend fun deleteNote(id: Long): Int {
        val removed = entities.removeAll { it.id == id }
        if (removed) updateFlow()
        return if (removed) 1 else 0
    }

    override suspend fun updatePinStatus(id: Long, isPinned: Boolean): Int {
        val index = entities.indexOfFirst { it.id == id }
        return if (index != -1) {
            entities[index] = entities[index].copy(isPinned = isPinned)
            updateFlow()
            1
        } else {
            0
        }
    }

    override suspend fun getNoteCount(): Int = entities.size
}

class NoteRepositoryTest {

    private lateinit var fakeDao: FakeNoteDao
    private lateinit var cryptoManager: CryptoManager
    private lateinit var repository: NoteRepository

    @Before
    fun setUp() {
        fakeDao = FakeNoteDao()
        cryptoManager = TestAesGcmCryptoManager()
        repository = NoteRepositoryImpl(fakeDao, cryptoManager)
    }

    @Test
    fun `saveNote encrypts data and ensures plaintext is never persisted in DAO`() = runBlocking {
        val plaintextTitle = "Secret Bank Account"
        val plaintextBody = "Account: 987654321 PIN: 4321"

        val note = Note(
            title = plaintextTitle,
            content = plaintextBody,
            isPinned = false
        )

        val id = repository.saveNote(note)
        assertTrue(id > 0)

        // Verify the DAO entity contains only ciphertext and IV
        val persistedEntity = fakeDao.entities.find { it.id == id }
        assertNotNull(persistedEntity)
        assertFalse(persistedEntity!!.titleEncrypted.contains(plaintextTitle))
        assertFalse(persistedEntity.contentEncrypted.contains(plaintextBody))
        assertNotNull(persistedEntity.titleIv)
        assertNotNull(persistedEntity.contentIv)

        // Verify that repository correctly decrypts the note
        val retrieved = repository.getNoteByIdDirect(id)
        assertNotNull(retrieved)
        assertEquals(plaintextTitle, retrieved!!.title)
        assertEquals(plaintextBody, retrieved.content)
    }

    @Test
    fun `updateNote modifies encrypted note successfully`() = runBlocking {
        val initialNote = Note(title = "Initial Title", content = "Initial Content")
        val id = repository.saveNote(initialNote)

        val updatedNote = Note(id = id, title = "Updated Title", content = "Updated Content", isPinned = true)
        repository.updateNote(updatedNote)

        val retrieved = repository.getNoteByIdDirect(id)
        assertNotNull(retrieved)
        assertEquals("Updated Title", retrieved!!.title)
        assertEquals("Updated Content", retrieved.content)
        assertTrue(retrieved.isPinned)
    }

    @Test
    fun `deleteNote removes note from storage`() = runBlocking {
        val note = Note(title = "To Delete", content = "Will be deleted")
        val id = repository.saveNote(note)

        val deleted = repository.deleteNote(id)
        assertTrue(deleted)

        val retrieved = repository.getNoteByIdDirect(id)
        assertNull(retrieved)
        assertEquals(0, repository.getNoteCount())
    }

    @Test
    fun `togglePin updates pinned state`() = runBlocking {
        val note = Note(title = "Important Note", content = "Don't lose this", isPinned = false)
        val id = repository.saveNote(note)

        repository.togglePin(id, true)
        var retrieved = repository.getNoteByIdDirect(id)
        assertTrue(retrieved!!.isPinned)

        repository.togglePin(id, false)
        retrieved = repository.getNoteByIdDirect(id)
        assertFalse(retrieved!!.isPinned)
    }

    @Test
    fun `decryption error is handled gracefully when database entity is corrupted`() = runBlocking {
        // Insert a note with corrupted ciphertext
        val corruptedEntity = EncryptedNoteEntity(
            id = 999L,
            titleEncrypted = "CorruptedBase64Ciphertext==",
            titleIv = "BadIV==",
            contentEncrypted = "CorruptedContent==",
            contentIv = "BadIV==",
            isPinned = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        fakeDao.insertNote(corruptedEntity)

        val note = repository.getNoteByIdDirect(999L)
        assertNotNull(note)
        assertTrue(note!!.title.contains("Decryption Error"))
        assertTrue(note.content.contains("Decryption Error"))
    }
}
