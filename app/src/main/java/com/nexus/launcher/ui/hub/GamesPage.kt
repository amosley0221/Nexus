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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.launcher.domain.AppEntry
import com.nexus.launcher.domain.RomEntry
import com.nexus.launcher.integration.emulators.Emulators
import com.nexus.launcher.ui.common.ChipRow
import com.nexus.launcher.ui.common.dashedBorder
import com.nexus.launcher.ui.common.GradientArt
import com.nexus.launcher.ui.common.NexusChip
import com.nexus.launcher.ui.theme.LocalWindowProfile
import com.nexus.launcher.ui.theme.NexusColor
import com.nexus.launcher.ui.theme.NexusRadius
import com.nexus.launcher.ui.theme.NexusType
import java.util.concurrent.TimeUnit

private val TABS = listOf("◆ Installed", "Emulators", "Cloud", "♡")

@Composable
fun GamesPage(
    games: List<AppEntry>,
    roms: List<RomEntry>,
    favouriteGameKeys: Set<String>,
    modifier: Modifier = Modifier,
    statusLeft: String = "",
    statusRight: String = "",
    onLaunchGame: (AppEntry) -> Unit,
    onGameOptions: (AppEntry) -> Unit,
    onLaunchRom: (RomEntry) -> Unit,
    onRomOptions: (RomEntry) -> Unit,
    onScanFolder: () -> Unit,
    onSearch: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var systemFilter by rememberSaveable { mutableIntStateOf(0) }
    val profile = LocalWindowProfile.current

    HubScaffold(
        title = "Games",
        modifier = modifier,
        statusLeft = statusLeft,
        statusRight = statusRight,
        trailing = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search games",
                tint = NexusColor.TextSecondary,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onSearch),
            )
        },
    ) {
        ChipRow(labels = TABS, selectedIndex = tab, onSelect = { tab = it })
        Spacer(Modifier.height(16.dp))

        when (tab) {
            1 -> EmulatorsTab(
                roms = roms,
                systemFilter = systemFilter,
                onSystemFilter = { systemFilter = it },
                columns = profile.hubGridColumns,
                onLaunchRom = onLaunchRom,
                onRomOptions = onRomOptions,
                onScanFolder = onScanFolder,
                modifier = Modifier.weight(1f),
            )

            2 -> HubEmptyState(
                message = "No cloud gaming service connected.",
                action = "Cloud services are configured in Nexus Settings",
                modifier = Modifier.weight(1f),
            )

            3 -> InstalledTab(
                games = games.filter { it.key in favouriteGameKeys },
                columns = profile.hubGridColumns,
                onLaunch = onLaunchGame,
                onOptions = onGameOptions,
                emptyMessage = "No favourite games yet — long-press a game to add one.",
                modifier = Modifier.weight(1f),
            )

            else -> InstalledTab(
                games = games,
                columns = profile.hubGridColumns,
                onLaunch = onLaunchGame,
                onOptions = onGameOptions,
                emptyMessage = "No installed games found. Apps that declare the game category appear here.",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun InstalledTab(
    games: List<AppEntry>,
    columns: Int,
    onLaunch: (AppEntry) -> Unit,
    onOptions: (AppEntry) -> Unit,
    emptyMessage: String,
    modifier: Modifier = Modifier,
) {
    if (games.isEmpty()) {
        HubEmptyState(message = emptyMessage, modifier = modifier)
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        items(games, key = { it.key }) { game ->
            GameCard(
                title = game.label,
                accentLine = playtimeLabel(game.usageMillis),
                seed = game.packageName,
                onClick = { onLaunch(game) },
                onOptions = { onOptions(game) },
            )
        }
    }
}

@Composable
private fun GameCard(
    title: String,
    accentLine: String?,
    seed: String,
    onClick: () -> Unit,
    onOptions: () -> Unit,
) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        GradientArt(
            seed = seed,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.95f),
        )
        Spacer(Modifier.height(7.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = NexusType.Body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                    color = NexusColor.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (accentLine != null) {
                    Text(
                        text = accentLine,
                        style = NexusType.Meta,
                        color = NexusColor.Accent,
                        maxLines = 1,
                    )
                }
            }
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Options",
                tint = NexusColor.TextFaint,
                modifier = Modifier
                    .size(16.dp)
                    .clickable(onClick = onOptions),
            )
        }
    }
}

