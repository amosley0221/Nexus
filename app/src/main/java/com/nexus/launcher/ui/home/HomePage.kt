package com.nexus.launcher.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.nexus.launcher.data.NexusSettings
import com.nexus.launcher.domain.AppEntry
import com.nexus.launcher.domain.FolderContents
import com.nexus.launcher.integration.claude.ClaudeHomeLine
import com.nexus.launcher.integration.weather.WeatherNow
import com.nexus.launcher.ui.clock.ClockText
import com.nexus.launcher.ui.common.AppIcon
import com.nexus.launcher.ui.theme.LocalWindowProfile
import com.nexus.launcher.ui.theme.NexusColor
import com.nexus.launcher.ui.theme.NexusType
import kotlinx.coroutines.launch

/**
 * Home: clock, date, optional Claude line, the pinned favourites, and the A–Z
 * strip. The window is transparent, so the system wallpaper is the background —
 * nothing is drawn behind this content.
 */
@Composable
fun HomePage(
    apps: List<AppEntry>,
    favorites: List<AppEntry>,
    folders: List<FolderContents>,
    settings: NexusSettings,
    clock: ClockText,
    weather: WeatherNow?,
    claudeLine: ClaudeHomeLine?,
    resetToken: Int,
    modifier: Modifier = Modifier,
    onLaunch: (AppEntry, androidx.compose.ui.geometry.Rect?) -> Unit,
    onLongPressApp: (AppEntry) -> Unit,
    onOpenFolder: (FolderContents) -> Unit,
    onClaudeClick: () -> Unit,
    onClockClick: () -> Unit,
) {
    val profile = LocalWindowProfile.current

    if (profile.useSideClock) {
        HomeLandscape(
            apps = apps,
            favorites = favorites,
            folders = folders,
            settings = settings,
            clock = clock,
            weather = weather,
            claudeLine = claudeLine,
            resetToken = resetToken,
            modifier = modifier,
            onLaunch = onLaunch,
            onLongPressApp = onLongPressApp,
            onOpenFolder = onOpenFolder,
            onClaudeClick = onClaudeClick,
            onClockClick = onClockClick,
        )
    } else {
        HomePortrait(
            apps = apps,
            favorites = favorites,
            folders = folders,
            settings = settings,
            clock = clock,
            weather = weather,
            claudeLine = claudeLine,
            resetToken = resetToken,
            modifier = modifier,
            onLaunch = onLaunch,
            onLongPressApp = onLongPressApp,
            onOpenFolder = onOpenFolder,
            onClaudeClick = onClaudeClick,
            onClockClick = onClockClick,
        )
    }
}

@Composable
private fun HomePortrait(
    apps: List<AppEntry>,
    favorites: List<AppEntry>,
    folders: List<FolderContents>,
    settings: NexusSettings,
    clock: ClockText,
    weather: WeatherNow?,
    claudeLine: ClaudeHomeLine?,
    resetToken: Int,
    modifier: Modifier,
    onLaunch: (AppEntry, androidx.compose.ui.geometry.Rect?) -> Unit,
    onLongPressApp: (AppEntry) -> Unit,
    onOpenFolder: (FolderContents) -> Unit,
    onClaudeClick: () -> Unit,
    onClockClick: () -> Unit,
) {
    val profile = LocalWindowProfile.current

    Column(modifier = modifier.fillMaxSize()) {
        ClockBlock(
            clock = clock,
            weather = weather,
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
            folders = folders,
            settings = settings,
            columns = profile.homeColumns,
            resetToken = resetToken,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            onLaunch = onLaunch,
            onLongPressApp = onLongPressApp,
            onOpenFolder = onOpenFolder,
        )
    }
}

