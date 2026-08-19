package com.nexus.launcher.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.launcher.data.IconPackMode
import com.nexus.launcher.data.NexusSettings
import com.nexus.launcher.domain.AppEntry
import com.nexus.launcher.ui.common.AppIcon
import com.nexus.launcher.ui.layout.WindowProfile
import com.nexus.launcher.ui.theme.LocalWindowProfile
import com.nexus.launcher.ui.theme.NexusColor
import com.nexus.launcher.ui.theme.NexusType
import kotlinx.coroutines.launch

/**
 * Home: clock, date, optional Claude line, the Niagara-style favourites list,
 * and the A–Z strip. The window is transparent, so the system wallpaper is the
 * background — nothing is drawn behind this content.
 */
@Composable
fun HomePage(
    apps: List<AppEntry>,
    favorites: List<AppEntry>,
    settings: NexusSettings,
    clockText: String,
    dateText: String,
    claudeLine: String?,
    modifier: Modifier = Modifier,
    onLaunch: (AppEntry, androidx.compose.ui.geometry.Rect?) -> Unit,
    onLongPressApp: (AppEntry) -> Unit,
    onClaudeClick: () -> Unit,
    onClockClick: () -> Unit,
) {
    val profile = LocalWindowProfile.current

    if (profile.useSideClock) {
        HomeLandscape(
            apps = apps,
            favorites = favorites,
            settings = settings,
            clockText = clockText,
            dateText = dateText,
            claudeLine = claudeLine,
            modifier = modifier,
            onLaunch = onLaunch,
            onLongPressApp = onLongPressApp,
            onClaudeClick = onClaudeClick,
            onClockClick = onClockClick,
        )
    } else {
        HomePortrait(
            apps = apps,
            favorites = favorites,
            settings = settings,
            clockText = clockText,
            dateText = dateText,
            claudeLine = claudeLine,
            modifier = modifier,
            onLaunch = onLaunch,
            onLongPressApp = onLongPressApp,
            onClaudeClick = onClaudeClick,
            onClockClick = onClockClick,
        )
    }
}

@Composable
private fun HomePortrait(
    apps: List<AppEntry>,
    favorites: List<AppEntry>,
    settings: NexusSettings,
    clockText: String,
    dateText: String,
    claudeLine: String?,
    modifier: Modifier,
    onLaunch: (AppEntry, androidx.compose.ui.geometry.Rect?) -> Unit,
    onLongPressApp: (AppEntry) -> Unit,
    onClaudeClick: () -> Unit,
    onClockClick: () -> Unit,
) {
    val profile = LocalWindowProfile.current

    Column(modifier = modifier.fillMaxSize()) {
        ClockBlock(
            clockText = clockText,
            dateText = dateText,
            claudeLine = claudeLine.takeIf { settings.showClaudeStatus },
            onClaudeClick = onClaudeClick,
            onClockClick = onClockClick,
            modifier = Modifier.padding(
                start = profile.homePadding,
                end = profile.homePadding,
                top = 34.dp,
            ),
        )

        Spacer(Modifier.height(22.dp))

        FavoritesWithScrub(
            apps = apps,
            favorites = favorites,
            settings = settings,
            columns = profile.homeColumns,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            onLaunch = onLaunch,
            onLongPressApp = onLongPressApp,
        )
    }
}

/** Folded landscape: clock pinned left, list centre, A–Z right. */
@Composable
private fun HomeLandscape(
    apps: List<AppEntry>,
    favorites: List<AppEntry>,
    settings: NexusSettings,
    clockText: String,
    dateText: String,
    claudeLine: String?,
    modifier: Modifier,
    onLaunch: (AppEntry, androidx.compose.ui.geometry.Rect?) -> Unit,
    onLongPressApp: (AppEntry) -> Unit,
    onClaudeClick: () -> Unit,
    onClockClick: () -> Unit,
) {
    val profile = LocalWindowProfile.current
    // The clock column takes a share of the window rather than a fixed 290px, so
    // it stays proportional across cover-screen widths.
    val clockWidth = (profile.widthDp.value * 0.42f).dp.coerceIn(200.dp, 340.dp)

    Row(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .width(clockWidth)
                .fillMaxHeight()
                .padding(start = profile.homePadding, end = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            ClockBlock(
                clockText = clockText,
                dateText = dateText,
                claudeLine = claudeLine.takeIf { settings.showClaudeStatus },
                onClaudeClick = onClaudeClick,
                onClockClick = onClockClick,
            )
        }

        FavoritesWithScrub(
            apps = apps,
            favorites = favorites,
            settings = settings,
            columns = 1,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            onLaunch = onLaunch,
            onLongPressApp = onLongPressApp,
        )
    }
}

