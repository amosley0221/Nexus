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
import com.nexus.launcher.integration.claude.ClaudeHomeLine
import com.nexus.launcher.domain.NotificationCard
import com.nexus.launcher.domain.PageKind
import com.nexus.launcher.domain.RomEntry
import com.nexus.launcher.NexusApp
import com.nexus.launcher.integration.notifications.NexusNotificationListener
import com.nexus.launcher.ui.home.HomePage
import com.nexus.launcher.ui.home.NotificationsOverscroll
import com.nexus.launcher.ui.home.PageJump
import com.nexus.launcher.ui.hub.PAGE_INDICATOR_CLEARANCE
import com.nexus.launcher.ui.home.SearchOverscroll
import com.nexus.launcher.ui.theme.NexusColor
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

/** Overlay scroll position at which the Google feed counts as fully open. */
private const val OVERLAY_OPEN = 0.9f

/**
 * Scroll position below which the feed is treated as committed to closing.
 *
 * Deliberately not near zero. Waiting for the overlay to finish closing means
 * moving the pager only after Google's window has already slid off, so the
 * transparent Discover page is visible for the whole trip home. Reacting while
 * that window still covers most of the screen hides the move behind it.
 */
private const val OVERLAY_CLOSING = 0.6f

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

        // Landing on Discover any other way than a drag — a page-jump, a BACK,
        // or simply starting there — produces no scroll for the Google app to
        // follow, so it would sit closed behind a blank page. Tell it directly.
        LaunchedEffect(pagerState, discoverState.isUsable, discoverIndex) {
            if (!discoverState.isUsable) return@LaunchedEffect
            snapshotFlow { pagerState.settledPage }.collect { settled ->
                if (settled == discoverIndex) {
                    discoverOverlay.openOverlay()
                } else {
                    discoverOverlay.closeOverlay()
                }
            }
        }

        // The other half of that conversation. Once the feed is open, the Google
        // window is on top and handles its own dismiss gesture — it closes
        // without the pager ever hearing about it. The pager would stay parked
        // on the Discover page, which paints nothing while the overlay is live,
        // so the user is left on bare wallpaper that cannot swipe back to the
        // feed. Follow the overlay home when it closes itself.
        //
        // Only after having actually seen it open: the echo starts at 0, and
        // reacting to that would bounce straight off the page on arrival.
        LaunchedEffect(pagerState, discoverState.isUsable, discoverIndex, homeIndex) {
            if (!discoverState.isUsable) return@LaunchedEffect
            var sawOpen = false
            discoverOverlay.overlayProgress.collect { progress ->
                when {
                    progress >= OVERLAY_OPEN -> sawOpen = true

                    sawOpen && progress <= OVERLAY_CLOSING -> {
                        sawOpen = false
                        if (!pagerState.isScrollInProgress &&
                            pagerState.settledPage == discoverIndex
                        ) {
                            // Snap rather than animate: the overlay is already
                            // playing its own close animation, and a second
                            // animation underneath it reads as the page sliding
                            // away by itself once the feed has gone.
                            pagerState.scrollToPage(homeIndex)
                        }
                    }
                }
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

    // Always enabled. BACK closes an overscroll layer, then returns to Home,
    // and on Home it does nothing at all — a launcher must never navigate away
    // from itself and leave the user on whatever was behind it.
    BackHandler {
        when {
            overlay != Overlay.None -> overlay = Overlay.None
            pagerState.currentPage != homeIndex ->
                scope.launch { pagerState.animateScrollToPage(homeIndex) }
            else -> Unit
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
        // The pager fills the window and the dot row floats over it. Giving the
        // dots their own row would shorten every page by that much and leave a
        // band of bare Home wallpaper below each hub page.
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
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
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(bottom = PAGE_INDICATOR_CLEARANCE),
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
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
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
    val claudeLine: ClaudeHomeLine?
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
