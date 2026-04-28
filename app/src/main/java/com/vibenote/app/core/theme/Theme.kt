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
    primary = VibeColors.Dark.brandGreen,
    secondary = VibeColors.Dark.actionGreen,
    background = VibeColors.Dark.backgroundDark,
    surface = VibeColors.Dark.surfaceDark,
    onPrimary = VibeColors.Dark.textPrimaryDark,
    onSecondary = VibeColors.Dark.textPrimaryDark,
    onBackground = VibeColors.Dark.textPrimaryDark,
    onSurface = VibeColors.Dark.textPrimaryDark,
    surfaceVariant = VibeColors.Dark.backgroundDark,
    onSurfaceVariant = VibeColors.Dark.textMutedDark,
    outline = VibeColors.Dark.borderStandardDark
)

private val LightColorScheme = lightColorScheme(
    primary = VibeColors.Light.brandGreen,
    secondary = VibeColors.Light.actionGreen,
    background = VibeColors.Light.backgroundLight,
    surface = VibeColors.Light.surfaceLight,
    onPrimary = VibeColors.Light.textPrimaryLight,
    onSecondary = VibeColors.Light.textPrimaryLight,
    onBackground = VibeColors.Light.textPrimaryLight,
    onSurface = VibeColors.Light.textPrimaryLight,
    surfaceVariant = VibeColors.Light.backgroundLight,
    onSurfaceVariant = VibeColors.Light.textMutedLight,
    outline = VibeColors.Light.borderStandardLight
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
