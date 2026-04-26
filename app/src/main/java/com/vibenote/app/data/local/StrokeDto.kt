package com.vibenote.app.data.local

import androidx.compose.ui.geometry.Offset
import com.vibenote.app.domain.model.Stroke
import com.vibenote.app.domain.model.StrokeType

data class StrokeDto(
    val points: String = "",
    val colorValue: Int = 0xFFFFFFFF.toInt(),
    val strokeWidth: Float = 4f,
    val isEraser: Boolean = false,
    val isHighlighter: Boolean = false,
    val strokeType: StrokeType = StrokeType.PEN
)

fun StrokeDto.toDomain(): Stroke {
    val pointsList = points.split(";").mapNotNull { pair ->
        val coords = pair.split(",")
        if (coords.size == 2) {
            try {
                Offset(coords[0].toFloat(), coords[1].toFloat())
            } catch (e: Exception) {
                null
            }
        } else null
    }
    return Stroke(
        points = pointsList,
        colorValue = colorValue,
        strokeWidth = strokeWidth,
        isEraser = isEraser,
        isHighlighter = isHighlighter,
        strokeType = strokeType
    )
}

fun Stroke.toDto(): StrokeDto {
    val pointsString = points.joinToString(";") { "${it.x},${it.y}" }
    return StrokeDto(
        points = pointsString,
        colorValue = colorValue,
        strokeWidth = strokeWidth,
        isEraser = isEraser,
        isHighlighter = isHighlighter,
        strokeType = strokeType
    )
}
