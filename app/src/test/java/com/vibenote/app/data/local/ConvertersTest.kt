package com.vibenote.app.data.local

import com.vibenote.app.domain.model.CanvasBackground
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `fromCanvasBackground converts correctly`() {
        assertEquals("dark", converters.fromCanvasBackground(CanvasBackground.DARK))
        assertEquals("white", converters.fromCanvasBackground(CanvasBackground.WHITE))
        assertEquals("lined", converters.fromCanvasBackground(CanvasBackground.LINED))
        assertEquals("dotted", converters.fromCanvasBackground(CanvasBackground.DOTTED))
        assertEquals("grid", converters.fromCanvasBackground(CanvasBackground.GRID))
    }

    @Test
    fun `toCanvasBackground converts correctly`() {
        assertEquals(CanvasBackground.DARK, converters.toCanvasBackground("dark"))
        assertEquals(CanvasBackground.WHITE, converters.toCanvasBackground("white"))
        assertEquals(CanvasBackground.LINED, converters.toCanvasBackground("lined"))
        assertEquals(CanvasBackground.DOTTED, converters.toCanvasBackground("dotted"))
        assertEquals(CanvasBackground.GRID, converters.toCanvasBackground("grid"))
    }

    @Test
    fun `toCanvasBackground handles invalid key`() {
        assertEquals(CanvasBackground.DARK, converters.toCanvasBackground("invalid"))
    }

    @Test
    fun `round trip conversion preserves value`() {
        val backgrounds = listOf(
            CanvasBackground.DARK,
            CanvasBackground.WHITE,
            CanvasBackground.LINED,
            CanvasBackground.DOTTED,
            CanvasBackground.GRID
        )
        
        backgrounds.forEach { background ->
            val key = converters.fromCanvasBackground(background)
            val converted = converters.toCanvasBackground(key)
            assertEquals(background, converted)
        }
    }
}
