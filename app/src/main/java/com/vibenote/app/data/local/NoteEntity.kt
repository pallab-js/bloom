package com.vibenote.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val createdAt: Long,
    val strokeDataPath: String,
    val isFavorite: Boolean = false,
    val tags: String = "",
    val folder: String = ""
)