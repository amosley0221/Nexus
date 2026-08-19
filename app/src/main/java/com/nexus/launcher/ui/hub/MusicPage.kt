package com.nexus.launcher.ui.hub

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.launcher.integration.media.NowPlaying
import com.nexus.launcher.ui.common.ChipRow
import com.nexus.launcher.ui.common.NexusProgressBar
import com.nexus.launcher.ui.theme.LocalWindowProfile
import com.nexus.launcher.ui.theme.NexusColor
import com.nexus.launcher.ui.theme.NexusRadius
import com.nexus.launcher.ui.theme.NexusType
import java.util.concurrent.TimeUnit

/** A shortcut card that deep-links into an Apple Music playlist or view. */
data class MusicShortcut(
    val title: String,
    val subtitle: String,
    val glyph: String,
    val accent: Color,
    val background: Color,
    val path: String,
)

private val TABS = listOf("⬇ Downloaded", "Playlists", "Albums", "Artists")

@Composable
fun MusicPage(
    nowPlaying: NowPlaying?,
    shortcuts: List<MusicShortcut>,
    downloadedCount: Int,
    modifier: Modifier = Modifier,
    statusLeft: String = "",
    statusRight: String = "",
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShortcutClick: (MusicShortcut) -> Unit,
    onOpenAppleMusic: () -> Unit,
    onSearch: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val profile = LocalWindowProfile.current

    HubScaffold(
        title = "Music",
        modifier = modifier,
        statusLeft = statusLeft,
        statusRight = statusRight,
        subtitle = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("♪ Apple Music ·", style = NexusType.BodySmall, color = NexusColor.TextSecondary)
                Text(
                    text = "⬇ $downloadedCount songs",
                    style = NexusType.BodySmall,
                    color = NexusColor.Green,
                )
            }
        },
        trailing = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search music",
                tint = NexusColor.TextSecondary,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onSearch),
            )
        },
    ) {
        ChipRow(labels = TABS, selectedIndex = tab, onSelect = { tab = it })
        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            NowPlayingCard(
                nowPlaying = nowPlaying,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
            )

            Spacer(Modifier.height(18.dp))
            SectionRow(
                title = "Downloaded from Apple Music",
                linkText = "View all →",
                onLinkClick = onOpenAppleMusic,
            )
            Spacer(Modifier.height(10.dp))

            // 2x2 (or wider) grid of coloured shortcut cards.
            shortcuts.chunked(profile.hubGridColumns).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                ) {
                    row.forEach { shortcut ->
                        ShortcutCard(
                            shortcut = shortcut,
                            modifier = Modifier.weight(1f),
                            onClick = { onShortcutClick(shortcut) },
                        )
                    }
                    repeat(profile.hubGridColumns - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            Text(
                text = "taps deep-link into Apple Music · Android exposes no downloaded-library API",
                style = NexusType.Caption,
                color = NexusColor.TextFaint,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun NowPlayingCard(
    nowPlaying: NowPlaying?,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(NexusRadius.CardLarge))
                .background(
                    Brush.linearGradient(listOf(Color(0xFF3B1E6E), Color(0xFF241245)))
                ),
            contentAlignment = Alignment.Center,
        ) {
            val artwork = nowPlaying?.artwork
            if (artwork != null) {
                Image(
                    painter = BitmapPainter(artwork.asImageBitmap()),
                    contentDescription = "Album artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = "now playing artwork",
                    style = NexusType.BodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nowPlaying?.title ?: "Nothing playing",
                    style = NexusType.PageTitle.copy(fontSize = 22.sp),
                    color = NexusColor.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = nowPlaying?.artist?.takeIf { it.isNotBlank() }
                        ?: "Start something in Apple Music",
                    style = NexusType.CardTitleSemi,
                    color = NexusColor.Accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text("♡", style = NexusType.SectionHeader, color = NexusColor.TextSecondary)
        }

        Spacer(Modifier.height(12.dp))
        NexusProgressBar(
            progress = nowPlaying?.progress ?: 0f,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatDuration(nowPlaying?.positionMillis ?: 0),
                style = NexusType.Meta,
                color = NexusColor.TextSecondary,
            )
            Text(
                text = formatDuration(nowPlaying?.durationMillis ?: 0),
                style = NexusType.Meta,
                color = NexusColor.TextSecondary,
            )
        }

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TransportIcon(Icons.Rounded.Shuffle, "Shuffle") {}
            TransportIcon(Icons.Rounded.SkipPrevious, "Previous", size = 28.dp, onClick = onPrevious)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(NexusColor.Accent)
                    .clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (nowPlaying?.isPlaying == true) {
                        Icons.Rounded.Pause
                    } else {
                        Icons.Rounded.PlayArrow
                    },
                    contentDescription = "Play or pause",
                    tint = NexusColor.DeepMaroonText,
                    modifier = Modifier.size(28.dp),
                )
            }
            TransportIcon(Icons.Rounded.SkipNext, "Next", size = 28.dp, onClick = onNext)
            TransportIcon(Icons.Rounded.QueueMusic, "Queue") {}
        }
    }
}

@Composable
private fun TransportIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    size: androidx.compose.ui.unit.Dp = 22.dp,
    onClick: () -> Unit,
) {
    Icon(
        imageVector = icon,
        contentDescription = description,
        tint = NexusColor.TextPrimary,
        modifier = Modifier
            .size(size)
            .clickable(onClick = onClick),
    )
}

@Composable
private fun ShortcutCard(shortcut: MusicShortcut, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .aspectRatio(1.55f)
            .clip(RoundedCornerShape(NexusRadius.Card))
            .background(shortcut.background)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "${shortcut.glyph} ${shortcut.title}",
            style = NexusType.CardTitle,
            color = shortcut.accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = shortcut.subtitle,
            style = NexusType.Meta.copy(fontWeight = FontWeight.Medium),
            color = NexusColor.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun formatDuration(millis: Long): String {
    if (millis <= 0) return "0:00"
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

/** The starter shortcut set, matching the design's four cards. */
fun defaultMusicShortcuts(): List<MusicShortcut> = listOf(
    MusicShortcut(
        title = "Favorites",
        subtitle = "⬇ 128 songs",
        glyph = "★",
        accent = Color(0xFFB08AF0),
        background = Color(0xFF241245),
        path = "library/songs",
    ),
    MusicShortcut(
        title = "Recently added",
        subtitle = "⬇ 36 songs",
        glyph = "◷",
        accent = Color(0xFF8AB0F0),
        background = Color(0xFF16233D),
        path = "library/recently-added",
    ),
    MusicShortcut(
        title = "Focus",
        subtitle = "⬇ playlist · 22 songs",
        glyph = "✦",
        accent = NexusColor.Green,
        background = Color(0xFF12331F),
        path = "library/playlists",
    ),
    MusicShortcut(
        title = "Chill vibes",
        subtitle = "⬇ playlist · 18 songs",
        glyph = "♨",
        accent = NexusColor.Amber,
        background = Color(0xFF3D2205),
        path = "library/playlists",
    ),
)
