package com.nexus.launcher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.launcher.data.NexusSettings
import com.nexus.launcher.domain.AppEntry
import com.nexus.launcher.domain.NotificationCard
import com.nexus.launcher.ui.common.AppIcon
import com.nexus.launcher.ui.common.Squircle
import com.nexus.launcher.ui.theme.NexusColor
import com.nexus.launcher.ui.theme.NexusRadius
import com.nexus.launcher.ui.theme.NexusType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pull-past-bottom search: dark scrim, maroon search pill, results beneath, and
 * a keyboard FAB bottom-right. Searches installed apps by name; the same field
 * also matches the hub pages so "budget" jumps rather than returning nothing.
 */
@Composable
fun SearchOverscroll(
    query: String,
    results: List<AppEntry>,
    recentlyInstalled: List<AppEntry>,
    settings: NexusSettings,
    modifier: Modifier = Modifier,
    onQueryChange: (String) -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NexusColor.OverscrollScrim)
            .clickable(onClick = onDismiss),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 28.dp, bottom = 24.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(NexusRadius.SearchPill))
                    .background(NexusColor.MaroonSurface)
                    .padding(horizontal = 22.dp, vertical = 14.dp),
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = NexusType.SearchPill.copy(color = NexusColor.OnWallpaper),
                    cursorBrush = SolidColor(NexusColor.Accent),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(
                                text = "Search apps",
                                style = NexusType.SearchPill,
                                color = Color(0xFFC9A8A2),
                            )
                        }
                        inner()
                    },
                )
            }

            Spacer(Modifier.height(18.dp))

            val shown = if (query.isBlank()) recentlyInstalled else results
            if (query.isBlank()) {
                Text(
                    text = "Recently installed",
                    style = NexusType.SectionHeader,
                    color = NexusColor.TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(shown, key = { it.key }) { app ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLaunch(app) }
                            .padding(vertical = 7.dp),
                    ) {
                        AppIcon(entry = app, size = 38.dp, mode = settings.iconPack)
                        Text(
                            text = app.label,
                            style = NexusType.AppLabel,
                            color = NexusColor.OnWallpaper,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        // Keyboard FAB
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(58.dp)
                .clip(CircleShape)
                .background(NexusColor.IconSquircle)
                .clickable { runCatching { focusRequester.requestFocus() } },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Keyboard,
                contentDescription = "Show keyboard",
                tint = NexusColor.Glyph,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

/**
 * Pull-down-at-top notification cards. Without notification access granted the
 * panel explains how to turn it on instead of showing an empty sheet.
 */
@Composable
fun NotificationsOverscroll(
    cards: List<NotificationCard>,
    accessGranted: Boolean,
    modifier: Modifier = Modifier,
    onCardClick: (NotificationCard) -> Unit,
    onGrantAccess: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NexusColor.OverscrollScrim)
            .clickable(onClick = onDismiss),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 44.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!accessGranted) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(NexusRadius.CardLarge))
                        .background(NexusColor.MaroonTranslucent)
                        .clickable(onClick = onGrantAccess)
                        .padding(18.dp),
                ) {
                    Column {
                        Text(
                            text = "Turn on notification access",
                            style = NexusType.CardTitle,
                            color = NexusColor.OnWallpaper,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Nexus needs it to show your notifications here.",
                            style = NexusType.Meta,
                            color = NexusColor.Accent,
                        )
                    }
                }
                return@Column
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(cards, key = { it.key }) { card ->
                    NotificationRow(card = card, onClick = { onCardClick(card) })
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(card: NotificationCard, onClick: () -> Unit) {
    val time = remember(card.whenMillis) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(card.whenMillis))
    }
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NexusRadius.CardLarge))
            .background(NexusColor.MaroonTranslucent)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Squircle(size = 32.dp) {
            Text(
                text = card.appName.firstOrNull()?.uppercase() ?: "?",
                style = NexusType.CardTitle,
                color = NexusColor.Glyph,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = card.appName,
                    style = NexusType.CardTitle,
                    color = NexusColor.OnWallpaper,
                    maxLines = 1,
                )
                Text(text = time, style = NexusType.Meta, color = NexusColor.Accent)
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = listOf(card.title, card.text).filter { it.isNotBlank() }.joinToString(" · "),
                style = NexusType.Meta.copy(fontSize = 12.sp),
                color = NexusColor.TextSecondary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}
