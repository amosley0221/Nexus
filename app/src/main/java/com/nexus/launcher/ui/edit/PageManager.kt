package com.nexus.launcher.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nexus.launcher.domain.PageConfig
import com.nexus.launcher.domain.PageKind
import com.nexus.launcher.ui.common.NexusChip
import com.nexus.launcher.ui.hub.HubScaffold
import com.nexus.launcher.ui.theme.NexusColor
import com.nexus.launcher.ui.theme.NexusRadius
import com.nexus.launcher.ui.theme.NexusType
import kotlin.math.roundToInt

private val TEMPLATES = listOf(
    "Games" to PageKind.Games,
    "Movies & TV" to PageKind.Media,
    "Music" to PageKind.Music,
    "Books" to PageKind.Files,
    "Budget" to PageKind.Budget,
    "＋ Blank" to PageKind.Blank,
)

/**
 * Reorderable list of pages with visibility toggles, plus the template chips
 * that add a new page.
 */
@Composable
fun PageManager(
    pages: List<PageConfig>,
    modifier: Modifier = Modifier,
    onReorder: (from: Int, to: Int) -> Unit,
    onToggleVisible: (PageConfig) -> Unit,
    onEditPage: (PageConfig) -> Unit,
    onAddTemplate: (PageKind) -> Unit,
    onDone: () -> Unit,
) {
    val listState = rememberLazyListState()
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var rowHeight by remember { mutableFloatStateOf(1f) }

    HubScaffold(
        title = "Pages",
        modifier = modifier,
        trailing = {
            Text(
                text = "Done",
                style = NexusType.CardTitle,
                color = NexusColor.Accent,
                modifier = Modifier.clickable(onClick = onDone),
            )
        },
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
        ) {
            itemsIndexed(pages, key = { _, page -> page.id }) { index, page ->
                val isDragging = draggingIndex == index
                PageRow(
                    page = page,
                    modifier = Modifier
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (isDragging) dragOffset else 0f
                            alpha = if (isDragging) 0.92f else 1f
                        },
                    onToggle = { onToggleVisible(page) },
                    onClick = { onEditPage(page) },
                    handleModifier = Modifier.pointerInput(pages.size, index) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingIndex = index
                                dragOffset = 0f
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffset += amount.y
                            },
                            onDragEnd = {
                                val moved = (dragOffset / rowHeight).roundToInt()
                                val target = (index + moved).coerceIn(0, pages.lastIndex)
                                if (target != index) onReorder(index, target)
                                draggingIndex = null
                                dragOffset = 0f
                            },
                            onDragCancel = {
                                draggingIndex = null
                                dragOffset = 0f
                            },
                        )
                    },
                    onMeasured = { rowHeight = it },
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = "+ Add page from template",
            style = NexusType.SectionHeader,
            color = NexusColor.TextPrimary,
        )
        Spacer(Modifier.height(10.dp))

        // Chips wrap so the row never overflows on a narrow cover screen.
        TEMPLATES.chunked(3).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            ) {
                row.forEach { (label, kind) ->
                    NexusChip(label = label, selected = false) { onAddTemplate(kind) }
                }
            }
        }
    }
}

@Composable
private fun PageRow(
    page: PageConfig,
    modifier: Modifier = Modifier,
    handleModifier: Modifier = Modifier,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onMeasured: (Float) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
            .onSizeChanged { size -> if (size.height > 0) onMeasured(size.height.toFloat()) },
    ) {
        Text(
            text = "⠿",
            style = NexusType.SectionHeader,
            color = NexusColor.TextFaint,
            modifier = handleModifier,
        )

        // Page thumbnail, tinted with the page's accent colour.
        Box(
            modifier = Modifier
                .width(38.dp)
                .height(62.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(NexusColor.Card)
                .border(1.dp, Color(page.accentColor).copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = page.title,
                style = NexusType.SectionHeader,
                color = NexusColor.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = page.description,
                style = NexusType.Meta,
                color = NexusColor.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Switch(
            checked = page.visible,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = NexusColor.Glyph,
                checkedTrackColor = NexusColor.Accent,
                uncheckedThumbColor = NexusColor.TextFaint,
                uncheckedTrackColor = NexusColor.Card,
                uncheckedBorderColor = NexusColor.Border,
            ),
        )
    }
}
