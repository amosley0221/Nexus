package com.nexus.launcher.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nexus.launcher.domain.PageConfig
import com.nexus.launcher.domain.WidgetConfig
import com.nexus.launcher.ui.common.NexusChip
import com.nexus.launcher.ui.common.dashedBorder
import com.nexus.launcher.ui.hub.HubScaffold
import com.nexus.launcher.ui.theme.NexusColor
import com.nexus.launcher.ui.theme.NexusRadius
import com.nexus.launcher.ui.theme.NexusType

/** Size options a widget type offers, keyed by type id. */
val WIDGET_SIZE_OPTIONS: Map<String, List<String>> = mapOf(
    "row" to listOf("Row S", "Cards M", "Hero L"),
    "list" to listOf("List", "Grid 2×", "Grid 3×"),
    "stat" to listOf("S", "M", "L"),
)

val ACCENT_SWATCHES = listOf(
    0xFFF6B1A4, 0xFF8B5CF6, 0xFFE8A33D, 0xFF7DE0A3, 0xFF8AB0F0, 0xFFE05656,
)

/**
 * Per-page widget configuration: toggle each block, pick its size, set the page
 * accent, and open the gallery to add more.
 */
@Composable
fun PageEditor(
    page: PageConfig,
    modifier: Modifier = Modifier,
    onToggleWidget: (WidgetConfig) -> Unit,
    onResizeWidget: (WidgetConfig, String) -> Unit,
    onRemoveWidget: (WidgetConfig) -> Unit,
    onAccentChange: (Long) -> Unit,
    onAddWidget: () -> Unit,
    onDone: () -> Unit,
) {
    HubScaffold(
        title = page.title,
        modifier = modifier,
        accent = Color(page.accentColor),
        subtitle = {
            Text(
                text = page.description,
                style = NexusType.BodySmall,
                color = NexusColor.TextSecondary,
            )
        },
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(page.widgets, key = { it.id }) { widget ->
                WidgetCard(
                    widget = widget,
                    onToggle = { onToggleWidget(widget) },
                    onResize = { onResizeWidget(widget, it) },
                    onRemove = { onRemoveWidget(widget) },
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(NexusRadius.Card))
                        .dashedBorder(NexusColor.Border, NexusRadius.Card)
                        .clickable(onClick = onAddWidget)
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "＋ Add widget from gallery · incl. KWGT & app widgets",
                        style = NexusType.Meta,
                        color = NexusColor.TextSecondary,
                    )
                }
            }

            item {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Accent colour",
                    style = NexusType.SectionHeader,
                    color = NexusColor.TextPrimary,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ACCENT_SWATCHES.forEach { swatch ->
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color(swatch))
                                .border(
                                    width = if (swatch == page.accentColor) 2.dp else 0.dp,
                                    color = NexusColor.TextPrimary,
                                    shape = CircleShape,
                                )
                                .clickable { onAccentChange(swatch) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetCard(
    widget: WidgetConfig,
    onToggle: () -> Unit,
    onResize: (String) -> Unit,
    onRemove: () -> Unit,
) {
    val sizes = WIDGET_SIZE_OPTIONS[widget.type] ?: WIDGET_SIZE_OPTIONS.getValue("stat")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NexusRadius.Card))
            .background(NexusColor.Card)
            .border(1.dp, NexusColor.Border, RoundedCornerShape(NexusRadius.Card))
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = WidgetCatalog.displayName(widget.type),
                    style = NexusType.CardTitle,
                    color = NexusColor.TextPrimary,
                )
                Text(
                    text = WidgetCatalog.description(widget.type),
                    style = NexusType.Meta,
                    color = NexusColor.TextSecondary,
                )
            }
            Switch(
                checked = widget.enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NexusColor.Glyph,
                    checkedTrackColor = NexusColor.Accent,
                    uncheckedThumbColor = NexusColor.TextFaint,
                    uncheckedTrackColor = NexusColor.HubBackground,
                    uncheckedBorderColor = NexusColor.Border,
                ),
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            sizes.forEach { size ->
                NexusChip(label = size, selected = size == widget.size) { onResize(size) }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Remove",
            style = NexusType.Meta,
            color = NexusColor.Negative,
            modifier = Modifier.clickable(onClick = onRemove),
        )
    }
}
