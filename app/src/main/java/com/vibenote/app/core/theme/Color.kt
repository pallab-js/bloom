package com.vibenote.app.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class VibeColors(
    val background: Color,
    val surface: Color,
    val brand: Color,
    val action: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val borderSubtle: Color,
    val borderStandard: Color,
    val borderHighlight: Color,
    val isDark: Boolean
) {
    companion object {
        val Dark = VibeColors(
            background = Color(0xFF171717),
            surface = Color(0xFF0F0F0F),
            brand = Color(0xFF3ECF8E),
            action = Color(0xFF00C573),
            textPrimary = Color(0xFFFAFAFA),
            textMuted = Color(0xFF898989),
            borderSubtle = Color(0xFF242424),
            borderStandard = Color(0xFF2E2E2E),
            borderHighlight = Color(0x4D3ECF8E),
            isDark = true
        )

        val Light = VibeColors(
            background = Color(0xFFF8F9FA),
            surface = Color(0xFFFFFFFF),
            brand = Color(0xFF3ECF8E),
            action = Color(0xFF00C573),
            textPrimary = Color(0xFF171717),
            textMuted = Color(0xFF6C757D),
            borderSubtle = Color(0xFFE9ECEF),
            borderStandard = Color(0xFFDEE2E6),
            borderHighlight = Color(0x4D3ECF8E),
            isDark = false
        )
        
        // Backward compatibility for existing references
        val BackgroundDark get() = Dark.background
        val SurfaceDeep get() = Dark.surface
        val BrandGreen get() = Dark.brand
        val ActionGreen get() = Dark.action
        val TextPrimary get() = Dark.textPrimary
        val TextMuted get() = Dark.textMuted
        val BorderSubtle get() = Dark.borderSubtle
        val BorderStandard get() = Dark.borderStandard
        val BorderHighlight get() = Dark.borderHighlight
    }
}

object VibeShapes {
    val PillShape = RoundedCornerShape(9999.dp)
    val SmallShape = RoundedCornerShape(6.dp)
    val CardShape = RoundedCornerShape(16.dp)
}
