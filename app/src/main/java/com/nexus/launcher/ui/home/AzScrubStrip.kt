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

/** Sections currently present in the app list, in display order. */
val AZ_LETTERS: List<Char> = ('A'..'Z').toList() + '#'

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
    onReleased: () -> Unit,
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
                        state.activeLetter = null
                        lastLetter = null
                        onReleased()
                    },
                    onDragCancel = {
                        state.activeLetter = null
                        lastLetter = null
                        onReleased()
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
        letters.forEach { letter ->
            Text(
                text = letter.toString(),
                style = NexusType.AzLetter,
                color = if (state.activeLetter == letter) NexusColor.Accent else NexusColor.OnWallpaper,
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
 * Horizontal offset for a row during a scrub: a Gaussian wave centred on the
 * finger, peaking at [MAX_WAVE_OFFSET_DP] and falling off over ~90px either side.
 * Rows cascade to the LEFT, so the offset is negative.
 */
fun waveOffsetDp(rowCenterY: Float, touchY: Float, active: Boolean): Float {
    if (!active) return 0f
    val distance = abs(rowCenterY - touchY)
    val falloff = exp(-(distance * distance) / (2f * SIGMA * SIGMA))
    return -(MAX_WAVE_OFFSET_DP * falloff)
}

/** Peak slide of the row nearest the finger, in dp. */
const val MAX_WAVE_OFFSET_DP = 140f

/** Standard deviation of the Gaussian, in pixels. */
private const val SIGMA = 90f

fun Float.roundToPx(): Int = this.roundToInt()
