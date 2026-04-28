package com.vibenote.app.core.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class VibeColorsTest {

    @Test
    fun `dark palette values are correct`() {
        val dark = VibeColors.Dark
        assertEquals(Color(0xFF0F0F0F), dark.backgroundDark)
        assertEquals(Color(0xFF3ECF8E), dark.brandGreen)
        assertEquals(Color(0xFFFAFAFA), dark.textPrimaryDark)
        assertEquals(Color(0xFF242424), dark.borderSubtle)
        // Derived properties should resolve to dark values
        assertEquals(dark.backgroundDark, dark.background)
        assertEquals(dark.textPrimaryDark, dark.textPrimary)
        assertEquals(dark.borderStandardDark, dark.borderStandard)
    }

    @Test
    fun `light palette values are correct`() {
        val light = VibeColors.Light
        assertEquals(Color(0xFFF8F9FA), light.backgroundLight)
        assertEquals(Color(0xFF3ECF8E), light.brandGreen)
        assertEquals(Color(0xFF171717), light.textPrimaryLight)
        assertEquals(Color(0xFFE9ECEF), light.borderSubtle)
        // Derived properties should resolve to light values
        assertEquals(light.backgroundLight, light.background)
        assertEquals(light.textPrimaryLight, light.textPrimary)
        assertEquals(light.borderStandardLight, light.borderStandard)
    }

    @Test
    fun `backward compatibility getters match dark theme`() {
        assertEquals(VibeColors.Dark.backgroundDark, VibeColors.BackgroundDark)
        assertEquals(VibeColors.Dark.brandGreen, VibeColors.BrandGreen)
        assertEquals(VibeColors.Dark.textPrimaryDark, VibeColors.TextPrimary)
        assertEquals(VibeColors.Dark.borderStandardDark, VibeColors.BorderStandard)
    }

    @Test
    fun `shapes match design specifications`() {
        // PillShape should have a very large radius (e.g. 9999.dp)
        // CardShape should have 16.dp radius
        // SmallShape should have 6.dp radius
        assertEquals(9999.dp, VibeShapes.PillShape.topStart)
        assertEquals(16.dp, VibeShapes.CardShape.topStart)
        assertEquals(6.dp, VibeShapes.SmallShape.topStart)
    }
}
