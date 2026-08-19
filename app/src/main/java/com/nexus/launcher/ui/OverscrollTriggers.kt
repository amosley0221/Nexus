package com.nexus.launcher.ui

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Thin edge strips that catch the overscroll gestures without stealing taps from
 * the page beneath: a pull up near the bottom opens search, a pull down at the
 * top edge opens the notification cards.
 */
@Composable
fun OverscrollTriggers(
    enabled: Boolean,
    onPullUp: () -> Unit,
    onPullDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!enabled) return

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TRIGGER_HEIGHT)
                .pointerInput(Unit) {
                    var travelled = 0f
                    detectVerticalDragGestures(
                        onDragStart = { travelled = 0f },
                        onDragEnd = { if (travelled > THRESHOLD_PX) onPullDown() },
                        onVerticalDrag = { _, delta -> travelled += delta },
                    )
                }
        )

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TRIGGER_HEIGHT)
                .pointerInput(Unit) {
                    var travelled = 0f
                    detectVerticalDragGestures(
                        onDragStart = { travelled = 0f },
                        onDragEnd = { if (-travelled > THRESHOLD_PX) onPullUp() },
                        onVerticalDrag = { _, delta -> travelled += delta },
                    )
                }
        )
    }
}

private val TRIGGER_HEIGHT = 24.dp
private const val THRESHOLD_PX = 60f
