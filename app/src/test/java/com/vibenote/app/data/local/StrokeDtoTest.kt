package com.vibenote.app.data.local

import androidx.compose.ui.geometry.Offset
import com.vibenote.app.domain.model.StrokeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokeDtoTest {

    @Test
    fun `toDomain handles normal data correctly`() {
        val dto = StrokeDto(
            points = "0.0,0.0;10.0,10.0;20.0,20.0",
            colorValue = 0xFF000000.toInt()
        )
        val stroke = dto.toDomain()
        
        assertEquals(3, stroke.points.size)
        assertEquals(Offset(0f, 0f), stroke.points[0])
        assertEquals(Offset(10f, 10f), stroke.points[1])
        assertEquals(Offset(20f, 20f), stroke.points[2])
    }

    @Test
    fun `toDomain handles malformed coordinates gracefully`() {
        val dto = StrokeDto(
            points = "0.0,0.0;invalid;10.0,10.0;bad,data;20.0,20.0"
        )
        val stroke = dto.toDomain()
        
        assertEquals(3, stroke.points.size)
        assertEquals(Offset(0f, 0f), stroke.points[0])
        assertEquals(Offset(10f, 10f), stroke.points[1])
        assertEquals(Offset(20f, 20f), stroke.points[2])
    }

    @Test
    fun `toDomain handles empty points string`() {
        val dto = StrokeDto(points = "")
        val stroke = dto.toDomain()
        
        assertTrue(stroke.points.isEmpty())
    }

    @Test
    fun `toDomain limits points to prevent OOM - large string`() {
        val largePoints = "0.0,0.0;".repeat(50_000)
        val dto = StrokeDto(points = largePoints)
        val stroke = dto.toDomain()
        
        assertTrue(stroke.points.isEmpty())
    }

    @Test
    fun `toDomain limits points count to 5000`() {
        val manyPoints = (0..6000).joinToString(";") { "$it.0,$it.0" }
        val dto = StrokeDto(points = manyPoints)
        val stroke = dto.toDomain()
        
        assertTrue(stroke.points.size <= 5000)
    }

    @Test
    fun `toDto converts stroke correctly`() {
        val stroke = com.vibenote.app.domain.model.Stroke(
            points = listOf(Offset(0f, 0f), Offset(10f, 10f)),
            colorValue = 0xFF000000.toInt(),
            strokeWidth = 5f,
            isEraser = false,
            isHighlighter = true,
            strokeType = StrokeType.LINE
        )
        val dto = stroke.toDto()
        
        assertEquals("0.0,0.0;10.0,10.0", dto.points)
        assertEquals(0xFF000000.toInt(), dto.colorValue)
        assertEquals(5f, dto.strokeWidth)
        assertEquals(false, dto.isEraser)
        assertEquals(true, dto.isHighlighter)
        assertEquals(StrokeType.LINE, dto.strokeType)
    }

    @Test
    fun `toDomain handles incomplete coordinate pairs`() {
        val dto = StrokeDto(points = "0.0,0.0;10.0;20.0,20.0")
        val stroke = dto.toDomain()
        
        assertEquals(2, stroke.points.size)
        assertEquals(Offset(0f, 0f), stroke.points[0])
        assertEquals(Offset(20f, 20f), stroke.points[1])
    }

    @Test
    fun `toDomain handles non-numeric values`() {
        val dto = StrokeDto(points = "abc,def;0.0,0.0")
        val stroke = dto.toDomain()
        
        assertEquals(1, stroke.points.size)
        assertEquals(Offset(0f, 0f), stroke.points[0])
    }
}
