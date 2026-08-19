package com.nexus.launcher.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nexus.launcher.ui.theme.NexusColor
import com.nexus.launcher.ui.theme.NexusRadius
import com.nexus.launcher.ui.theme.NexusType

/** Filter chip used across every hub header. */
@Composable
fun NexusChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(NexusRadius.Chip)
    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (selected) {
                    Modifier
                        .background(NexusColor.ChipActiveBg)
                        .border(1.dp, NexusColor.ChipActiveBorder, shape)
                } else {
                    Modifier.border(1.dp, NexusColor.Border, shape)
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = label,
            style = NexusType.Chip,
            color = if (selected) NexusColor.ChipActiveText else NexusColor.TextSecondary,
            maxLines = 1,
        )
    }
}

@Composable
fun ChipRow(
    labels: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        labels.forEachIndexed { index, label ->
            NexusChip(label = label, selected = index == selectedIndex) { onSelect(index) }
        }
    }
}

/** Standard hub card surface: dark fill, hairline border, large radius. */
@Composable
fun NexusCard(
    modifier: Modifier = Modifier,
    background: Color = NexusColor.Card,
    border: Color = NexusColor.Border,
    radius: Dp = NexusRadius.CardLarge,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(background)
            .border(BorderStroke(1.dp, border), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        content()
    }
}

/** The page header every hub shares: Baloo title plus an optional trailing action. */
@Composable
fun HubHeader(
    title: String,
    modifier: Modifier = Modifier,
    accent: Color = NexusColor.Accent,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = NexusType.PageTitle, color = accent)
        trailing?.invoke()
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, color: Color = NexusColor.TextPrimary) {
    Text(text = text, style = NexusType.SectionHeader, color = color, modifier = modifier)
}

/** Thin progress bar in the accent colour, as used on cards throughout. */
@Composable
fun NexusProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = NexusColor.Accent,
    track: Color = NexusColor.Border,
    height: Dp = 4.dp,
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(CircleShape)
            .background(track)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(height)
                .clip(CircleShape)
                .background(color)
        )
    }
}

/** Small pill badge (LIVE, PLEX, "On track", system names, …). */
@Composable
fun Badge(
    text: String,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
    radius: Dp = 6.dp,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(background)
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(text = text, style = NexusType.Badge, color = foreground, maxLines = 1)
    }
}

/**
 * Stand-in artwork for cards whose real image has not been scraped or fetched
 * yet. Derives a stable two-stop gradient from the title so a given game or
 * album always looks the same between launches.
 */
@Composable
fun GradientArt(
    seed: String,
    modifier: Modifier = Modifier,
    radius: Dp = NexusRadius.Card,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(gradientFor(seed))
    )
}

fun gradientFor(seed: String): Brush {
    val palette = listOf(
        Color(0xFF2A9D8F) to Color(0xFF0D2F2A),
        Color(0xFF8B5CF6) to Color(0xFF241245),
        Color(0xFF7A3CE0) to Color(0xFF3D1414),
        Color(0xFF7DC4E8) to Color(0xFF1C3648),
        Color(0xFF1A7F8A) to Color(0xFF04191C),
        Color(0xFFD98A2B) to Color(0xFF3D2205),
        Color(0xFFE0503C) to Color(0xFF3D1010),
        Color(0xFF4A8A94) to Color(0xFF10262B),
    )
    val hash = seed.fold(7) { acc, c -> acc * 31 + c.code }
    val (start, end) = palette[(hash and 0x7FFFFFFF) % palette.size]
    return Brush.linearGradient(listOf(start, end))
}

@Composable
fun EllipsizedText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
) {
    Text(
        text = text,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** Rounded app-icon container in the design's blush squircle. */
@Composable
fun Squircle(
    size: Dp,
    modifier: Modifier = Modifier,
    background: Color = NexusColor.IconSquircle,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.34f))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
