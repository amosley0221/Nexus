package com.nexus.launcher.ui.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.launcher.ui.common.ChipRow
import com.nexus.launcher.ui.common.FileTypeTile
import com.nexus.launcher.ui.theme.NexusColor
import com.nexus.launcher.ui.theme.NexusRadius
import com.nexus.launcher.ui.theme.NexusType

/** A file surfaced from MediaStore or a granted SAF folder. */
data class FileEntry(
    val uri: String,
    val name: String,
    val extension: String,
    val sizeBytes: Long,
    val modifiedMillis: Long,
    val starred: Boolean = false,
    val dueLabel: String? = null,
)

/** An assignment or deadline shown in the DUE SOON card. */
data class DueItem(val title: String, val due: String)

private val TABS = listOf("Work", "School", "Media", "⬇")

@Composable
fun FilesPage(
    files: List<FileEntry>,
    dueItems: List<DueItem>,
    freeSpaceLabel: String,
    modifier: Modifier = Modifier,
    onFileClick: (FileEntry) -> Unit,
    onStarToggle: (FileEntry) -> Unit,
    onPickFolder: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }

    HubScaffold(
        title = "Files",
        modifier = modifier,
        trailing = {
            Text(
                text = freeSpaceLabel,
                style = NexusType.BodySmall,
                color = NexusColor.TextSecondary,
            )
        },
    ) {
        ChipRow(labels = TABS, selectedIndex = tab, onSelect = { tab = it })
        Spacer(Modifier.height(16.dp))

        if (dueItems.isNotEmpty()) {
            DueSoonCard(items = dueItems)
            Spacer(Modifier.height(18.dp))
        }

        SectionRow(title = "Recent", linkText = "Sort: modified ▾")
        Spacer(Modifier.height(6.dp))

        if (files.isEmpty()) {
            HubEmptyState(
                message = "No files found yet.",
                action = "Pick a folder to index",
                onAction = onPickFolder,
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(files, key = { it.uri }) { file ->
                    FileRow(
                        file = file,
                        onClick = { onFileClick(file) },
                        onStar = { onStarToggle(file) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DueSoonCard(items: List<DueItem>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NexusRadius.Card))
            .background(NexusColor.AmberSurface)
            .border(1.dp, NexusColor.AmberBorder, RoundedCornerShape(NexusRadius.Card))
            .padding(14.dp),
    ) {
        Text(
            text = "DUE SOON",
            style = NexusType.Caption.copy(fontWeight = FontWeight.Bold),
            color = NexusColor.Amber,
        )
        Spacer(Modifier.height(8.dp))
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = item.title,
                    style = NexusType.CardTitleSemi,
                    color = NexusColor.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = item.due,
                    style = NexusType.Meta,
                    color = NexusColor.Amber,
                )
            }
        }
    }
}

@Composable
private fun FileRow(file: FileEntry, onClick: () -> Unit, onStar: () -> Unit) {
    val meta = remember(file.uri, file.modifiedMillis, file.sizeBytes) {
        buildString {
            append(relativeDay(file.modifiedMillis))
            if (file.sizeBytes > 0) {
                append(" · ")
                append(formatSize(file.sizeBytes))
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
    ) {
        FileTypeTile(extension = file.extension)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = NexusType.CardTitleSemi,
                color = NexusColor.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = file.dueLabel ?: meta,
                style = NexusType.Meta,
                color = if (file.dueLabel != null) NexusColor.Amber else NexusColor.TextSecondary,
                maxLines = 1,
            )
        }
        Text(
            text = if (file.starred) "★" else "☆",
            style = NexusType.SectionHeader,
            color = if (file.starred) NexusColor.Star else NexusColor.TextFaint,
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onStar),
        )
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1024 -> "${bytes / 1024} KB"
    else -> "$bytes B"
}

private fun relativeDay(millis: Long): String {
    if (millis <= 0) return "unknown"
    val elapsed = System.currentTimeMillis() - millis
    val hours = elapsed / 3_600_000
    return when {
        hours < 1 -> "just now"
        hours < 24 -> "${hours}h ago"
        hours < 48 -> "yesterday"
        else -> java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
            .format(java.util.Date(millis))
    }
}
