package com.nexus.launcher.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp
import com.nexus.launcher.ui.layout.WindowProfile

/** Radii from the handoff: cards 14–20, chips 16–18, squircles ~34% of size. */
object NexusRadius {
    val Card = 16.dp
    val CardLarge = 20.dp
    val CardSmall = 14.dp
    val Chip = 18.dp
    val ChipSmall = 16.dp
    val Button = 14.dp
    val ButtonSmall = 11.dp
    val Squircle = 13.dp
    val Thumb = 18.dp
    val SearchPill = 28.dp
    val Frame = 34.dp
}

private val NexusShapes = Shapes(
    extraSmall = RoundedCornerShape(NexusRadius.ButtonSmall),
    small = RoundedCornerShape(NexusRadius.CardSmall),
    medium = RoundedCornerShape(NexusRadius.Card),
    large = RoundedCornerShape(NexusRadius.CardLarge),
    extraLarge = RoundedCornerShape(NexusRadius.Frame),
)

private val NexusColorScheme = darkColorScheme(
    primary = NexusColor.Accent,
    onPrimary = NexusColor.DeepMaroonText,
    secondary = NexusColor.IconSquircle,
    onSecondary = NexusColor.Glyph,
    background = NexusColor.HubBackground,
    onBackground = NexusColor.TextPrimary,
    surface = NexusColor.Card,
    onSurface = NexusColor.TextPrimary,
    surfaceVariant = NexusColor.DiscoverCard,
    onSurfaceVariant = NexusColor.TextSecondary,
    outline = NexusColor.Border,
    error = NexusColor.Negative,
)

/**
 * Current window profile (size class + fold posture). Provided once at the root
 * so any composable can adapt without threading it through every signature.
 */
val LocalWindowProfile: ProvidableCompositionLocal<WindowProfile> =
    compositionLocalOf { WindowProfile.Default }

@Composable
fun NexusTheme(
    windowProfile: WindowProfile = WindowProfile.Default,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalWindowProfile provides windowProfile) {
        MaterialTheme(
            colorScheme = NexusColorScheme,
            typography = NexusTypography,
            shapes = NexusShapes,
            content = content,
        )
    }
}
