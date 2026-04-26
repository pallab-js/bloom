package com.vibenote.app.domain.model

import java.util.UUID

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val strokeDataPath: String = "",
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList(),
    val folder: String = "",
    val canvasBackground: CanvasBackground = CanvasBackground.DARK
)