package com.nexus.launcher.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.nexus.launcher.ui.common.ChipRow
import com.nexus.launcher.ui.common.GradientArt
import com.nexus.launcher.ui.hub.HubScaffold
import com.nexus.launcher.ui.theme.LocalWindowProfile
import com.nexus.launcher.ui.theme.NexusColor
import com.nexus.launcher.ui.theme.NexusRadius
import com.nexus.launcher.ui.theme.NexusType

/** A widget offered in the gallery — either a Nexus block or a system AppWidget. */
data class GalleryEntry(
    val type: String,
    val name: String,
    val category: String,
    val sizes: String,
    /** Set when this entry comes from an installed AppWidget provider. */
    val providerPackage: String? = null,
)

/** The built-in Nexus blocks, plus metadata used by the page editor. */
object WidgetCatalog {

    val builtIn = listOf(
        GalleryEntry("clock", "Clock & date", "Nexus", "2×1, 4×2"),
        GalleryEntry("favorites", "Favourites list", "Nexus", "List, Grid 2×"),
        GalleryEntry("claude", "Claude activity", "Nexus", "2×1, 2×2"),
        GalleryEntry("nowplaying", "Now playing", "Media", "4×1, 4×2"),
        GalleryEntry("continue", "Continue watching", "Media", "Row S, Cards M"),
        GalleryEntry("live", "Twitch live follows", "Media", "List"),
        GalleryEntry("budgetring", "Budget ring", "Budget", "2×2"),
        GalleryEntry("transactions", "Recent transactions", "Budget", "List"),
        GalleryEntry("duesoon", "Due soon", "Files", "4×1"),
        GalleryEntry("recentfiles", "Recent files", "Files", "List"),
        GalleryEntry("games", "Game covers", "Games", "Grid 2×, Grid 3×"),
        GalleryEntry("roms", "Emulator library", "Games", "Grid 2×"),
    )

    val categories = listOf("All", "Nexus", "Media", "Budget", "Files", "Games", "System")

    fun displayName(type: String): String =
        builtIn.firstOrNull { it.type == type }?.name ?: type.replaceFirstChar { it.uppercase() }

    fun description(type: String): String = when (type) {
        "clock" -> "Time, date, and weather line"
        "favorites" -> "Your Home favourites"
        "claude" -> "Running and blocked tasks"
        "nowplaying" -> "Current MediaSession"
        "continue" -> "Plex and local progress"
        "live" -> "Followed channels that are live"
        "budgetring" -> "Spend against the monthly limit"
        "transactions" -> "Latest entries"
        "duesoon" -> "Upcoming deadlines"
        "recentfiles" -> "Recently modified"
        "games" -> "Installed game covers"
        "roms" -> "Scanned ROM library"
        else -> "App widget"
    }
}

@Composable
fun WidgetGallery(
    appWidgets: List<GalleryEntry>,
    modifier: Modifier = Modifier,
    onPick: (GalleryEntry) -> Unit,
    onDone: () -> Unit,
) {
    var category by rememberSaveable { mutableIntStateOf(0) }
    val profile = LocalWindowProfile.current

    val all = remember(appWidgets) { WidgetCatalog.builtIn + appWidgets }
    val shown = remember(all, category) {
        val name = WidgetCatalog.categories[category]
        if (name == "All") all else all.filter { it.category == name }
    }

    HubScaffold(
        title = "Widgets",
        modifier = modifier,
        trailing = {
            Text(
                text = "Done",
                style = NexusType.CardTitle,
                color = NexusColor.Accent,
                modifier = Modifier.clickable(onClick = onDone),
            )
        },
    ) {
        ChipRow(
            labels = WidgetCatalog.categories,
            selectedIndex = category,
            onSelect = { category = it },
        )
        Spacer(Modifier.height(14.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(profile.hubGridColumns),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(shown, key = { it.type + it.name }) { entry ->
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(NexusRadius.Card))
                        .background(NexusColor.Card)
                        .border(1.dp, NexusColor.Border, RoundedCornerShape(NexusRadius.Card))
                        .clickable { onPick(entry) }
                        .padding(10.dp),
                ) {
                    GradientArt(
                        seed = entry.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = entry.name,
                        style = NexusType.Body.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        ),
                        color = NexusColor.TextPrimary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                    Text(
                        text = entry.sizes,
                        style = NexusType.Caption,
                        color = NexusColor.TextSecondary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
