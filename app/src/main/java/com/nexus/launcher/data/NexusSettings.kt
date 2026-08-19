package com.nexus.launcher.data

import com.nexus.launcher.domain.AppFolder
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
    /** User-made groups. Members drop out of the A-Z run and live here instead. */
    val folders: List<AppFolder> = emptyList(),

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
    /** Populates the Claude feed with sample tasks. Never affects the Home line. */
    val claudeSamplePreview: Boolean = false,
    val romFolderUris: List<String> = emptyList(),
    val emulatorMap: Map<String, String> = emptyMap(),

    // HOME
    val showClaudeStatus: Boolean = true,
    /** Weather beside the date. Off until asked for — it needs coarse location. */
    val showWeather: Boolean = false,
    val useBundledWallpaper: Boolean = false,
    /** Set once the default-home prompt has been answered either way. */
    val defaultLauncherPromptSeen: Boolean = false,
)
