package com.example.secretnote.data.repository

import com.example.secretnote.data.crypto.CryptoException
import com.example.secretnote.data.crypto.CryptoManager
import com.example.secretnote.data.crypto.EncryptedData
import com.example.secretnote.data.local.EncryptedNoteEntity
import com.example.secretnote.data.local.NoteDao
import com.example.secretnote.domain.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class NoteRepositoryImpl(
    private val noteDao: NoteDao,
    private val cryptoManager: CryptoManager
) : NoteRepository {

    override fun getAllNotes(): Flow<List<Note>> {
        return noteDao.getAllNotes().map { entities ->
            entities.map { entity ->
                decryptEntity(entity)
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun getNoteById(id: Long): Flow<Note?> {
        return noteDao.getNoteById(id).map { entity ->
            entity?.let { decryptEntity(it) }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getNoteByIdDirect(id: Long): Note? = withContext(Dispatchers.IO) {
        val entity = noteDao.getNoteByIdDirect(id)
        entity?.let { decryptEntity(it) }
    }

    override suspend fun saveNote(note: Note): Long = withContext(Dispatchers.IO) {
        val entity = encryptNote(note)
        noteDao.insertNote(entity)
    }

    override suspend fun updateNote(note: Note) = withContext(Dispatchers.IO) {
        val entity = encryptNote(note)
        noteDao.updateNote(entity)
    }

    override suspend fun deleteNote(id: Long): Boolean = withContext(Dispatchers.IO) {
        noteDao.deleteNote(id) > 0
    }

    override suspend fun togglePin(id: Long, isPinned: Boolean): Boolean = withContext(Dispatchers.IO) {
        noteDao.updatePinStatus(id, isPinned) > 0
    }

    override suspend fun getNoteCount(): Int = withContext(Dispatchers.IO) {
        noteDao.getNoteCount()
    }

    private fun encryptNote(note: Note): EncryptedNoteEntity {
        val titleEncrypted = cryptoManager.encrypt(note.title)
        val contentEncrypted = cryptoManager.encrypt(note.content)

        return EncryptedNoteEntity(
            id = note.id,
            titleEncrypted = titleEncrypted.ciphertextBase64,
            titleIv = titleEncrypted.ivBase64,
            contentEncrypted = contentEncrypted.ciphertextBase64,
            contentIv = contentEncrypted.ivBase64,
            isPinned = note.isPinned,
            createdAt = note.createdAt,
            updatedAt = note.updatedAt
        )
    }

    private fun decryptEntity(entity: EncryptedNoteEntity): Note {
        val title = try {
            cryptoManager.decrypt(
                EncryptedData(
                    ciphertextBase64 = entity.titleEncrypted,
                    ivBase64 = entity.titleIv
                )
            )
        } catch (e: CryptoException) {
            "[Decryption Error: Title Corrupted]"
        }

        val content = try {
            cryptoManager.decrypt(
                EncryptedData(
                    ciphertextBase64 = entity.contentEncrypted,
                    ivBase64 = entity.contentIv
                )
            )
        } catch (e: CryptoException) {
            "[Decryption Error: Content could not be decrypted with current key]"
        }

        return Note(
            id = entity.id,
            title = title,
            content = content,
            isPinned = entity.isPinned,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
}
