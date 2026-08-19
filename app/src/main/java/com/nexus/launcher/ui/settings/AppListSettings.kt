package com.nexus.launcher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.launcher.data.IconPackMode
import com.nexus.launcher.domain.AppEntry
import com.nexus.launcher.ui.common.AppIcon
import com.nexus.launcher.ui.hub.HubScaffold
import com.nexus.launcher.ui.theme.NexusColor
import com.nexus.launcher.ui.theme.NexusRadius
import com.nexus.launcher.ui.theme.NexusType

/**
 * Reusable per-app toggle list, used for favourites, hidden apps, and app lock.
 * [lockedStyle] renders the design's "🔒 Locked" label instead of a bare switch.
 */
@Composable
fun AppToggleListPage(
    title: String,
    subtitle: String,
    apps: List<AppEntry>,
    selected: Set<String>,
    iconPack: IconPackMode,
    modifier: Modifier = Modifier,
    lockedStyle: Boolean = false,
    onToggle: (AppEntry, Boolean) -> Unit,
    onDone: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val shown = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter { it.label.contains(query, ignoreCase = true) }
    }

    HubScaffold(
        title = title,
        modifier = modifier,
        subtitle = {
            Text(subtitle, style = NexusType.BodySmall, color = NexusColor.TextSecondary)
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(NexusRadius.Chip))
                .background(NexusColor.Card)
                .padding(horizontal = 14.dp, vertical = 11.dp),
        ) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = NexusType.BodySmall.copy(color = NexusColor.TextPrimary),
                cursorBrush = SolidColor(NexusColor.Accent),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(
                            text = "Search apps",
                            style = NexusType.BodySmall,
                            color = NexusColor.TextFaint,
                        )
                    }
                    inner()
                },
            )
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(shown, key = { it.key }) { app ->
                val isSelected = app.key in selected
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(app, !isSelected) }
                        .padding(vertical = 8.dp),
                ) {
                    AppIcon(entry = app, size = 34.dp, mode = iconPack)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.label,
                            style = NexusType.CardTitleSemi,
                            color = NexusColor.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (lockedStyle && isSelected) {
                            Text(
                                text = "🔒 Locked",
                                style = NexusType.Meta,
                                color = NexusColor.Accent,
                            )
                        }
                    }
                    Switch(
                        checked = isSelected,
                        onCheckedChange = { onToggle(app, it) },
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
        }
    }
}
