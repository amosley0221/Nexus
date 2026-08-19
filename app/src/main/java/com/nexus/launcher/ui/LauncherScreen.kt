package com.nexus.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.launcher.domain.AppEntry
import com.nexus.launcher.domain.NotificationCard
import com.nexus.launcher.domain.PageKind
import com.nexus.launcher.domain.RomEntry
import com.nexus.launcher.NexusApp
import com.nexus.launcher.integration.notifications.NexusNotificationListener
import com.nexus.launcher.ui.home.HomePage
import com.nexus.launcher.ui.home.NotificationsOverscroll
import com.nexus.launcher.ui.home.PageJump
import com.nexus.launcher.ui.home.SearchOverscroll
import com.nexus.launcher.ui.theme.NexusColor
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

/** Which full-screen overlay, if any, is on top of the pager. */
enum class Overlay { None, Search, Notifications }

/**
 * The launcher shell: a horizontal pager over the configured pages, the page-jump
 * dot row, and the two overscroll layers. Long-pressing anywhere opens edit mode.
 */
@Composable
fun LauncherScreen(
    viewModel: LauncherViewModel,
    clockText: String,
    dateText: String,
    host: LauncherHost,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val activePages by viewModel.activePages.collectAsStateWithLifecycle()
    val apps by viewModel.visibleApps.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val recentlyInstalled by viewModel.recentlyInstalled.collectAsStateWithLifecycle()
    val notifications by NexusNotificationListener.cards.collectAsStateWithLifecycle()

    var overlay by remember { mutableStateOf(Overlay.None) }

    val homeIndex = remember(activePages) {
        activePages.indexOfFirst { it.kind == PageKind.Home }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(
        initialPage = homeIndex,
        pageCount = { activePages.size.coerceAtLeast(1) },
    )

    // Drive the Google Discover overlay from the pager. Nexus never draws the
    // feed — the Google app renders it into its own window over ours — so all
    // the launcher owes it is a scroll position: 1f when the Discover page is
    // fully in view, 0f once a full page away from it.
    val discoverIndex = remember(activePages) {
        activePages.indexOfFirst { it.kind == PageKind.Discover }
    }
    if (discoverIndex >= 0) {
        val discoverOverlay = remember(context) {
            (context.applicationContext as NexusApp).discoverOverlay
        }
        val discoverState by discoverOverlay.state.collectAsStateWithLifecycle()

        LaunchedEffect(pagerState, discoverState.isUsable, discoverIndex) {
            if (!discoverState.isUsable) return@LaunchedEffect
            snapshotFlow {
                val position = pagerState.currentPage + pagerState.currentPageOffsetFraction
                (1f - (position - discoverIndex).absoluteValue).coerceIn(0f, 1f)
            }.collect { progress -> discoverOverlay.onScroll(progress) }
        }

        LaunchedEffect(pagerState, discoverState.isUsable) {
            if (!discoverState.isUsable) return@LaunchedEffect
            snapshotFlow { pagerState.isScrollInProgress }.collect { scrolling ->
                if (scrolling) discoverOverlay.startScroll() else discoverOverlay.endScroll()
            }
        }
    }

    // Pages arrive from DataStore a beat after first composition, so the pager
    // starts on index 0. Settle it on Home once the real list is in.
    var settledOnHome by remember { mutableStateOf(false) }
    LaunchedEffect(activePages.size) {
        if (!settledOnHome && activePages.isNotEmpty()) {
            settledOnHome = true
            pagerState.scrollToPage(homeIndex)
        }
    }

    // HOME press returns to the Home page rather than exiting.
    LaunchedEffect(host.homePressCount) {
        if (host.homePressCount > 0) {
            overlay = Overlay.None
            pagerState.animateScrollToPage(homeIndex)
        }
    }

    BackHandler(enabled = overlay != Overlay.None || pagerState.currentPage != homeIndex) {
        if (overlay != Overlay.None) {
            overlay = Overlay.None
        } else {
            scope.launch { pagerState.animateScrollToPage(homeIndex) }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Long-press anywhere on a page opens edit mode.
                detectDragGesturesAfterLongPress(
                    onDragStart = { host.openEditMode() },
                    onDrag = { _, _ -> },
                )
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                beyondViewportPageCount = 1,
            ) { index ->
                val page = activePages.getOrNull(index) ?: return@HorizontalPager

                if (page.kind == PageKind.Home) {
                    HomePage(
                        apps = apps,
                        favorites = favorites,
                        settings = settings,
                        clockText = clockText,
                        dateText = dateText,
                        claudeLine = host.claudeLine,
                        onLaunch = { entry, bounds -> host.launchApp(entry, bounds) },
                        onLongPressApp = { host.openAppOptions(it) },
                        onClaudeClick = { host.openClaudeFeed() },
                        onClockClick = { host.openClock() },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    HubPageContent(
                        page = page,
                        viewModel = viewModel,
                        host = host,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            PageJump(
                pageNames = activePages.map { it.title },
                currentPage = pagerState.currentPage,
                onJump = { target -> scope.launch { pagerState.animateScrollToPage(target) } },
            )
        }

        // Overscroll layers sit above the pager.
        AnimatedVisibility(
            visible = overlay == Overlay.Search,
            enter = fadeIn() + slideInVertically { it / 4 },
            exit = fadeOut() + slideOutVertically { it / 4 },
        ) {
            SearchOverscroll(
                query = searchQuery,
                results = searchResults,
                recentlyInstalled = recentlyInstalled,
                settings = settings,
                onQueryChange = viewModel::setSearchQuery,
                onLaunch = { entry ->
                    overlay = Overlay.None
                    host.launchApp(entry, null)
                },
                onDismiss = { overlay = Overlay.None },
                modifier = Modifier.fillMaxSize(),
            )
        }

        AnimatedVisibility(
            visible = overlay == Overlay.Notifications,
            enter = fadeIn() + slideInVertically { -it / 4 },
            exit = fadeOut() + slideOutVertically { -it / 4 },
        ) {
            NotificationsOverscroll(
                cards = notifications,
                accessGranted = NexusNotificationListener.isEnabled(context),
                onCardClick = { card -> host.openNotification(card) },
                onGrantAccess = { NexusNotificationListener.requestAccess(context) },
                onDismiss = { overlay = Overlay.None },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Edge gestures that open the overscroll layers.
        OverscrollTriggers(
            enabled = overlay == Overlay.None,
            onPullUp = { overlay = Overlay.Search },
            onPullDown = { overlay = Overlay.Notifications },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * The launcher's outward-facing actions, implemented by the activity. Keeping
 * them behind an interface lets the composables stay free of Android plumbing.
 */
interface LauncherHost {
    val claudeLine: String?
    val homePressCount: Int

    fun launchApp(entry: AppEntry, bounds: androidx.compose.ui.geometry.Rect?)
    fun openAppOptions(entry: AppEntry)
    fun openEditMode()
    fun openSettings()
    fun openClaudeFeed()
    fun openClock()
    fun openNotification(card: NotificationCard)
    fun launchRom(rom: RomEntry)
    fun openRomOptions(rom: RomEntry)
    fun pickRomFolder()
    fun openDeepLink(target: String)
    fun openSearch()
}
