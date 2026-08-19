package com.nexus.launcher.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.nexus.launcher.domain.AppShortcut
import com.nexus.launcher.ui.theme.NexusColor
import com.nexus.launcher.ui.theme.NexusType

/**
 * A shortcut's own icon — a channel avatar, a contact photo. Falls back to the
 * first letter of the label, since a shortcut without an icon is common.
 */
@Composable
fun ShortcutIcon(shortcut: AppShortcut, size: Dp, modifier: Modifier = Modifier) {
    val bitmap = remember(shortcut.id, shortcut.packageName) {
        shortcut.icon?.let { drawable ->
            runCatching { drawable.toBitmap(width = 96, height = 96).asImageBitmap() }.getOrNull()
        }
    }

    val shape = RoundedCornerShape(size * 0.32f)

    if (bitmap != null) {
        Image(
            painter = BitmapPainter(bitmap),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(shape),
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(NexusColor.Card),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = shortcut.label.firstOrNull()?.uppercase() ?: "•",
                style = NexusType.CardTitle,
                color = NexusColor.Accent,
            )
        }
    }
}
