package com.nexus.launcher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nexus.launcher.R

/** Baloo 2 — clock and page titles. */
val Baloo2 = FontFamily(
    Font(R.font.baloo2_400, FontWeight.Normal),
    Font(R.font.baloo2_700, FontWeight.Bold),
    Font(R.font.baloo2_800, FontWeight.ExtraBold),
)

/** Comic Neue — app labels, dates, pills. */
val ComicNeue = FontFamily(
    Font(R.font.comicneue_400, FontWeight.Normal),
    Font(R.font.comicneue_700, FontWeight.Bold),
)

/** Outfit — hub UI body text. */
val Outfit = FontFamily(
    Font(R.font.outfit_400, FontWeight.Normal),
    Font(R.font.outfit_500, FontWeight.Medium),
    Font(R.font.outfit_600, FontWeight.SemiBold),
    Font(R.font.outfit_700, FontWeight.Bold),
)

/**
 * Named styles matching the handoff's type scale. CSS px map 1:1 to sp — the
 * design was drawn at ~376dp wide, which is a typical folded portrait width.
 */
object NexusType {
    val Clock = TextStyle(
        fontFamily = Baloo2,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 78.sp,
        lineHeight = 70.sp,
        letterSpacing = (-4).sp,
    )
    val DateLine = TextStyle(
        fontFamily = ComicNeue,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
    )
    val AppLabel = TextStyle(
        fontFamily = ComicNeue,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
    )
    val AzLetter = TextStyle(
        fontFamily = ComicNeue,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
    )
    val ScrubBubble = TextStyle(
        fontFamily = Baloo2,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
    )
    val PageTitle = TextStyle(
        fontFamily = Baloo2,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
    )
    val SectionHeader = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
    )
    val CardTitle = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
    )
    val CardTitleSemi = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
    )
    val Body = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
    )
    val BodySmall = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
    )
    val Meta = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
    )
    val Caption = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
    )
    val Badge = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 9.sp,
    )
    val Chip = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
    )
    val Pill = TextStyle(
        fontFamily = ComicNeue,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
    )
    val StatusBar = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
    )
    val SearchPill = TextStyle(
        fontFamily = ComicNeue,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
    )
}

val NexusTypography = Typography(
    bodyLarge = NexusType.Body,
    bodyMedium = NexusType.BodySmall,
    bodySmall = NexusType.Meta,
    titleLarge = NexusType.PageTitle,
    titleMedium = NexusType.SectionHeader,
    titleSmall = NexusType.CardTitle,
    labelSmall = NexusType.Caption,
)
