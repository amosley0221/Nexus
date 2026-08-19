package com.nexus.launcher.ui.common

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.launcher.ui.theme.NexusColor
import com.nexus.launcher.ui.theme.NexusRadius
import com.nexus.launcher.ui.theme.NexusType

/** One tappable row inside an options sheet. */
data class SheetAction(
    val label: String,
    val detail: String? = null,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * The long-press options sheet used by games, ROMs, and apps: artwork, title,
 * meta line, a filled primary button, then plain action rows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsSheet(
    title: String,
    meta: String,
    artSeed: String,
    actions: List<SheetAction>,
    modifier: Modifier = Modifier,
    primaryLabel: String? = null,
    onPrimary: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NexusColor.HubBackground,
        contentColor = NexusColor.TextPrimary,
        dragHandle = null,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                GradientArt(seed = artSeed, modifier = Modifier.size(64.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = NexusType.SectionHeader.copy(fontSize = 18.sp),
                        color = NexusColor.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (meta.isNotBlank()) {
                        Text(
                            text = meta,
                            style = NexusType.BodySmall,
                            color = NexusColor.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            if (primaryLabel != null) {
                Spacer(Modifier.height(18.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(NexusRadius.Button))
                        .background(NexusColor.Accent)
                        .clickable(onClick = onPrimary)
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = primaryLabel,
                        style = NexusType.CardTitle,
                        color = NexusColor.DeepMaroonText,
                        maxLines = 1,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            actions.forEach { action ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = action.onClick)
                        .padding(vertical = 13.dp),
                ) {
                    Text(
                        text = action.label,
                        style = NexusType.CardTitleSemi,
                        color = if (action.destructive) NexusColor.Negative else NexusColor.TextPrimary,
                    )
                    if (action.detail != null) {
                        Text(
                            text = action.detail,
                            style = NexusType.Meta,
                            color = NexusColor.TextSecondary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}
