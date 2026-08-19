package com.nexus.launcher.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.launcher.data.IconPackMode
import com.nexus.launcher.data.NexusSettings
import com.nexus.launcher.data.StatusBarGesture
import com.nexus.launcher.ui.common.ChipRow
import com.nexus.launcher.ui.hub.HubScaffold
import com.nexus.launcher.ui.theme.NexusColor
import com.nexus.launcher.ui.theme.NexusType

/** Nexus Settings: LAYOUT, APPS, SECURITY, and integration endpoints. */
@Composable
fun SettingsPage(
    settings: NexusSettings,
    usageAccessGranted: Boolean,
    notificationAccessGranted: Boolean,
    modifier: Modifier = Modifier,
    onUpdate: ((NexusSettings) -> NexusSettings) -> Unit,
    onOpenAppLock: () -> Unit,
    onOpenHiddenApps: () -> Unit,
    onOpenFavorites: () -> Unit,
    onRequestUsageAccess: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onPickRomFolder: () -> Unit,
    onOpenIntegrations: () -> Unit,
    onSetDefaultLauncher: () -> Unit,
    discoverDiagnostics: String,
    onDone: () -> Unit,
) {
    HubScaffold(
        title = "Nexus",
        modifier = modifier,
        subtitle = {
            Text(
                text = "Launcher settings",
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
        LazyColumn(modifier = Modifier.weight(1f)) {
            item { SettingsSection("LAYOUT") }

            item {
                StepperRow(
                    title = "App list columns (folded)",
                    value = settings.appListColumnsFolded,
                    range = 1..3,
                    onChange = { value -> onUpdate { it.copy(appListColumnsFolded = value) } },
                )
            }
            item {
                StepperRow(
                    title = "App list columns (unfolded)",
                    value = settings.appListColumnsUnfolded,
                    range = 1..4,
                    onChange = { value -> onUpdate { it.copy(appListColumnsUnfolded = value) } },
                )
            }
            item {
                SliderRow(
                    title = "Icon size",
                    valueLabel = "${settings.iconSizeDp} dp",
                    value = settings.iconSizeDp.toFloat(),
                    range = 28f..56f,
                    onChange = { value -> onUpdate { it.copy(iconSizeDp = value.toInt()) } },
                )
            }
            item {
                SliderRow(
                    title = "Label size",
                    valueLabel = "${settings.labelSizeSp} sp",
                    value = settings.labelSizeSp.toFloat(),
                    range = 12f..24f,
                    onChange = { value -> onUpdate { it.copy(labelSizeSp = value.toInt()) } },
                )
            }
            item {
                ChipSettingRow(
                    title = "Icon pack",
                    options = listOf("Nexus Blush", "System icons", "Third-party"),
                    selectedIndex = when (settings.iconPack) {
                        IconPackMode.NexusBlush -> 0
                        IconPackMode.SystemIcons -> 1
                        IconPackMode.ThirdParty -> 2
                    },
                    onSelect = { index ->
                        onUpdate {
                            it.copy(
                                iconPack = when (index) {
                                    0 -> IconPackMode.NexusBlush
                                    1 -> IconPackMode.SystemIcons
                                    else -> IconPackMode.ThirdParty
                                }
                            )
                        }
                    },
                )
            }
            item {
                ToggleRow(
                    title = "Hide status bar",
                    subtitle = "Swipe down at the top edge to reveal it",
                    checked = settings.hideStatusBar,
                    onCheckedChange = { value -> onUpdate { it.copy(hideStatusBar = value) } },
                )
            }
            item {
                ChipSettingRow(
                    title = "Status-bar gesture",
                    options = listOf("Swipe down", "Always show", "Never show"),
                    selectedIndex = when (settings.statusBarGesture) {
                        StatusBarGesture.SwipeDownTopEdge -> 0
                        StatusBarGesture.Always -> 1
                        StatusBarGesture.Never -> 2
                    },
                    onSelect = { index ->
                        onUpdate {
                            it.copy(
                                statusBarGesture = when (index) {
                                    0 -> StatusBarGesture.SwipeDownTopEdge
                                    1 -> StatusBarGesture.Always
                                    else -> StatusBarGesture.Never
                                }
                            )
                        }
                    },
                )
            }

            item { SettingsSection("APPS") }

            item {
                NavigationRow(
                    title = "Home favourites",
                    subtitle = "${settings.favorites.size} pinned",
                    onClick = onOpenFavorites,
                )
            }
            item {
                NavigationRow(
                    title = "Hidden apps",
                    subtitle = "${settings.hiddenApps.size} hidden",
                    onClick = onOpenHiddenApps,
                )
            }
            item {
                ToggleRow(
                    title = "Widgets",
                    subtitle = "Allow widgets on pages",
                    checked = settings.widgetsEnabled,
                    onCheckedChange = { value -> onUpdate { it.copy(widgetsEnabled = value) } },
                )
            }
            item {
                ToggleRow(
                    title = "Discover feed",
                    subtitle = "Show the leftmost Google feed page",
                    checked = settings.discoverEnabled,
                    onCheckedChange = { value -> onUpdate { it.copy(discoverEnabled = value) } },
                )
            }
            item {
                ToggleRow(
                    title = "Claude status on Home",
                    subtitle = "Hidden automatically when nothing is running",
                    checked = settings.showClaudeStatus,
                    onCheckedChange = { value -> onUpdate { it.copy(showClaudeStatus = value) } },
                )
            }

            item { SettingsSection("SECURITY") }

            item {
                ToggleRow(
                    title = "App lock",
                    subtitle = "Biometric gate for chosen apps",
                    checked = settings.appLockEnabled,
                    onCheckedChange = { value -> onUpdate { it.copy(appLockEnabled = value) } },
                )
            }
            item {
                NavigationRow(
                    title = "Locked apps",
                    subtitle = "${settings.lockedApps.size} locked",
                    onClick = onOpenAppLock,
                )
            }
            item {
                ToggleRow(
                    title = "Re-lock immediately on exit",
                    subtitle = "Ask again as soon as you leave the app",
                    checked = settings.relockImmediately,
                    onCheckedChange = { value -> onUpdate { it.copy(relockImmediately = value) } },
                )
            }

            item { SettingsSection("PERMISSIONS") }

            item {
                NavigationRow(
                    title = "Usage access",
                    subtitle = if (usageAccessGranted) {
                        "Granted — playtime shown on game cards"
                    } else {
                        "Not granted — tap to enable playtime"
                    },
                    accent = !usageAccessGranted,
                    onClick = onRequestUsageAccess,
                )
            }
            item {
                NavigationRow(
                    title = "Notification access",
                    subtitle = if (notificationAccessGranted) {
                        "Granted — pull down on Home for cards"
                    } else {
                        "Not granted — tap to enable notification cards"
                    },
                    accent = !notificationAccessGranted,
                    onClick = onRequestNotificationAccess,
                )
            }
            item {
                NavigationRow(
                    title = "Set Nexus as default launcher",
                    subtitle = "Opens the system Home app picker",
                    onClick = onSetDefaultLauncher,
                )
            }

            item { SettingsSection("INTEGRATIONS") }

            item {
                NavigationRow(
                    title = "ROM folders",
                    subtitle = if (settings.romFolderUris.isEmpty()) {
                        "No folder granted — tap to pick one"
                    } else {
                        "${settings.romFolderUris.size} folder(s) granted"
                    },
                    onClick = onPickRomFolder,
                )
            }
            item {
                NavigationRow(
                    title = "Plex, Twitch & Claude relay",
                    subtitle = "Server URLs and tokens",
                    onClick = onOpenIntegrations,
                )
            }
            item {
                // The Discover page is transparent whenever the overlay is live,
                // so when the feed does not appear there is nothing on it to
                // explain why. This is where that state is readable.
                NavigationRow(
                    title = "Discover feed status",
                    subtitle = discoverDiagnostics,
                    subtitleMaxLines = 5,
                    onClick = onOpenIntegrations,
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun SettingsSection(title: String) {
    Text(
        text = title,
        style = NexusType.Caption.copy(fontWeight = FontWeight.Bold),
        color = NexusColor.TextSecondary,
        modifier = Modifier.padding(top = 20.dp, bottom = 6.dp),
    )
}

@Composable
fun ToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = NexusType.CardTitleSemi, color = NexusColor.TextPrimary)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = NexusType.Meta,
                    color = NexusColor.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
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

@Composable
fun NavigationRow(
    title: String,
    subtitle: String?,
    accent: Boolean = false,
    subtitleMaxLines: Int = 2,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = NexusType.CardTitleSemi, color = NexusColor.TextPrimary)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = NexusType.Meta,
                    color = if (accent) NexusColor.Amber else NexusColor.TextSecondary,
                    maxLines = subtitleMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text("›", style = NexusType.SectionHeader, color = NexusColor.TextFaint)
    }
}

@Composable
private fun StepperRow(title: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    ) {
        Text(
            text = title,
            style = NexusType.CardTitleSemi,
            color = NexusColor.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "−",
                style = NexusType.PageTitle,
                color = if (value > range.first) NexusColor.Accent else NexusColor.TextFaint,
                modifier = Modifier.clickable {
                    if (value > range.first) onChange(value - 1)
                },
            )
            Text(value.toString(), style = NexusType.CardTitle, color = NexusColor.TextPrimary)
            Text(
                text = "+",
                style = NexusType.PageTitle,
                color = if (value < range.last) NexusColor.Accent else NexusColor.TextFaint,
                modifier = Modifier.clickable {
                    if (value < range.last) onChange(value + 1)
                },
            )
        }
    }
}

@Composable
private fun SliderRow(
    title: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = NexusType.CardTitleSemi,
                color = NexusColor.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(valueLabel, style = NexusType.Meta, color = NexusColor.Accent)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = NexusColor.Accent,
                activeTrackColor = NexusColor.Accent,
                inactiveTrackColor = NexusColor.Border,
            ),
        )
    }
}

@Composable
private fun ChipSettingRow(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 10.dp)) {
        Text(title, style = NexusType.CardTitleSemi, color = NexusColor.TextPrimary)
        Spacer(Modifier.height(8.dp))
        ChipRow(labels = options, selectedIndex = selectedIndex, onSelect = onSelect)
    }
}
