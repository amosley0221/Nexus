package com.nexus.launcher.ui.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.launcher.ui.common.GradientArt
import com.nexus.launcher.ui.theme.NexusColor
import com.nexus.launcher.ui.theme.NexusRadius
import com.nexus.launcher.ui.theme.NexusType

/** A card in the Discover feed, as delivered by the companion app. */
data class DiscoverStory(
    val id: String,
    val source: String,
    val title: String,
    val snippet: String,
    val url: String,
    val hero: Boolean = false,
)

data class DiscoverChip(val label: String, val value: String)

/**
 * The leftmost page. Google's feed is only reachable through the LauncherClient
 * overlay protocol, which needs a separately-signed companion APK — until that
 * is installed this page says so plainly rather than faking a feed.
 */
@Composable
fun DiscoverPage(
    stories: List<DiscoverStory>,
    chips: List<DiscoverChip>,
    overlayUsable: Boolean,
    statusTitle: String,
    statusDetail: String,
    modifier: Modifier = Modifier,
    onStoryClick: (DiscoverStory) -> Unit,
    onCompanionInfo: () -> Unit,
) {
    if (overlayUsable) {
        // The Google app renders the feed into its own window, parented to the
        // launcher window and drawn *behind* it. So this page must paint
        // nothing at all — no background, no header, not even the title. Any
        // opaque pixel here covers the feed and the page reads as blank.
        Box(modifier = modifier.fillMaxSize())
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NexusColor.DiscoverBg)
            .padding(horizontal = 20.dp)
            .padding(top = 22.dp, bottom = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Discover",
                style = NexusType.PageTitle,
                color = NexusColor.TextPrimary,
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(NexusColor.DiscoverCard)
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            chips.forEach { chip ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(NexusRadius.Card))
                        .background(NexusColor.DiscoverCard)
                        .border(1.dp, NexusColor.DiscoverBorder, RoundedCornerShape(NexusRadius.Card))
                        .padding(12.dp),
                ) {
                    Text(chip.label, style = NexusType.Meta, color = NexusColor.TextSecondary)
                    Text(chip.value, style = NexusType.CardTitle, color = NexusColor.TextPrimary)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(NexusRadius.Card))
                .background(NexusColor.DiscoverCard)
                .border(1.dp, NexusColor.DiscoverBorder, RoundedCornerShape(NexusRadius.Card))
                .clickable(onClick = onCompanionInfo)
                .padding(14.dp),
        ) {
            Column {
                Text(
                    text = statusTitle,
                    style = NexusType.CardTitle,
                    color = NexusColor.TextPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = statusDetail,
                    style = NexusType.Meta,
                    color = NexusColor.TextSecondary,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(stories, key = { it.id }) { story ->
                if (story.hero) {
                    HeroStory(story = story, onClick = { onStoryClick(story) })
                } else {
                    CompactStory(story = story, onClick = { onStoryClick(story) })
                }
            }
        }
    }
}

@Composable
private fun HeroStory(story: DiscoverStory, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NexusRadius.Card))
            .background(NexusColor.DiscoverCard)
            .border(1.dp, NexusColor.DiscoverBorder, RoundedCornerShape(NexusRadius.Card))
            .clickable(onClick = onClick),
    ) {
        GradientArt(
            seed = story.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp),
            radius = 0.dp,
        )
        Column(modifier = Modifier.padding(14.dp)) {
            Text(story.source, style = NexusType.Meta, color = NexusColor.TextSecondary)
            Spacer(Modifier.height(4.dp))
            Text(
                text = story.title,
                style = NexusType.SectionHeader.copy(fontSize = 16.sp),
                color = NexusColor.TextPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = story.snippet,
                style = NexusType.BodySmall,
                color = NexusColor.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CompactStory(story: DiscoverStory, onClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NexusRadius.Thumb))
            .background(NexusColor.DiscoverCard)
            .border(1.dp, NexusColor.DiscoverBorder, RoundedCornerShape(NexusRadius.Thumb))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(story.source, style = NexusType.Meta, color = NexusColor.TextSecondary)
            Spacer(Modifier.height(3.dp))
            Text(
                text = story.title,
                style = NexusType.CardTitle,
                color = NexusColor.TextPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        GradientArt(
            seed = story.id,
            modifier = Modifier.size(74.dp),
            radius = NexusRadius.Thumb,
        )
    }
}
