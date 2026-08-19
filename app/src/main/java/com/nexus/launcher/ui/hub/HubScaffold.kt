package com.nexus.launcher.ui.hub

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nexus.launcher.ui.common.HubBackground
import com.nexus.launcher.ui.common.HubHeader
import com.nexus.launcher.ui.common.LocalWallpaperBitmap
import com.nexus.launcher.ui.theme.LocalWindowProfile
import com.nexus.launcher.ui.theme.NexusColor
import com.nexus.launcher.ui.theme.NexusType

/**
 * Shared chrome for every hub page: the blurred-wallpaper hub surface painted
 * edge to edge, then a Baloo title with an optional trailing action and subtitle
 * above the page body.
 *
 * The background deliberately extends under the status and gesture bars — only
 * the *content* is inset — so no page ever exposes a band of the sharp Home
 * wallpaper. The bars themselves are the system's to draw; nothing here imitates
 * them.
 */
@Composable
fun HubScaffold(
    title: String,
    modifier: Modifier = Modifier,
    accent: Color = NexusColor.Accent,
    subtitle: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val profile = LocalWindowProfile.current
    val wallpaper = LocalWallpaperBitmap.current
    val systemBars = WindowInsets.systemBars.asPaddingValues()

    HubBackground(wallpaper = wallpaper, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(systemBars)
                .padding(horizontal = profile.pagePadding)
                .padding(top = 12.dp, bottom = PAGE_INDICATOR_CLEARANCE),
        ) {
            HubHeader(title = title, accent = accent, trailing = trailing)

            if (subtitle != null) {
                Spacer(Modifier.height(6.dp))
                subtitle()
            }

            Spacer(Modifier.height(14.dp))

            Column(modifier = Modifier.weight(1f)) { content() }
        }
    }
}

/** Vertical room the floating page-indicator dots need at the bottom of a page. */
val PAGE_INDICATOR_CLEARANCE = 30.dp

/** A section title row with an optional trailing link. */
@Composable
fun SectionRow(
    title: String,
    modifier: Modifier = Modifier,
    linkText: String? = null,
    onLinkClick: () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = NexusType.SectionHeader, color = NexusColor.TextPrimary)
        if (linkText != null) {
            Text(
                text = linkText,
                style = NexusType.Meta,
                color = NexusColor.Accent,
                modifier = Modifier.clickable(onClick = onLinkClick),
            )
        }
    }
}

/** Centered empty state used when an integration has nothing to show yet. */
@Composable
fun HubEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = NexusType.BodySmall,
                color = NexusColor.TextFaint,
            )
            if (action != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = action,
                    style = NexusType.CardTitle,
                    color = NexusColor.Accent,
                    modifier = Modifier.clickable(onClick = onAction),
                )
            }
        }
    }
}
