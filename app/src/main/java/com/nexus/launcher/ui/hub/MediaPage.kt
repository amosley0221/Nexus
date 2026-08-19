package com.nexus.launcher.ui.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.launcher.ui.common.Badge
import com.nexus.launcher.ui.common.ChipRow
import com.nexus.launcher.ui.common.GradientArt
import com.nexus.launcher.ui.common.NexusProgressBar
import com.nexus.launcher.ui.theme.LocalWindowProfile
import com.nexus.launcher.ui.theme.NexusColor
import com.nexus.launcher.ui.theme.NexusRadius
import com.nexus.launcher.ui.theme.NexusType

/** A followed Twitch channel that is currently live. */
data class LiveChannel(
    val login: String,
    val displayName: String,
    val game: String,
    val viewers: Int,
)

/** An in-progress item from Plex or the local library. */
data class ContinueItem(
    val title: String,
    val source: String,
    val progress: Float,
)

data class LibraryItem(
    val title: String,
    val year: String,
    val rating: String,
)

private val TABS = listOf("Plex", "Local", "Streaming", "● Live")

@Composable
fun MediaPage(
    liveChannels: List<LiveChannel>,
    continueWatching: List<ContinueItem>,
    library: List<LibraryItem>,
    modifier: Modifier = Modifier,
    statusLeft: String = "",
    statusRight: String = "",
    plexConfigured: Boolean = false,
    twitchConfigured: Boolean = false,
    onChannelClick: (LiveChannel) -> Unit,
    onContinueClick: (ContinueItem) -> Unit,
    onLibraryClick: (LibraryItem) -> Unit,
    onStreamingClick: (String) -> Unit,
    onConfigure: () -> Unit,
    onSearch: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val profile = LocalWindowProfile.current

    HubScaffold(
        title = "Media",
        modifier = modifier,
        statusLeft = statusLeft,
        statusRight = statusRight,
        trailing = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search media",
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (tab == 2) {
                SectionRow(title = "Streaming apps")
                StreamingButtons(onClick = onStreamingClick)
                return@Column
            }

            // ● Live on Twitch — offline follows are hidden by design.
            SectionRow(
                title = "● Live on Twitch",
                linkText = if (twitchConfigured) "Following →" else "Connect →",
                onLinkClick = onConfigure,
            )
            if (liveChannels.isEmpty()) {
                HubEmptyState(
                    message = if (twitchConfigured) {
                        "Nobody you follow is live right now."
                    } else {
                        "Connect Twitch in Nexus Settings to see live follows."
                    },
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            } else {
                liveChannels.forEach { channel ->
                    LiveRow(channel = channel, onClick = { onChannelClick(channel) })
                }
                Text(
                    text = "tap → opens their stream in Twitch · offline follows hidden",
                    style = NexusType.Caption,
                    color = NexusColor.TextFaint,
                )
            }

            SectionRow(title = "Continue watching", linkText = "View all →")
            if (continueWatching.isEmpty()) {
                HubEmptyState(
                    message = if (plexConfigured) {
                        "Nothing in progress."
                    } else {
                        "Add your Plex server in Nexus Settings."
                    },
                    action = if (plexConfigured) null else "Open settings",
                    onAction = onConfigure,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    continueWatching.take(profile.hubGridColumns).forEach { item ->
                        ContinueCard(
                            item = item,
                            modifier = Modifier.weight(1f),
                            onClick = { onContinueClick(item) },
                        )
                    }
                }
            }

            SectionRow(title = "Plex library", linkText = "Browse →", onLinkClick = onConfigure)
            if (library.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    library.take(profile.hubGridColumns).forEach { item ->
                        PosterCard(
                            item = item,
                            modifier = Modifier.weight(1f),
                            onClick = { onLibraryClick(item) },
                        )
                    }
                }
            }

            StreamingButtons(onClick = onStreamingClick)
        }
    }
}

@Composable
private fun LiveRow(channel: LiveChannel, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NexusRadius.Card))
            .background(NexusColor.Card)
            .border(1.dp, NexusColor.Border, RoundedCornerShape(NexusRadius.Card))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .border(2.dp, NexusColor.Live, CircleShape)
                .padding(2.dp)
        ) {
            GradientArt(
                seed = channel.login,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                radius = 16.dp,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = channel.displayName,
                style = NexusType.CardTitle,
                color = NexusColor.TextPrimary,
                maxLines = 1,
            )
            Text(
                text = "${channel.game} · ${formatViewers(channel.viewers)} watching",
                style = NexusType.Meta,
                color = NexusColor.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Badge(text = "LIVE", background = NexusColor.Live, foreground = Color.White)
    }
}

@Composable
private fun ContinueCard(item: ContinueItem, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val (badgeBg, badgeFg) = when (item.source.uppercase()) {
        "PLEX" -> NexusColor.PlexAmber to Color.Black
        else -> NexusColor.LocalBlue to Color.Black
    }
    Box(
        modifier = modifier
            .aspectRatio(1.5f)
            .clip(RoundedCornerShape(NexusRadius.Card))
            .clickable(onClick = onClick),
    ) {
        GradientArt(seed = item.title, modifier = Modifier.fillMaxWidth().height(200.dp))
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.title,
                    style = NexusType.CardTitle,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Badge(text = item.source.uppercase(), background = badgeBg, foreground = badgeFg)
            }
            Spacer(Modifier.height(6.dp))
            NexusProgressBar(progress = item.progress, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PosterCard(item: LibraryItem, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier = modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.4f)
                .clip(RoundedCornerShape(NexusRadius.Card)),
        ) {
            GradientArt(seed = item.title, modifier = Modifier.fillMaxWidth().height(160.dp))
            Text(
                text = item.title,
                style = NexusType.CardTitle,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(item.year, style = NexusType.Meta, color = NexusColor.TextSecondary)
            Text("★ ${item.rating}", style = NexusType.Meta, color = NexusColor.Star)
        }
    }
}

@Composable
private fun StreamingButtons(onClick: (String) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        listOf("Netflix", "Disney+", "Prime").forEach { name ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(NexusRadius.ButtonSmall))
                    .border(1.dp, NexusColor.Border, RoundedCornerShape(NexusRadius.ButtonSmall))
                    .clickable { onClick(name) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$name ↗",
                    style = NexusType.BodySmall.copy(fontWeight = FontWeight.Medium),
                    color = NexusColor.TextPrimary,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun formatViewers(count: Int): String = when {
    count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000f)
    count >= 1_000 -> String.format("%.1fk", count / 1_000f)
    else -> count.toString()
}