/** Folded landscape: clock pinned left, list centre, A–Z right. */
@Composable
private fun HomeLandscape(
    apps: List<AppEntry>,
    favorites: List<AppEntry>,
    folders: List<FolderContents>,
    settings: NexusSettings,
    clock: ClockText,
    weather: WeatherNow?,
    claudeLine: ClaudeHomeLine?,
    resetToken: Int,
    modifier: Modifier,
    onLaunch: (AppEntry, androidx.compose.ui.geometry.Rect?) -> Unit,
    onLongPressApp: (AppEntry) -> Unit,
    onOpenFolder: (FolderContents) -> Unit,
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
                clock = clock,
                weather = weather,
                claudeLine = claudeLine.takeIf { settings.showClaudeStatus },
                onClaudeClick = onClaudeClick,
                onClockClick = onClockClick,
            )
        }

        FavoritesWithScrub(
            apps = apps,
            favorites = favorites,
            folders = folders,
            settings = settings,
            columns = 1,
            resetToken = resetToken,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            onLaunch = onLaunch,
            onLongPressApp = onLongPressApp,
            onOpenFolder = onOpenFolder,
        )
    }
}

/**
 * The clock, Niagara-style: no separator at all — the minutes are simply set in
 * the accent and pulled back so their first glyph tucks under the hour's last
 * one. The date sits directly beneath, close enough to read as one block, and
 * carries the weather and battery.
 */
@Composable
private fun ClockBlock(
    clock: ClockText,
    weather: WeatherNow?,
    claudeLine: ClaudeHomeLine?,
    onClaudeClick: () -> Unit,
    onClockClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile = LocalWindowProfile.current
    val digitSize = NexusType.Clock.fontSize.value * profile.clockScale
    val digitStyle = NexusType.Clock.copy(
        fontSize = digitSize.sp,
        lineHeight = (NexusType.Clock.lineHeight.value * profile.clockScale).sp,
    )
    // How far the minutes slide back under the hour. A share of the type size,
    // so it holds at every clock scale.
    val overlap = (digitSize * 0.13f).dp

    val bounce = remember { Animatable(1f) }
    LaunchedEffect(clock.minute) {
        bounce.snapTo(0.94f)
        bounce.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        )
    }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .graphicsLayer {
                    // Anchored at the left edge so the bounce grows out of the
                    // margin rather than swelling around the centre.
                    transformOrigin = TransformOrigin(0f, 0.5f)
                    scaleX = bounce.value
                    scaleY = bounce.value
                }
                .clickable(onClick = onClockClick),
        ) {
            Text(
                text = clock.hour,
                style = digitStyle,
                color = NexusColor.OnWallpaper,
                // Drawn above the minutes, so the hour stays whole where the
                // two overlap and the minutes are the ones that give way.
                modifier = Modifier.zIndex(1f),
            )
            Text(
                text = clock.minute,
                style = digitStyle,
                color = NexusColor.Accent,
                modifier = Modifier.offset(x = -overlap),
            )
            if (clock.meridiem.isNotEmpty()) {
                Text(
                    text = clock.meridiem,
                    style = NexusType.DateLine.copy(fontSize = (digitSize * 0.22f).sp),
                    color = NexusColor.TextSecondary,
                    modifier = Modifier
                        .offset(x = -overlap + 4.dp)
                        .padding(bottom = (digitSize * 0.13f).dp),
                )
            }
        }

        // Negative, not a spacer: the clock's line box carries a band of descent
        // space below the glyphs, and the date has to climb back through it to
        // sit under the numbers rather than a finger-width below them.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.offset(y = (-digitSize * 0.14f).dp),
        ) {
            Text(
                text = clock.date,
                style = NexusType.DateLine,
                color = NexusColor.OnWallpaper,
            )
            if (weather != null) {
                Text(
                    text = weather.short,
                    style = NexusType.DateLine,
                    color = NexusColor.OnWallpaper,
                    maxLines = 1,
                )
            }
        }

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
 * The Home list and the scrub strip.
 *
 * At rest the list is the pinned favourites and nothing else — Home is not an
 * app drawer. Touching the A–Z strip switches it to the full alphabetical run
 * so the user can reach anything, and releasing leaves them parked in that
 * section rather than snapping back. Leaving Home and returning restores the
 * favourites, which is what [resetToken] is for: the caller bumps it whenever
 * the Home page stops being the settled page.
 */
