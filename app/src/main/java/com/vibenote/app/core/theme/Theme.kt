package com.vibenote.app.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalVibeColors = staticCompositionLocalOf { VibeColors.Dark }

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

private val LightColorScheme = lightColorScheme(
    primary = VibeColors.BrandGreen,
    secondary = VibeColors.ActionGreen,
    background = VibeColors.Light.background,
    surface = VibeColors.Light.surface,
    onPrimary = VibeColors.Light.textPrimary,
    onSecondary = VibeColors.Light.textPrimary,
    onBackground = VibeColors.Light.textPrimary,
    onSurface = VibeColors.Light.textPrimary,
    surfaceVariant = VibeColors.Light.background,
    onSurfaceVariant = VibeColors.Light.textMuted,
    outline = VibeColors.Light.borderStandard
)

@Composable
fun VibeNoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) VibeColors.Dark else VibeColors.Light
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalVibeColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = VibeTypography,
            content = content
        )
    }
}
