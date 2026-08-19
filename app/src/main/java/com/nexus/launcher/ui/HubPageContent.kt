package com.nexus.launcher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.launcher.NexusApp
import com.nexus.launcher.domain.PageConfig
import com.nexus.launcher.domain.PageKind
import com.nexus.launcher.ui.edit.BlankPage
import com.nexus.launcher.ui.hub.BudgetPage
import com.nexus.launcher.ui.hub.ContinueItem
import com.nexus.launcher.ui.hub.DiscoverChip
import com.nexus.launcher.ui.hub.DiscoverPage
import com.nexus.launcher.ui.hub.DiscoverStory
import com.nexus.launcher.ui.hub.FileEntry
import com.nexus.launcher.ui.hub.FilesPage
import com.nexus.launcher.ui.hub.GamesPage
import com.nexus.launcher.ui.hub.LibraryItem
import com.nexus.launcher.ui.hub.LiveChannel
import com.nexus.launcher.ui.hub.MediaPage
import com.nexus.launcher.ui.hub.MusicPage
import com.nexus.launcher.ui.hub.defaultMusicShortcuts
import androidx.compose.ui.platform.LocalContext

/** Renders whichever hub belongs to [page]. */
@Composable
fun HubPageContent(
    page: PageConfig,
    viewModel: LauncherViewModel,
    host: LauncherHost,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    when (page.kind) {
        PageKind.Games -> {
            val games by viewModel.games.collectAsStateWithLifecycle()
            val roms by viewModel.roms.collectAsStateWithLifecycle()
            GamesPage(
                games = games,
                roms = roms,
                favouriteGameKeys = settings.favorites.toSet(),
                modifier = modifier,
                onLaunchGame = { host.launchApp(it, null) },
                onGameOptions = { host.openAppOptions(it) },
                onLaunchRom = { host.launchRom(it) },
                onRomOptions = { host.openRomOptions(it) },
                onScanFolder = { host.pickRomFolder() },
                onSearch = { host.openSearch() },
            )
        }

        PageKind.Media -> {
            MediaPage(
                liveChannels = emptyList(),
                continueWatching = emptyList(),
                library = emptyList(),
                plexConfigured = settings.plexServerUrl.isNotBlank(),
                twitchConfigured = settings.twitchToken.isNotBlank(),
                modifier = modifier,
                onChannelClick = { host.openDeepLink("twitch:${it.login}") },
                onContinueClick = { host.openDeepLink("plex:") },
                onLibraryClick = { host.openDeepLink("plex:") },
                onStreamingClick = { host.openDeepLink("stream:$it") },
                onConfigure = { host.openSettings() },
                onSearch = { host.openSearch() },
            )
        }

        PageKind.Music -> {
            val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
            val app = context.applicationContext as NexusApp
            MusicPage(
                nowPlaying = nowPlaying,
                shortcuts = remember { defaultMusicShortcuts() },
                downloadedCount = 0,
                modifier = modifier,
                onPlayPause = { app.nowPlaying.playPause() },
                onNext = { app.nowPlaying.next() },
                onPrevious = { app.nowPlaying.previous() },
                onShortcutClick = { host.openDeepLink("applemusic:${it.path}") },
                onOpenAppleMusic = { host.openDeepLink("applemusic:") },
                onSearch = { host.openSearch() },
            )
        }

        PageKind.Budget -> {
            val budget by viewModel.budget.collectAsStateWithLifecycle()
            BudgetPage(
                budget = budget,
                modifier = modifier,
                onAddTransaction = { host.openSettings() },
                onPeriodClick = { },
            )
        }

        PageKind.Files -> {
            FilesPage(
                files = emptyList<FileEntry>(),
                dueItems = emptyList(),
                freeSpaceLabel = remember { freeSpaceLabel() },
                modifier = modifier,
                onFileClick = { host.openDeepLink("file:${it.uri}") },
                onStarToggle = { },
                onPickFolder = { host.pickRomFolder() },
            )
        }

        PageKind.Discover -> {
            DiscoverPage(
                stories = emptyList<DiscoverStory>(),
                chips = emptyList<DiscoverChip>(),
                companionInstalled = false,
                modifier = modifier,
                onStoryClick = { host.openDeepLink(it.url) },
                onCompanionInfo = { host.openSettings() },
            )
        }

        PageKind.Blank -> {
            val app = context.applicationContext as NexusApp
            BlankPage(
                page = page,
                widgetHost = app.widgetHost,
                modifier = modifier,
                onAddAt = { _, _ -> host.openEditMode() },
                onWidgetLongPress = { host.openEditMode() },
            )
        }

        PageKind.Home -> Unit // handled by LauncherScreen
    }
}

private fun freeSpaceLabel(): String {
    val stat = android.os.StatFs(android.os.Environment.getDataDirectory().path)
    val freeGb = stat.availableBytes / 1_073_741_824.0
    return "%.0f GB free".format(freeGb)
}
