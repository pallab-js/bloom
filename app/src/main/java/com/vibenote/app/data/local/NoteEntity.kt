package com.vibenote.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vibenote.app.domain.model.CanvasBackground

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long = System.currentTimeMillis(),
    val strokeDataPath: String,
    val isFavorite: Boolean = false,
    val tags: String = "",
    val folder: String = "",
    val canvasBackground: CanvasBackground = CanvasBackground.DARK,
    val folderId: String? = null,
    val sourceUri: String? = null,
    val contentJson: String? = null
)