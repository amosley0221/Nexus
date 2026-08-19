package com.nexus.launcher.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.launcher.data.IconPackMode
import com.nexus.launcher.data.NexusSettings
import com.nexus.launcher.domain.AppEntry
import com.nexus.launcher.integration.claude.ClaudeHomeLine
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
    claudeLine: ClaudeHomeLine?,
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
    claudeLine: ClaudeHomeLine?,
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
    claudeLine: ClaudeHomeLine?,
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
    claudeLine: ClaudeHomeLine?,
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
                text = claudeLine.text,
                style = NexusType.DateLine,
                color = if (claudeLine.needsInput) NexusColor.Amber else NexusColor.Accent,
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

    // Favourites first, then everything alphabetically — one uniform list, no
    // header or divider between the two halves.
    //
    // Pinned apps are lifted out of the A–Z run rather than repeated in it. They
    // are already in [apps], and a lazy list throws outright on a duplicate key;
    // with no divider between the halves, the same app twice would read as a
    // duplication bug anyway.
    val rows = remember(apps, favorites) {
        val pinned = favorites.mapTo(HashSet()) { it.key }
        (favorites + apps.filterNot { it.key in pinned }).distinctBy { it.key }
    }

    // Index of the first row for each section letter, so a scrub can jump
    // straight to it. Favourites occupy the rows before the A–Z run.
    val letterAnchors = remember(rows, favorites) {
        buildMap {
            for (index in favorites.size until rows.size) {
                putIfAbsent(rows[index].sortLetter, index)
            }
        }
    }

    val presentLetters = remember(letterAnchors) {
        AZ_LETTERS.filter { it == STAR_LETTER || it in letterAnchors }
    }

    /** First row at or after [letter]; the star and unknown letters go to the top. */
    fun anchorFor(letter: Char): Int {
        if (letter == STAR_LETTER) return 0
        letterAnchors[letter]?.let { return it }
        // The touched letter has no apps — fall forward to the next one that does.
        return AZ_LETTERS
            .dropWhile { it != letter }
            .firstNotNullOfOrNull { letterAnchors[it] }
            ?: 0
    }

    // The pin hint occupies a lazy slot of its own, so every anchor shifts by
    // one while it is showing.
    val headerRows = if (favorites.isEmpty()) 1 else 0

    suspend fun scrollTo(rowIndex: Int) {
        val index = rowIndex + headerRows
        if (columns > 1) gridState.scrollToItem(index) else listState.scrollToItem(index)
    }

    // Where the list sat before the scrub began, so a cancelled gesture can put
    // it back rather than dumping the user at the top. Null means this gesture
    // has not captured a resting position yet.
    var preScrub by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    LaunchedEffect(scrub.activeLetter) {
        val letter = scrub.activeLetter ?: return@LaunchedEffect
        scrollTo(anchorFor(letter))
    }

    Box(modifier = modifier) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                val contentPadding = PaddingValues(
                    start = profile.homePadding,
                    end = 8.dp,
                    top = 8.dp,
                    bottom = 8.dp,
                )

                if (columns > 1) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        state = gridState,
                        contentPadding = contentPadding,
                        horizontalArrangement = Arrangement.spacedBy(44.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (favorites.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) { PinHint() }
                        }
                        gridItems(rows, key = { it.key }) { app ->
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
                        contentPadding = contentPadding,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (favorites.isEmpty()) {
                            item { PinHint() }
                        }
                        items(rows, key = { it.key }) { app ->
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
                onLetterChanged = {
                    // Capture the resting position on the first letter of a
                    // gesture, before any preview scrolling moves the list.
                    if (preScrub == null) {
                        preScrub = if (columns > 1) {
                            gridState.firstVisibleItemIndex to
                                gridState.firstVisibleItemScrollOffset
                        } else {
                            listState.firstVisibleItemIndex to
                                listState.firstVisibleItemScrollOffset
                        }
                    }
                },
                onReleased = { committed ->
                    // The preview scroll already put the list where it belongs;
                    // committing just means leaving it there.
                    preScrub = null
                    if (committed != null) {
                        scope.launch { scrollTo(anchorFor(committed)) }
                    }
                },
                onCancelled = {
                    val resting = preScrub
                    preScrub = null
                    if (resting != null) {
                        scope.launch {
                            val (index, offset) = resting
                            if (columns > 1) {
                                gridState.scrollToItem(index, offset)
                            } else {
                                listState.scrollToItem(index, offset)
                            }
                        }
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
 * Shown only while no favourites are pinned. Deliberately a bare line in the
 * date-line font — not a card, not a placeholder row — and it disappears for
 * good once the first favourite exists.
 */
@Composable
private fun PinHint() {
    val profile = LocalWindowProfile.current
    Text(
        text = "Long-press an app to pin it here",
        style = NexusType.DateLine.copy(fontSize = 13.sp),
        color = NexusColor.TextSecondary,
        modifier = Modifier.padding(bottom = 10.dp, end = profile.homePadding),
    )
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
    // Plain holders, deliberately not snapshot state. onPlaced fires on every
    // layout pass, so writing observable state here made each visible row
    // recompose on every frame of a scroll — the single biggest source of
    // choppiness in the list. Nothing needs to recompose when a row moves; the
    // wave is a draw-time transform and the bounds are only read on a tap.
    val placement = remember { RowPlacement() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .onPlaced { coords ->
                placement.centerY = coords.positionInParent().y + coords.size.height / 2f
                placement.bounds = coords.boundsInWindow()
            }
            // graphicsLayer over offset: the scrub state is read in the draw
            // phase, so a moving wave invalidates the layer instead of
            // recomposing and re-laying-out every row it touches.
            .graphicsLayer {
                translationX = waveOffsetDp(
                    rowCenterY = placement.centerY,
                    touchY = scrub.touchY,
                    active = scrub.isScrubbing,
                ) * density
            }
            .combinedClickable(
                onClick = { onLaunch(app, placement.bounds) },
                onLongClick = { onLongPress(app) },
            )
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

/** Where a row currently sits, held outside the snapshot system on purpose. */
private class RowPlacement {
    var centerY: Float = 0f
    var bounds: androidx.compose.ui.geometry.Rect? = null
}
