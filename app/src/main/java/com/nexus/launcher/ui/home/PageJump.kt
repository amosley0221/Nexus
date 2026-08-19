package com.nexus.launcher.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.nexus.launcher.ui.theme.NexusColor
import com.nexus.launcher.ui.theme.NexusType

/**
 * The dot row above the gesture bar. Holding it fans out named page pills;
 * sliding the finger highlights one and releasing jumps to it.
 */
@Composable
fun PageJump(
    pageNames: List<String>,
    currentPage: Int,
    modifier: Modifier = Modifier,
    onJump: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var highlighted by remember { mutableIntStateOf(currentPage) }
    var rowWidth by remember { mutableIntStateOf(0) }
    val haptics = LocalHapticFeedback.current

    fun pageAt(x: Float): Int {
        if (rowWidth <= 0 || pageNames.isEmpty()) return currentPage
        return ((x / rowWidth) * pageNames.size).toInt().coerceIn(0, pageNames.lastIndex)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 10.dp),
            ) {
                pageNames.forEachIndexed { index, name ->
                    val active = index == highlighted
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (active) NexusColor.ActivePill else NexusColor.MaroonTranslucent
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = name,
                            style = NexusType.Pill,
                            color = NexusColor.OnWallpaper,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .onSizeChanged { rowWidth = it.width }
                .pointerInput(pageNames.size) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            expanded = true
                            highlighted = pageAt(offset.x)
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val next = pageAt(change.position.x)
                            if (next != highlighted) {
                                highlighted = next
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        onDragEnd = {
                            expanded = false
                            onJump(highlighted)
                        },
                        onDragCancel = { expanded = false },
                    )
                },
        ) {
            pageNames.indices.forEach { index ->
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == currentPage) NexusColor.Accent
                            else NexusColor.OnWallpaper.copy(alpha = 0.45f)
                        )
                )
            }
        }
    }
}