@Composable
private fun ClockBlock(
    clockText: String,
    dateText: String,
    claudeLine: String?,
    onClaudeClick: () -> Unit,
    onClockClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile = LocalWindowProfile.current
    val clockStyle = NexusType.Clock.copy(
        fontSize = (NexusType.Clock.fontSize.value * profile.clockScale).sp,
        lineHeight = (NexusType.Clock.lineHeight.value * profile.clockScale).sp,
    )

    Column(modifier = modifier) {
        Text(
            text = clockText,
            style = clockStyle,
            color = NexusColor.Accent,
            modifier = Modifier.clickable(onClick = onClockClick),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = dateText,
            style = NexusType.DateLine,
            color = NexusColor.OnWallpaper,
        )
        if (claudeLine != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = claudeLine,
                style = NexusType.DateLine,
                color = NexusColor.Accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(onClick = onClaudeClick),
            )
        }
    }
}

/**
 * The favourites list plus the scrub strip, wired together: dragging the strip
 * filters the list to that letter's section and pushes rows left in a wave.
 */
@Composable
private fun FavoritesWithScrub(
    apps: List<AppEntry>,
    favorites: List<AppEntry>,
    settings: NexusSettings,
    columns: Int,
    modifier: Modifier = Modifier,
    onLaunch: (AppEntry, androidx.compose.ui.geometry.Rect?) -> Unit,
    onLongPressApp: (AppEntry) -> Unit,
) {
    val profile = LocalWindowProfile.current
    val scrub = rememberScrubState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    // Letters that actually have apps, so the strip never points at an empty jump.
    val presentLetters = remember(apps) {
        val present = apps.mapTo(LinkedHashSet()) { it.sortLetter }
        AZ_LETTERS.filter { it in present }
    }

    // While scrubbing, the list shows every app from the active letter onward;
    // at rest it shows just the favourites, as the design specifies.
    val visible = remember(apps, favorites, scrub.activeLetter) {
        val letter = scrub.activeLetter
        if (letter == null) favorites else apps.filter { it.sortLetter >= letter }
    }

    LaunchedEffect(scrub.activeLetter) {
        if (scrub.activeLetter != null) {
            if (columns > 1) gridState.scrollToItem(0) else listState.scrollToItem(0)
        }
    }

    Box(modifier = modifier) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                if (columns > 1) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        state = gridState,
                        contentPadding = PaddingValues(
                            start = profile.homePadding,
                            end = 8.dp,
                            top = 8.dp,
                            bottom = 8.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(44.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        gridItems(visible, key = { it.key }) { app ->
                            FavoriteRow(
                                app = app,
                                settings = settings,
                                scrub = scrub,
                                onLaunch = onLaunch,
                                onLongPress = onLongPressApp,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            start = profile.homePadding,
                            end = 8.dp,
                            top = 8.dp,
                            bottom = 8.dp,
                        ),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(visible, key = { it.key }) { app ->
                            FavoriteRow(
                                app = app,
                                settings = settings,
                                scrub = scrub,
                                onLaunch = onLaunch,
                                onLongPress = onLongPressApp,
                            )
                        }
                    }
                }
            }

            AzScrubStrip(
                letters = presentLetters,
                state = scrub,
                modifier = Modifier.padding(end = 4.dp),
                onLetterChanged = { },
                onReleased = {
                    scope.launch {
                        if (columns > 1) gridState.scrollToItem(0) else listState.scrollToItem(0)
                    }
                },
            )
        }

        // Bubble rides beside the strip, vertically tracking the finger.
        if (scrub.isScrubbing) {
            val density = LocalDensity.current
            val bubbleY = with(density) { scrub.touchY.toDp() } - 31.dp
            ScrubBubble(
                state = scrub,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-46).dp, y = bubbleY.coerceAtLeast(0.dp)),
            )
        }
    }
}

/**
 * One favourites row: blush squircle icon plus the app name. During a scrub it
 * slides left by the Gaussian wave amount for its distance from the finger.
 */
@Composable
private fun FavoriteRow(
    app: AppEntry,
    settings: NexusSettings,
    scrub: ScrubState,
    onLaunch: (AppEntry, androidx.compose.ui.geometry.Rect?) -> Unit,
    onLongPress: (AppEntry) -> Unit,
) {
    var rowCenterY by remember { mutableFloatStateOf(0f) }
    var bounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }

    val waveDp = waveOffsetDp(rowCenterY, scrub.touchY, scrub.isScrubbing)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = waveDp.dp)
            .onGloballyPositioned { coords ->
                bounds = coords.boundsInWindow()
                rowCenterY = coords.positionInParent().y + coords.size.height / 2f
            }
            .clickable { onLaunch(app, bounds) }
            .padding(vertical = 6.dp),
    ) {
        AppIcon(
            entry = app,
            size = settings.iconSizeDp.dp,
            mode = settings.iconPack,
        )
        Text(
            text = app.label,
            style = NexusType.AppLabel.copy(fontSize = settings.labelSizeSp.sp),
            color = NexusColor.OnWallpaper,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
