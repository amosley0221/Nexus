package com.nexus.launcher.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.nexus.launcher.domain.PageConfig
import com.nexus.launcher.domain.WidgetConfig
import com.nexus.launcher.integration.widgets.AppWidgetSlot
import com.nexus.launcher.integration.widgets.NexusWidgetHost
import com.nexus.launcher.ui.common.dashedBorder
import com.nexus.launcher.ui.hub.HubScaffold
import com.nexus.launcher.ui.layout.WindowProfile
import com.nexus.launcher.ui.theme.LocalWindowProfile
import com.nexus.launcher.ui.theme.NexusColor
import com.nexus.launcher.ui.theme.NexusRadius
import com.nexus.launcher.ui.theme.NexusType

/**
 * A free widget canvas. Cells are computed from the window rather than fixed, so
 * the same page is a 4×2-class grid folded and a 6×3-class grid unfolded.
 * Occupied cells host Nexus blocks or any AppWidget; empty cells are dashed
 * add-slots.
 */
@Composable
fun BlankPage(
    page: PageConfig,
    widgetHost: NexusWidgetHost?,
    modifier: Modifier = Modifier,
    editing: Boolean = false,
    onAddAt: (gridX: Int, gridY: Int) -> Unit,
    onWidgetLongPress: (WidgetConfig) -> Unit,
) {
    val profile: WindowProfile = LocalWindowProfile.current
    val columns = profile.widgetGridColumns
    val rows = profile.widgetGridRows

    HubScaffold(
        title = page.title,
        modifier = modifier,
        subtitle = {
            Text(
                text = "$columns × $rows grid · snap to grid",
                style = NexusType.Meta,
                color = NexusColor.TextSecondary,
            )
        },
    ) {
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val gap = 8.dp
            val cellWidth = (maxWidth - gap * (columns - 1)) / columns
            val cellHeight = (maxHeight - gap * (rows - 1)) / rows

            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                repeat(rows) { y ->
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        repeat(columns) { x ->
                            // A widget owns this cell if the cell falls inside its span.
                            val occupant = page.widgets.firstOrNull { widget ->
                                widget.enabled &&
                                    x >= widget.gridX && x < widget.gridX + widget.spanX &&
                                    y >= widget.gridY && y < widget.gridY + widget.spanY
                            }
                            val isOrigin = occupant != null &&
                                occupant.gridX == x && occupant.gridY == y

                            when {
                                isOrigin && occupant != null -> {
                                    val w = cellWidth * occupant.spanX + gap * (occupant.spanX - 1)
                                    val h = cellHeight * occupant.spanY + gap * (occupant.spanY - 1)
                                    Box(
                                        modifier = Modifier
                                            .width(w)
                                            .height(h)
                                            .clip(RoundedCornerShape(NexusRadius.Card))
                                            .background(NexusColor.Card)
                                            .clickable(enabled = editing) {
                                                onWidgetLongPress(occupant)
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (occupant.appWidgetId >= 0 && widgetHost != null) {
                                            AppWidgetSlot(
                                                host = widgetHost,
                                                appWidgetId = occupant.appWidgetId,
                                                widthDp = w,
                                                heightDp = h,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        } else {
                                            Text(
                                                text = WidgetCatalog.displayName(occupant.type),
                                                style = NexusType.Meta,
                                                color = NexusColor.TextSecondary,
                                            )
                                        }
                                    }
                                }

                                occupant != null -> Unit // covered by its origin cell

                                else -> Box(
                                    modifier = Modifier
                                        .width(cellWidth)
                                        .height(cellHeight)
                                        .clip(RoundedCornerShape(NexusRadius.CardSmall))
                                        .dashedBorder(NexusColor.BorderSoft, NexusRadius.CardSmall)
                                        .clickable { onAddAt(x, y) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "＋",
                                        style = NexusType.SectionHeader,
                                        color = NexusColor.TextFaint,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
