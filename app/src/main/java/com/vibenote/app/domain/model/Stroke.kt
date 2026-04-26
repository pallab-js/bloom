package com.vibenote.app.domain.model

data class Stroke(
    val points: String = "",
    val colorValue: Int = 0xFFFFFFFF.toInt(),
    val strokeWidth: Float = 4f,
    val isEraser: Boolean = false,
    val isHighlighter: Boolean = false,
    val strokeType: StrokeType = StrokeType.PEN
)

enum class StrokeType {
    PEN,
    HIGHLIGHTER,
    LINE,
    RECTANGLE,
    CIRCLE
}