@Composable
private fun EmulatorsTab(
    roms: List<RomEntry>,
    systemFilter: Int,
    onSystemFilter: (Int) -> Unit,
    columns: Int,
    onLaunchRom: (RomEntry) -> Unit,
    onRomOptions: (RomEntry) -> Unit,
    onScanFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Only offer sub-chips for systems that actually have ROMs behind them.
    val systemsPresent = remember(roms) {
        listOf("All") + Emulators.systems
            .filter { system -> roms.any { it.system == system.id } }
            .map { it.label }
    }
    val activeSystemId = remember(systemFilter, systemsPresent) {
        systemsPresent.getOrNull(systemFilter)
            ?.let { label -> Emulators.systems.firstOrNull { it.label == label }?.id }
    }
    val shown = remember(roms, activeSystemId) {
        if (activeSystemId == null) roms else roms.filter { it.system == activeSystemId }
    }

    Column(modifier = modifier) {
        ChipRow(
            labels = systemsPresent,
            selectedIndex = systemFilter.coerceIn(0, systemsPresent.lastIndex),
            onSelect = onSystemFilter,
        )
        Spacer(Modifier.height(14.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(shown, key = { it.uri }) { rom ->
                RomCard(
                    rom = rom,
                    onClick = { onLaunchRom(rom) },
                    onOptions = { onRomOptions(rom) },
                )
            }
            item {
                ScanFolderCard(onClick = onScanFolder)
            }
        }
    }
}

@Composable
private fun RomCard(rom: RomEntry, onClick: () -> Unit, onOptions: () -> Unit) {
    val systemLabel = remember(rom.system) {
        Emulators.systems.firstOrNull { it.id == rom.system }?.label ?: rom.system.uppercase()
    }
    val emulatorLabel = remember(rom.preferredEmulator) {
        rom.preferredEmulator?.let { id -> Emulators.targets.firstOrNull { it.id == id }?.label }
    }

    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.95f),
        ) {
            GradientArt(seed = rom.displayName, modifier = Modifier.fillMaxSize())

            // System badge, top-left
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(7.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            ) {
                Text(text = systemLabel, style = NexusType.Badge, color = NexusColor.OnWallpaper)
            }

            // Play FAB, bottom-right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(7.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(NexusColor.IconSquircle.copy(alpha = 0.92f))
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play",
                    tint = NexusColor.Glyph,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(Modifier.height(7.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rom.displayName,
                    style = NexusType.Body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                    color = NexusColor.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = emulatorLabel ?: "Tap to choose emulator",
                    style = NexusType.Meta,
                    color = NexusColor.Accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Options",
                tint = NexusColor.TextFaint,
                modifier = Modifier
                    .size(16.dp)
                    .clickable(onClick = onOptions),
            )
        }
    }
}

/** Dashed final cell that opens the SAF folder picker. */
@Composable
private fun ScanFolderCard(onClick: () -> Unit) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.95f)
                .clip(RoundedCornerShape(NexusRadius.Card))
                .dashedBorder(NexusColor.Border, NexusRadius.Card)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Scan folder\nfor ROMs",
                style = NexusType.Meta,
                color = NexusColor.TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = "Auto-fetch box art",
            style = NexusType.Meta,
            color = NexusColor.TextFaint,
            maxLines = 1,
        )
    }
}

private fun playtimeLabel(usageMillis: Long): String? {
    if (usageMillis <= 0) return null
    val hours = TimeUnit.MILLISECONDS.toHours(usageMillis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(usageMillis) % 60
    return if (hours > 0) "● ${hours}h ${minutes.toString().padStart(2, '0')}m" else "● ${minutes}m"
}