@Composable
private fun FavoritesWithScrub(
    apps: List<AppEntry>,
    favorites: List<AppEntry>,
    folders: List<FolderContents>,
    settings: NexusSettings,
    columns: Int,
    resetToken: Int,
    modifier: Modifier = Modifier,
    onLaunch: (AppEntry, androidx.compose.ui.geometry.Rect?) -> Unit,
    onLongPressApp: (AppEntry) -> Unit,
    onOpenFolder: (FolderContents) -> Unit,
) {
    val profile = LocalWindowProfile.current
    val scrub = rememberScrubState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    // False is Home proper: favourites only. True is the browse state a scrub
    // opens up, holding the full A–Z list.
    var browsing by remember { mutableStateOf(false) }

    LaunchedEffect(resetToken) { browsing = false }

    // Duplicate keys throw outright in a lazy list, and both lists are built
    // from settings that could name the same package twice.
    val pinnedRows = remember(favorites) { favorites.distinctBy { it.key } }
    val allRows = remember(apps) { apps.distinctBy { it.key } }
    val rows = if (browsing) allRows else pinnedRows

    // Index of the first row for each section letter in the browse list.
    val letterAnchors = remember(allRows) {
        buildMap {
            allRows.forEachIndexed { index, app -> putIfAbsent(app.sortLetter, index) }
        }
    }

    val presentLetters = remember(letterAnchors) {
        AZ_LETTERS.filter { it == STAR_LETTER || it in letterAnchors }
    }

    /** First row at or after [letter]; unknown letters fall forward to the next real one. */
    fun anchorFor(letter: Char): Int {
        if (letter == STAR_LETTER) return 0
        letterAnchors[letter]?.let { return it }
        return AZ_LETTERS
            .dropWhile { it != letter }
            .firstNotNullOfOrNull { letterAnchors[it] }
            ?: 0
    }

    // The pin hint occupies a lazy slot of its own, so every anchor shifts by
    // one while it is showing. It only shows at rest, and scrubbing leaves rest.
    val showPinHint = !browsing && pinnedRows.isEmpty()

    suspend fun scrollTo(rowIndex: Int) {
        if (columns > 1) gridState.scrollToItem(rowIndex) else listState.scrollToItem(rowIndex)
    }

    // Coming back to favourites from anywhere down the alphabet, the saved
    // scroll position is meaningless — start at the top.
    LaunchedEffect(browsing) {
        if (!browsing) scrollTo(0)
    }

    // Where the list sat before the scrub began, so a cancelled gesture can put
    // it back rather than dumping the user at the top. Null means this gesture
    // has not captured a resting position yet.
    var preScrub by remember { mutableStateOf<ScrubOrigin?>(null) }

    LaunchedEffect(scrub.activeLetter, browsing) {
        val letter = scrub.activeLetter ?: return@LaunchedEffect
        if (letter == STAR_LETTER) return@LaunchedEffect
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
                        if (showPinHint) {
                            item(span = { GridItemSpan(maxLineSpan) }) { PinHint() }
                        }
                        gridItems(rows, key = { it.key }) { app ->
                            FavoriteRow(
                                app = app,
                                settings = settings,
                                onLaunch = onLaunch,
                                onLongPress = onLongPressApp,
                            )
                        }
                        if (browsing) {
                            gridItems(folders, key = { "folder:${it.id}" }) { folder ->
                                FolderRow(
                                    folder = folder,
                                    settings = settings,
                                    onClick = onOpenFolder,
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = contentPadding,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (showPinHint) {
                            item { PinHint() }
                        }
                        items(rows, key = { it.key }) { app ->
                            FavoriteRow(
                                app = app,
                                settings = settings,
                                onLaunch = onLaunch,
                                onLongPress = onLongPressApp,
                            )
                        }
                        // Folders live after Z: they are where apps go to leave
                        // the alphabet, so they sit past the end of it.
                        if (browsing) {
                            items(folders, key = { "folder:${it.id}" }) { folder ->
                                FolderRow(
                                    folder = folder,
                                    settings = settings,
                                    onClick = onOpenFolder,
                                )
                            }
                        }
                    }
                }
            }

            AzScrubStrip(
                letters = presentLetters,
                state = scrub,
                modifier = Modifier.padding(end = 4.dp),
                onLetterChanged = { letter ->
                    // Capture the resting position on the first letter of a
                    // gesture, before any preview scrolling moves the list.
                    if (preScrub == null) {
                        preScrub = ScrubOrigin(
                            browsing = browsing,
                            firstVisibleIndex = if (columns > 1) {
                                gridState.firstVisibleItemIndex
                            } else {
                                listState.firstVisibleItemIndex
                            },
                            firstVisibleOffset = if (columns > 1) {
                                gridState.firstVisibleItemScrollOffset
                            } else {
                                listState.firstVisibleItemScrollOffset
                            },
                        )
                    }
                    // The star is the way back: it means favourites, not the
                    // top of the alphabet.
                    browsing = letter != STAR_LETTER
                },
                onReleased = {
                    // The preview scroll already put the list where it belongs;
                    // committing just means leaving it there — including the
                    // browse list itself, which stays up until Home is re-entered.
                    preScrub = null
                },
                onCancelled = {
                    val origin = preScrub
                    preScrub = null
                    if (origin != null) {
                        browsing = origin.browsing
                        scope.launch {
                            if (columns > 1) {
                                gridState.scrollToItem(
                                    origin.firstVisibleIndex,
                                    origin.firstVisibleOffset,
                                )
                            } else {
                                listState.scrollToItem(
                                    origin.firstVisibleIndex,
                                    origin.firstVisibleOffset,
                                )
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

/** What the list looked like before a scrub, so a cancelled gesture can undo it. */
private data class ScrubOrigin(
    val browsing: Boolean,
    val firstVisibleIndex: Int,
    val firstVisibleOffset: Int,
)

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

/** One list row: blush squircle icon plus the app name. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteRow(
    app: AppEntry,
    settings: NexusSettings,
    onLaunch: (AppEntry, androidx.compose.ui.geometry.Rect?) -> Unit,
    onLongPress: (AppEntry) -> Unit,
) {
    // Plain holder, deliberately not snapshot state: onPlaced fires on every
    // layout pass, and writing observable state here would recompose every
    // visible row on every frame of a scroll. The bounds are only read on a tap.
    val placement = remember { RowPlacement() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .onPlaced { coords -> placement.bounds = coords.boundsInWindow() }
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

/**
 * One folder row. The tile is a quarter-grid of the first four members rather
 * than a folder glyph — at a glance you recognise a folder by what is in it.
 */
@Composable
private fun FolderRow(
    folder: FolderContents,
    settings: NexusSettings,
    onClick: (FolderContents) -> Unit,
) {
    val tile = settings.iconSizeDp.dp

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(folder) }
            .padding(vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(tile)
                .clip(RoundedCornerShape(tile * 0.3f))
                .background(NexusColor.Accent.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                folder.apps.take(4).chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        pair.forEach { app ->
                            AppIcon(
                                entry = app,
                                size = tile * 0.38f,
                                mode = settings.iconPack,
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = folder.name,
            style = NexusType.AppLabel.copy(fontSize = settings.labelSizeSp.sp),
            color = NexusColor.OnWallpaper,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = folder.apps.size.toString(),
            style = NexusType.AppLabel.copy(fontSize = (settings.labelSizeSp - 3).sp),
            color = NexusColor.TextSecondary,
        )
    }
}

/** Where a row currently sits, held outside the snapshot system on purpose. */
private class RowPlacement {
    var bounds: androidx.compose.ui.geometry.Rect? = null
}
