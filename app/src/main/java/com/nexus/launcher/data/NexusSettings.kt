package com.nexus.launcher.data

import kotlinx.serialization.Serializable

@Serializable
enum class IconPackMode { NexusBlush, SystemIcons, ThirdParty }

@Serializable
enum class StatusBarGesture { SwipeDownTopEdge, Always, Never }

/** Everything under Nexus Settings, persisted as one JSON blob in DataStore. */
@Serializable
data class NexusSettings(
    // LAYOUT
    val appListColumnsFolded: Int = 1,
    val appListColumnsUnfolded: Int = 2,
    val iconSizeDp: Int = 38,
    val labelSizeSp: Int = 17,
    val iconPack: IconPackMode = IconPackMode.NexusBlush,
    val thirdPartyIconPackPackage: String? = null,
    val hideStatusBar: Boolean = false,
    val statusBarGesture: StatusBarGesture = StatusBarGesture.SwipeDownTopEdge,

    // APPS
    val favorites: List<String> = emptyList(),
    val hiddenApps: Set<String> = emptySet(),
    val widgetsEnabled: Boolean = true,
    val discoverEnabled: Boolean = true,
    val maxHomeFavorites: Int = 10,

    // SECURITY
    val appLockEnabled: Boolean = false,
    val lockedApps: Set<String> = emptySet(),
    val requireBiometric: Boolean = true,
    val relockImmediately: Boolean = true,

    // INTEGRATIONS
    val plexServerUrl: String = "",
    val plexToken: String = "",
    val twitchToken: String = "",
    val twitchLogin: String = "",
    val claudeRelayUrl: String = "",
    val romFolderUris: List<String> = emptyList(),
    val emulatorMap: Map<String, String> = emptyMap(),

    // HOME
    val showClaudeStatus: Boolean = true,
    val useBundledWallpaper: Boolean = false,
)
