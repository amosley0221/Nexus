package com.nexus.launcher.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Design tokens straight out of the Nexus handoff. Kept as a plain object rather
 * than a Material ColorScheme because the design is hand-specified per surface
 * and never derives colours from a seed.
 */
object NexusColor {
    // Accent family
    val Accent = Color(0xFFF6B1A4)
    val IconSquircle = Color(0xFFF6B3A6)
    val Glyph = Color(0xFF8C2320)
    val DeepMaroonText = Color(0xFF5C1F16)

    // Reds / maroons
    val ScrubBubble = Color(0xFFE0503C)
    val ActivePill = Color(0xFFE0503C)
    val MaroonSurface = Color(0xFF2A0F0D)
    val MaroonTranslucent = Color(0xEB2A0F0D) // rgba(42,15,13,0.92)

    // Hub surfaces
    val HubBackground = Color(0xFF0C0E14)
    val Card = Color(0xFF141A24)
    val Border = Color(0xFF232A36)
    val BorderSoft = Color(0xFF1A2130)

    // Discover neutrals
    val DiscoverBg = Color(0xFF121316)
    val DiscoverCard = Color(0xFF1C1E24)
    val DiscoverBorder = Color(0xFF2A2D36)

    // Text
    val TextPrimary = Color(0xFFE8EDF4)
    val TextSecondary = Color(0xFF8A94A6)
    val TextFaint = Color(0xFF5A6478)
    val OnWallpaper = Color(0xFFFFFFFF)

    // Chips
    val ChipActiveBg = Color(0xFF3D2621)
    val ChipActiveBorder = Color(0xFF5C3A32)
    val ChipActiveText = Accent

    // Semantic
    val Green = Color(0xFF7DE0A3)
    val GreenBg = Color(0xFF12331F)
    val Amber = Color(0xFFE0A835)
    val AmberBg = Color(0xFF332B12)
    val AmberSurface = Color(0xFF1F1A10)
    val AmberBorder = Color(0xFF3D3520)
    val Live = Color(0xFFE05656)
    val Negative = Color(0xFFF08A8A)
    val Star = Color(0xFFE0C035)

    // Source badges
    val PlexAmber = Color(0xFFE8A33D)
    val LocalBlue = Color(0xFF8AB0F0)

    // Filetype tiles
    val PdfBg = Color(0xFF3D1A1A)
    val PdfFg = Color(0xFFF08A8A)
    val XlsBg = Color(0xFF12331F)
    val XlsFg = Green
    val DocBg = Color(0xFF16233D)
    val DocFg = LocalBlue
    val PptBg = Color(0xFF2B1A3D)
    val PptFg = Color(0xFFB08AF0)

    // Scrims
    val OverscrollScrim = Color(0x730A0608) // rgba(10,6,8,0.45)
}
