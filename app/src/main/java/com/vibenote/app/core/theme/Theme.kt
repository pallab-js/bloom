package com.vibenote.app.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = VibeColors.BrandGreen,
    secondary = VibeColors.ActionGreen,
    background = VibeColors.BackgroundDark,
    surface = VibeColors.SurfaceDeep,
    onPrimary = VibeColors.TextPrimary,
    onSecondary = VibeColors.TextPrimary,
    onBackground = VibeColors.TextPrimary,
    onSurface = VibeColors.TextPrimary,
    surfaceVariant = VibeColors.BackgroundDark,
    onSurfaceVariant = VibeColors.TextMuted,
    outline = VibeColors.BorderStandard
)

@Composable
fun VibeNoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = VibeTypography,
        content = content
    )
}