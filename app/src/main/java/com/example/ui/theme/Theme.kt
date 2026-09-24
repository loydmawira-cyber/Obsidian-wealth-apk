package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.models.AccentColor
import com.example.data.models.ThemeMode

private fun darkSchemeFor(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = Color.Black,
    primaryContainer = accent.copy(alpha = 0.24f),
    onPrimaryContainer = accent,
    secondary = EmeraldGrowth,
    onSecondary = Color.Black,
    secondaryContainer = EmeraldGlow,
    onSecondaryContainer = EmeraldLight,
    tertiary = CyanAccent,
    onTertiary = Color.Black,
    tertiaryContainer = CyanGlow,
    onTertiaryContainer = CyanLight,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceTint = accent,
    outline = ObsidianBorder,
    outlineVariant = ObsidianBorderSubtle,
    error = CrimsonDebt,
    onError = Color.White,
    errorContainer = CrimsonGlow,
    onErrorContainer = CrimsonLight
)

// Light counterparts for the same tokens, kept close in feel to the dark "Obsidian" surfaces.
private val LightBg = Color(0xFFFAF9F6)
private val LightSurface = Color(0xFFFFFFFF)
private val LightSurfaceVariant = Color(0xFFF0EEE8)
private val LightBorder = Color(0xFFE2DFD6)
private val LightTextPrimary = Color(0xFF1B1C1E)
private val LightTextSecondary = Color(0xFF54524C)

private fun lightSchemeFor(accent: Color) = lightColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent.copy(alpha = 0.16f),
    onPrimaryContainer = Color(0xFF1B1C1E),
    secondary = EmeraldDark,
    onSecondary = Color.White,
    secondaryContainer = EmeraldGlow,
    onSecondaryContainer = EmeraldDark,
    tertiary = CyanAccent,
    onTertiary = Color.White,
    tertiaryContainer = CyanGlow,
    onTertiaryContainer = Color(0xFF0B3A55),
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    surfaceTint = accent,
    outline = LightBorder,
    outlineVariant = LightBorder,
    error = CrimsonDebt,
    onError = Color.White,
    errorContainer = CrimsonGlow,
    onErrorContainer = Color(0xFF7A1F1F)
)

@Composable
fun MyApplicationTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    accentColor: AccentColor = AccentColor.GOLD,
    dynamicColor: Boolean = false, // Enforce the Obsidian palette family over system dynamic color
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val accent = Color(accentColor.argb)
    val colorScheme = if (isDark) darkSchemeFor(accent) else lightSchemeFor(accent)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
