package com.nexus.launcher.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import com.nexus.launcher.ui.theme.NexusColor
import com.nexus.launcher.ui.theme.NexusType
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * The strip's top entry. Not a section letter — it jumps to the very top of the
 * list, which is the favourites when any are pinned and the start of A–Z when
 * they are not.
 */
const val STAR_LETTER: Char = '\u2606'

/** Sections the strip can show, in display order. */
val AZ_LETTERS: List<Char> = listOf(STAR_LETTER) + ('A'..'Z').toList() + '#'

/**
 * State shared between the A–Z strip and the favourites list: which letter the
 * finger is on, and how far each row should slide left in the wave.
 */
class ScrubState {
    var activeLetter by mutableStateOf<Char?>(null)
        internal set

    /** Y position of the finger inside the strip, in pixels. */
    var touchY by mutableFloatStateOf(0f)
        internal set

    var stripHeight by mutableFloatStateOf(0f)
        internal set

    val isScrubbing: Boolean get() = activeLetter != null

    /**
     * The letter the finger was on when the gesture ended. Release commits this
     * as the list's new anchor; a cancel leaves it null so the caller can put
     * the list back where it was.
     */
    var committedLetter: Char? = null
        internal set
}

@Composable
fun rememberScrubState(): ScrubState = remember { ScrubState() }

/**
 * The A–Z strip pinned to the right edge. Dragging it drives the wave, the red
 * bubble, and the jump — matching the prototype's Gaussian falloff.
 */
@Composable
fun AzScrubStrip(
    letters: List<Char>,
    state: ScrubState,
    modifier: Modifier = Modifier,
    onLetterChanged: (Char) -> Unit,
    onReleased: (committed: Char?) -> Unit,
    onCancelled: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var lastLetter by remember { mutableStateOf<Char?>(null) }

    fun letterAt(y: Float): Char? {
        if (letters.isEmpty() || state.stripHeight <= 0f) return null
        val index = ((y / state.stripHeight) * letters.size)
            .toInt()
            .coerceIn(0, letters.lastIndex)
        return letters[index]
    }

    Column(
        modifier = modifier
            .width(22.dp)
            .fillMaxHeight()
            .onSizeChanged { state.stripHeight = it.height.toFloat() }
            .pointerInput(letters) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        state.touchY = offset.y
                        letterAt(offset.y)?.let { letter ->
                            state.activeLetter = letter
                            lastLetter = letter
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLetterChanged(letter)
                        }
                    },
                    onDragEnd = {
                        // Release commits: the letter under the finger becomes
                        // the list's anchor and the wave animates away without
                        // the list moving again.
                        state.committedLetter = state.activeLetter
                        state.activeLetter = null
                        lastLetter = null
                        onReleased(state.committedLetter)
                    },
                    onDragCancel = {
                        // A cancel is not a choice — the list goes back to where
                        // it was before the scrub started.
                        state.committedLetter = null
                        state.activeLetter = null
                        lastLetter = null
                        onCancelled()
                    },
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        state.touchY = change.position.y.coerceIn(0f, state.stripHeight)
                        val letter = letterAt(state.touchY)
                        if (letter != null && letter != lastLetter) {
                            lastLetter = letter
                            state.activeLetter = letter
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onLetterChanged(letter)
                        }
                    },
                )
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEachIndexed { index, letter ->
            Text(
                text = letter.toString(),
                style = NexusType.AzLetter,
                color = if (state.activeLetter == letter) NexusColor.Accent else NexusColor.OnWallpaper,
                // The wave belongs to the alphabet, not the app list: the
                // letters bow out toward the finger and the rows stay put.
                //
                // Positions are derived from the index rather than measured,
                // since the letters are evenly spaced down the strip — and
                // reading the scrub state inside graphicsLayer keeps this a
                // draw-phase transform, so a moving wave never recomposes.
                modifier = Modifier.graphicsLayer {
                    val spacing = state.stripHeight / letters.size
                    val letterCenter = spacing * (index + 0.5f)
                    translationX = waveOffsetDp(
                        positionY = letterCenter,
                        touchY = state.touchY,
                        active = state.isScrubbing,
                    ) * density
                },
            )
        }
    }
}

/**
 * The red bubble that rides beside the strip while scrubbing, showing the letter
 * currently under the finger.
 */
@Composable
fun ScrubBubble(state: ScrubState, modifier: Modifier = Modifier) {
    val letter = state.activeLetter ?: return
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(0.7f) }

    remember(letter) {
        scope.launch { scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
        letter
    }

    Box(
        modifier = modifier
            .size(62.dp)
            .clip(CircleShape)
            .background(NexusColor.ScrubBubble),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter.toString(),
            style = NexusType.ScrubBubble,
            color = NexusColor.OnWallpaper,
            modifier = Modifier.padding(bottom = 2.dp),
        )
    }
}

/**
 * Horizontal offset for a letter during a scrub: a Gaussian wave centred on the
 * finger, peaking at [MAX_WAVE_OFFSET_DP] and falling off over ~90px either
 * side, which is what bows the alphabet out into an arc. Letters cascade to the
 * LEFT, so the offset is negative.
 */
fun waveOffsetDp(positionY: Float, touchY: Float, active: Boolean): Float {
    if (!active) return 0f
    val distance = abs(positionY - touchY)
    val falloff = exp(-(distance * distance) / (2f * SIGMA * SIGMA))
    return -(MAX_WAVE_OFFSET_DP * falloff)
}

/** Peak slide of the letter nearest the finger, in dp. */
const val MAX_WAVE_OFFSET_DP = 140f

/** Standard deviation of the Gaussian, in pixels. */
private const val SIGMA = 90f

fun Float.roundToPx(): Int = this.roundToInt()
