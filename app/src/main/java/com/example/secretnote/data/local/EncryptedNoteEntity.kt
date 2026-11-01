package com.example.secretnote.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class EncryptedNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "title_encrypted")
    val titleEncrypted: String,

    @ColumnInfo(name = "title_iv")
    val titleIv: String,

    @ColumnInfo(name = "content_encrypted")
    val contentEncrypted: String,

    @ColumnInfo(name = "content_iv")
    val contentIv: String,

    @ColumnInfo(name = "is_pinned", defaultValue = "0")
    val isPinned: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
