package com.vibenote.app.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Design‑system colour palette derived from DESIGN.md.
 * Provides both dark and light variants and a set of compatibility getters
 * for legacy code that still references the old field names.
 */
data class VibeColors(
    val backgroundDark: Color,
    val backgroundLight: Color,
    val surfaceDark: Color,
    val surfaceLight: Color,
    val brandGreen: Color,
    val actionGreen: Color,
    val textPrimaryDark: Color,
    val textPrimaryLight: Color,
    val textMutedDark: Color,
    val textMutedLight: Color,
    val borderSubtle: Color,
    val borderStandardDark: Color,
    val borderStandardLight: Color,
    val borderHighlight: Color,
    val isDark: Boolean
) {
    // Convenient derived properties used throughout the UI
    val background: Color get() = if (isDark) backgroundDark else backgroundLight
    val surface: Color get() = if (isDark) surfaceDark else surfaceLight
    val textPrimary: Color get() = if (isDark) textPrimaryDark else textPrimaryLight
    val textMuted: Color get() = if (isDark) textMutedDark else textMutedLight
    val borderStandard: Color get() = if (isDark) borderStandardDark else borderStandardLight

    companion object {
        // Dark palette – matches the Supabase‑inspired dark theme
        val Dark = VibeColors(
            backgroundDark = Color(0xFF0F0F0F), // primary button background
            backgroundLight = Color(0xFF171717), // page background
            surfaceDark = Color(0xFF0F0F0F),
            surfaceLight = Color(0xFF1A1A1A),
            brandGreen = Color(0xFF3ECF8E),
            actionGreen = Color(0xFF00C573),
            textPrimaryDark = Color(0xFFFAFAFA),
            textPrimaryLight = Color(0xFFFAFAFA),
            textMutedDark = Color(0xFF898989),
            textMutedLight = Color(0xFF898989),
            borderSubtle = Color(0xFF242424),
            borderStandardDark = Color(0xFF2E2E2E),
            borderStandardLight = Color(0xFF363636),
            borderHighlight = Color(0x4D3ECF8E), // rgba(62,207,142,0.3)
            isDark = true
        )

        // Light palette – the light counterpart of the design system
        val Light = VibeColors(
            backgroundDark = Color(0xFF171717),
            backgroundLight = Color(0xFFF8F9FA),
            surfaceDark = Color(0xFFFFFFFF),
            surfaceLight = Color(0xFFFFFFFF),
            brandGreen = Color(0xFF3ECF8E),
            actionGreen = Color(0xFF00C573),
            textPrimaryDark = Color(0xFF171717),
            textPrimaryLight = Color(0xFF171717),
            textMutedDark = Color(0xFF6C757D),
            textMutedLight = Color(0xFF6C757D),
            borderSubtle = Color(0xFFE9ECEF),
            borderStandardDark = Color(0xFFDEE2E6),
            borderStandardLight = Color(0xFFDEE2E6),
            borderHighlight = Color(0x4D3ECF8E),
            isDark = false
        )

        // ---------------------------------------------------------------------
        // Backward‑compatibility getters – keep existing UI code functional.
        // ---------------------------------------------------------------------
        val BackgroundDark get() = Dark.backgroundDark
        val BackgroundLight get() = Light.backgroundLight
        val SurfaceDeep get() = Dark.surfaceDark
        val SurfaceLight get() = Light.surfaceLight
        val BrandGreen get() = Dark.brandGreen
        val ActionGreen get() = Dark.actionGreen
        val TextPrimary get() = Dark.textPrimaryDark
        val TextMuted get() = Dark.textMutedDark
        val BorderSubtle get() = Dark.borderSubtle
        val BorderStandard get() = Dark.borderStandardDark
        val BorderHighlight get() = Dark.borderHighlight
    }
}

object VibeShapes {
    val PillShape = RoundedCornerShape(9999.dp)
    val SmallShape = RoundedCornerShape(6.dp)
    val CardShape = RoundedCornerShape(16.dp)
}
