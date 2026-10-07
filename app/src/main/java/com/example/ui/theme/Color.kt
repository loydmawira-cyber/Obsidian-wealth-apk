package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * The colours that change between themes. Every themed colour below (ObsidianBg, SovereignGold,
 * TextPrimary...) reads from [ActivePalette], so switching theme restyles the whole app without
 * touching the screens. Reading a themed colour inside a composable or draw block recomposes it
 * when the palette changes.
 */
data class ObsidianPalette(
    val bg: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceElevated: Color,
    val border: Color,
    val borderSubtle: Color,
    val gold: Color,
    val goldLight: Color,
    val goldBright: Color,
    val goldDark: Color,
    val goldGlow: Color,
    val goldSurface: Color,
    val goldSurfaceElevated: Color,
    val goldBorder: Color,
    val goldBorderSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textMuted: Color,
    val cardGradientStart: Color,
    val cardGradientEnd: Color,
    val goldGradientStart: Color,
    val goldGradientEnd: Color
)

object ObsidianPalettes {
    /** The original dark "AI Studio" canvas with luminous amber gold. */
    val Classic = ObsidianPalette(
        bg = Color(0xFF131314),
        surface = Color(0xFF1E1F20),
        surfaceVariant = Color(0xFF282A2C),
        surfaceElevated = Color(0xFF333538),
        border = Color(0xFF3E3B30),
        borderSubtle = Color(0xFF2E2B22),
        gold = Color(0xFFFBBF24),
        goldLight = Color(0xFFFFDF70),
        goldBright = Color(0xFFFFEA9F),
        goldDark = Color(0xFFD97706),
        goldGlow = Color(0x3DFBBF24),
        goldSurface = Color(0xFF2A2312),
        goldSurfaceElevated = Color(0xFF382F18),
        goldBorder = Color(0x88FBBF24),
        goldBorderSubtle = Color(0x44FBBF24),
        textPrimary = Color(0xFFF0F0F0),
        textSecondary = Color(0xFFC4C7C5),
        textTertiary = Color(0xFF9E9E9E),
        textMuted = Color(0xFF757575),
        cardGradientStart = Color(0xFF2D2615),
        cardGradientEnd = Color(0xFF1E1F20),
        goldGradientStart = Color(0xFF3E3114),
        goldGradientEnd = Color(0xFF201D16)
    )

    /**
     * "Rich Remains" black and champagne gold, sampled from the reference artwork:
     * navy-black canvas (#010A14), navy cards (#06131F), champagne gold headings (#EEC27F),
     * pale-gold numbers (#F7DFA6), bronze glow (#C7945A), and near-white headline text (#FBFEFF).
     */
    val RoyalGold = ObsidianPalette(
        bg = Color(0xFF010A14),
        surface = Color(0xFF06131F),
        surfaceVariant = Color(0xFF0D1B2A),
        surfaceElevated = Color(0xFF152638),
        border = Color(0xFF7A6642),
        borderSubtle = Color(0xFF2A2318),
        gold = Color(0xFFEEC27F),
        goldLight = Color(0xFFF7DFA6),
        goldBright = Color(0xFFFBE9BE),
        goldDark = Color(0xFFC7945A),
        goldGlow = Color(0x3DEEC27F),
        goldSurface = Color(0xFF17120B),
        goldSurfaceElevated = Color(0xFF241B0F),
        goldBorder = Color(0x99EEC27F),
        goldBorderSubtle = Color(0x44EEC27F),
        textPrimary = Color(0xFFFBFEFF),
        textSecondary = Color(0xFFD2CEC4),
        textTertiary = Color(0xFF9B978C),
        textMuted = Color(0xFF75726A),
        cardGradientStart = Color(0xFF1C150B),
        cardGradientEnd = Color(0xFF06131F),
        goldGradientStart = Color(0xFF2E2210),
        goldGradientEnd = Color(0xFF0C0A07)
    )
}

object ActivePalette {
    var current by mutableStateOf(ObsidianPalettes.Classic)
}

// ---- Themed colours (follow the active palette) ----
val ObsidianBg: Color get() = ActivePalette.current.bg
val ObsidianSurface: Color get() = ActivePalette.current.surface
val ObsidianSurfaceVariant: Color get() = ActivePalette.current.surfaceVariant
val ObsidianSurfaceElevated: Color get() = ActivePalette.current.surfaceElevated
val ObsidianBorder: Color get() = ActivePalette.current.border
val ObsidianBorderSubtle: Color get() = ActivePalette.current.borderSubtle

// Sovereign Gold (bullion, badges & highlights)
val SovereignGold: Color get() = ActivePalette.current.gold
val GoldLight: Color get() = ActivePalette.current.goldLight
val GoldBright: Color get() = ActivePalette.current.goldBright
val GoldDark: Color get() = ActivePalette.current.goldDark
val GoldGlow: Color get() = ActivePalette.current.goldGlow
val GoldSurface: Color get() = ActivePalette.current.goldSurface
val GoldSurfaceElevated: Color get() = ActivePalette.current.goldSurfaceElevated
val GoldBorder: Color get() = ActivePalette.current.goldBorder
val GoldBorderSubtle: Color get() = ActivePalette.current.goldBorderSubtle

// Typography hierarchy
val TextPrimary: Color get() = ActivePalette.current.textPrimary
val TextSecondary: Color get() = ActivePalette.current.textSecondary
val TextTertiary: Color get() = ActivePalette.current.textTertiary
val TextMuted: Color get() = ActivePalette.current.textMuted

// Card gradients
val CardGradientStart: Color get() = ActivePalette.current.cardGradientStart
val CardGradientEnd: Color get() = ActivePalette.current.cardGradientEnd
val GoldGradientStart: Color get() = ActivePalette.current.goldGradientStart
val GoldGradientEnd: Color get() = ActivePalette.current.goldGradientEnd

// ---- Fixed colours (same in every theme) ----
// Institutional Growth (Alpha, Inflows, Yields & Returns)
val EmeraldGrowth = Color(0xFF10B981)
val EmeraldLight = Color(0xFF34D399)
val EmeraldDark = Color(0xFF059669)
val EmeraldGlow = Color(0x3310B981)

// Financial Intelligence & Fixed Income (Cyan & Sapphire)
val CyanAccent = Color(0xFF38BDF8)
val CyanLight = Color(0xFF7DD3FC)
val CyanGlow = Color(0x3338BDF8)
val ElectricIndigo = Color(0xFF818CF8)
val IndigoLight = Color(0xFFA5B4FC)

// Risk & Liabilities (Liabilities, Debt, Outflows)
val CrimsonDebt = Color(0xFFF87171)
val CrimsonLight = Color(0xFFFCA5A5)
val CrimsonGlow = Color(0x33F87171)

val AmberWarning = Color(0xFFFBBF24)
val AmberGlow = Color(0x33FBBF24)
val PurpleAccent = Color(0xFFA78BFA)
