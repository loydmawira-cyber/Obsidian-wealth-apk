package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ObsidianColorScheme = darkColorScheme(
    primary = SovereignGold,
    onPrimary = Color.Black,
    primaryContainer = GoldGlow,
    onPrimaryContainer = GoldLight,
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
    surfaceTint = SovereignGold,
    outline = ObsidianBorder,
    outlineVariant = ObsidianBorderSubtle,
    error = CrimsonDebt,
    onError = Color.White,
    errorContainer = CrimsonGlow,
    onErrorContainer = CrimsonLight
)


@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Enforce Obsidian luxury dark palette
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ObsidianColorScheme,
        typography = Typography,
        content = content
    )
}
