package com.nexus.launcher.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.launcher.data.IconPackMode
import com.nexus.launcher.data.NexusSettings
import com.nexus.launcher.domain.AppEntry
import com.nexus.launcher.domain.FolderContents
import com.nexus.launcher.ui.theme.NexusColor
import com.nexus.launcher.ui.theme.NexusRadius
import com.nexus.launcher.ui.theme.NexusType

/**
 * Second step of the long-press menu: which category this app belongs to.
 * Existing folders first, then the option to start a new one — the same shape
 * as picking a playlist.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToCategorySheet(
    entry: AppEntry,
    folders: List<FolderContents>,
    settings: NexusSettings,
    onAddTo: (FolderContents) -> Unit,
    onCreateFolder: (String) -> Unit,
    onRemoveFromFolder: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var naming by remember { mutableStateOf(false) }
    val currentFolder = remember(folders, entry.key) {
        folders.firstOrNull { folder -> folder.apps.any { it.key == entry.key } }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NexusColor.HubBackground,
        contentColor = NexusColor.TextPrimary,
        dragHandle = null,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp)) {
            Text(
                text = "Add to category",
                style = NexusType.SectionHeader.copy(fontSize = 20.sp),
                color = NexusColor.TextPrimary,
            )
            Text(
                text = entry.label,
                style = NexusType.BodySmall,
                color = NexusColor.TextSecondary,
            )

            Spacer(Modifier.height(16.dp))

            folders.forEach { folder ->
                val isCurrent = folder.id == currentFolder?.id
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAddTo(folder) }
                        .padding(vertical = 12.dp),
                ) {
                    FolderTile(folder = folder, iconPack = settings.iconPack, size = 38.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = folder.name,
                            style = NexusType.CardTitleSemi,
                            color = NexusColor.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (isCurrent) {
                                "Already in this folder"
                            } else {
                                "${folder.apps.size} apps"
                            },
                            style = NexusType.Meta,
                            color = NexusColor.TextSecondary,
                        )
                    }
                }
            }

            if (folders.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(NexusColor.Border),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { naming = true }
                    .padding(vertical = 14.dp),
            ) {
                Text(
                    text = "Create folder",
                    style = NexusType.CardTitleSemi,
                    color = NexusColor.TextPrimary,
                )
                Text(
                    text = "Groups apps together and takes them out of the A–Z list",
                    style = NexusType.Meta,
                    color = NexusColor.TextSecondary,
                )
            }

            if (currentFolder != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onRemoveFromFolder)
                        .padding(vertical = 14.dp),
                ) {
                    Text(
                        text = "Remove from ${currentFolder.name}",
                        style = NexusType.CardTitleSemi,
                        color = NexusColor.Negative,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }

    if (naming) {
        TextPromptDialog(
            title = "New folder",
            initial = "",
            confirmLabel = "Create",
            onConfirm = { name ->
                naming = false
                onCreateFolder(name)
            },
            onDismiss = { naming = false },
        )
    }
}

/**
 * A folder, opened. Tapping a member launches it; long-pressing one opens its
 * own options, so an app can be taken back out from where it lives.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FolderSheet(
    folder: FolderContents,
    settings: NexusSettings,
    onLaunch: (AppEntry) -> Unit,
    onLongPressApp: (AppEntry) -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var renaming by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NexusColor.HubBackground,
        contentColor = NexusColor.TextPrimary,
        dragHandle = null,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                FolderTile(folder = folder, iconPack = settings.iconPack, size = 56.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folder.name,
                        style = NexusType.SectionHeader.copy(fontSize = 18.sp),
                        color = NexusColor.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${folder.apps.size} apps",
                        style = NexusType.BodySmall,
                        color = NexusColor.TextSecondary,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                items(folder.apps, key = { it.key }) { app ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onLaunch(app) },
                                onLongClick = { onLongPressApp(app) },
                            )
                            .padding(vertical = 9.dp),
                    ) {
                        AppIcon(entry = app, size = 38.dp, mode = settings.iconPack)
                        Text(
                            text = app.label,
                            style = NexusType.CardTitleSemi,
                            color = NexusColor.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(NexusColor.Border),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { renaming = true }
                    .padding(vertical = 13.dp),
            ) {
                Text("Rename folder", style = NexusType.CardTitleSemi, color = NexusColor.TextPrimary)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDelete)
                    .padding(vertical = 13.dp),
            ) {
                Text(
                    text = "Delete folder",
                    style = NexusType.CardTitleSemi,
                    color = NexusColor.Negative,
                )
                Text(
                    text = "Its apps go back to the A–Z list; nothing is uninstalled",
                    style = NexusType.Meta,
                    color = NexusColor.TextSecondary,
                )
            }

            Spacer(Modifier.height(12.dp))
        }
    }

    if (renaming) {
        TextPromptDialog(
            title = "Rename folder",
            initial = folder.name,
            confirmLabel = "Rename",
            onConfirm = { name ->
                renaming = false
                onRename(name)
            },
            onDismiss = { renaming = false },
        )
    }
}

/** A folder's tile: a quarter-grid of the first four members. */
@Composable
fun FolderTile(
    folder: FolderContents,
    iconPack: IconPackMode,
    size: androidx.compose.ui.unit.Dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
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
                        AppIcon(entry = app, size = size * 0.38f, mode = iconPack)
                    }
                }
            }
        }
    }
}

/** One-field prompt, used for naming and renaming folders. */
@Composable
fun TextPromptDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NexusColor.HubBackground,
        shape = RoundedCornerShape(NexusRadius.Card),
        title = {
            Text(title, style = NexusType.SectionHeader.copy(fontSize = 18.sp), color = NexusColor.TextPrimary)
        },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = NexusColor.TextPrimary,
                    unfocusedTextColor = NexusColor.TextPrimary,
                    focusedBorderColor = NexusColor.Accent,
                    unfocusedBorderColor = NexusColor.Border,
                    cursorColor = NexusColor.Accent,
                ),
            )
        },
        confirmButton = {
            Text(
                text = confirmLabel,
                style = NexusType.CardTitleSemi,
                color = if (value.isBlank()) NexusColor.TextFaint else NexusColor.Accent,
                modifier = Modifier
                    .clickable(enabled = value.isNotBlank()) { onConfirm(value) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        },
        dismissButton = {
            Text(
                text = "Cancel",
                style = NexusType.CardTitleSemi,
                color = NexusColor.TextSecondary,
                modifier = Modifier
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        },
    )
}
