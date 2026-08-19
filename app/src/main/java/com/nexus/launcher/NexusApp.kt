package com.nexus.launcher

import android.app.Application
import com.nexus.launcher.data.SettingsRepository
import com.nexus.launcher.integration.apps.AppRepository
import com.nexus.launcher.integration.claude.ClaudeBridge
import com.nexus.launcher.integration.discover.DiscoverOverlayController
import com.nexus.launcher.integration.media.NowPlayingController
import com.nexus.launcher.integration.widgets.NexusWidgetHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers

/**
 * Process-wide singletons. A launcher is a long-lived, single-activity process,
 * so these outlive the activity across fold/rotation recreation and keep the app
 * list warm instead of rescanning on every configuration change.
 */
class NexusApp : Application() {

    val scope: CoroutineScope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val appRepository: AppRepository by lazy { AppRepository(this) }
    val nowPlaying: NowPlayingController by lazy { NowPlayingController(this) }
    val claudeBridge: ClaudeBridge by lazy { ClaudeBridge(scope) }
    val widgetHost: NexusWidgetHost by lazy { NexusWidgetHost(this) }
    val discoverOverlay: DiscoverOverlayController by lazy { DiscoverOverlayController(this) }
}
