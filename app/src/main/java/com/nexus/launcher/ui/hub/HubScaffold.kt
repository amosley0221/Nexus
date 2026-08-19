package com.nexus.launcher.ui.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nexus.launcher.ui.common.GestureBar
import com.nexus.launcher.ui.common.HubHeader
import com.nexus.launcher.ui.theme.LocalWindowProfile
import com.nexus.launcher.ui.theme.NexusColor
import com.nexus.launcher.ui.theme.NexusType

/**
 * Shared chrome for every hub page: opaque dark background, status row, Baloo
 * title with optional trailing action and subtitle, then the page body.
 */
@Composable
fun HubScaffold(
    title: String,
    modifier: Modifier = Modifier,
    accent: Color = NexusColor.Accent,
    statusLeft: String = "",
    statusRight: String = "",
    subtitle: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val profile = LocalWindowProfile.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NexusColor.HubBackground)
            .padding(horizontal = profile.pagePadding)
            .padding(top = 18.dp, bottom = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(statusLeft, style = NexusType.StatusBar, color = NexusColor.TextSecondary)
            Text(statusRight, style = NexusType.StatusBar, color = NexusColor.TextSecondary)
        }

        Spacer(Modifier.height(16.dp))

        HubHeader(title = title, accent = accent, trailing = trailing)

        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            subtitle()
        }

        Spacer(Modifier.height(14.dp))

        Column(modifier = Modifier.weight(1f)) { content() }

        GestureBar(modifier = Modifier.padding(top = 12.dp, bottom = 2.dp))
    }
}

/** Right-aligned "View all →"-style link used across the hub sections. */
@Composable
fun SectionLink(text: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Text(
        text = text,
        style = NexusType.Meta,
        color = NexusColor.Accent,
        modifier = modifier.clickable(onClick = onClick),
    )
}

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
