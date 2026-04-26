package com.vibenote.app.domain.model

import java.util.UUID

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val strokeDataPath: String = "",
    val strokes: List<Stroke> = emptyList(),
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList(),
    val folder: String = "",
    val canvasBackground: CanvasBackground = CanvasBackground.DARK,
    val folderId: String? = null,
    val sourceUri: String? = null,
    val contentJson: String? = null
)