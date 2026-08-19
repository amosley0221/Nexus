package com.nexus.launcher

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.nexus.launcher.domain.AppEntry
import com.nexus.launcher.domain.NotificationCard
import com.nexus.launcher.domain.RomEntry
import com.nexus.launcher.integration.apps.AppLock
import com.nexus.launcher.integration.emulators.Emulators
import com.nexus.launcher.integration.media.DeepLinks
import com.nexus.launcher.ui.LauncherHost
import com.nexus.launcher.ui.LauncherScreen
import com.nexus.launcher.ui.LauncherViewModel
import com.nexus.launcher.ui.clock.rememberClockText
import com.nexus.launcher.ui.layout.rememberWindowProfile
import com.nexus.launcher.ui.overlay.LauncherOverlay
import com.nexus.launcher.ui.overlay.OverlayRoute
import com.nexus.launcher.ui.theme.NexusColor
import com.nexus.launcher.ui.theme.NexusTheme

/**
 * The single HOME activity. Everything the launcher shows lives here; there is no
 * back stack to restore, and the window stays transparent so the system wallpaper
 * is the Home background.
 */
class NexusLauncherActivity : FragmentActivity() {

    private lateinit var viewModel: LauncherViewModel

    /** Bumped each time HOME is pressed while already on the launcher. */
    private var homePressCount by mutableIntStateOf(0)
    private var overlayRoute by mutableStateOf<OverlayRoute>(OverlayRoute.None)
    private var pendingWidgetPageId: String? = null

    private val folderPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        runCatching {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        viewModel.addRomFolder(uri.toString())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        viewModel = ViewModelProvider(this)[LauncherViewModel::class.java]

        val app = application as NexusApp

        setContent {
            val profile by rememberWindowProfile(this)
            val clock = rememberClockText()

            NexusTheme(windowProfile = profile) {
                Box(modifier = Modifier.fillMaxSize()) {
                    LauncherScreen(
                        viewModel = viewModel,
                        clockText = clock.time,
                        dateText = clock.date,
                        host = host,
                        modifier = Modifier.fillMaxSize(),
                    )

                    // Edit mode and settings render as full-screen overlays over
                    // the pager rather than separate activities, so the launcher
                    // never leaves its single task.
                    AnimatedVisibility(
                        visible = overlayRoute != OverlayRoute.None,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(NexusColor.HubBackground)
                        ) {
                            LauncherOverlay(
                                route = overlayRoute,
                                viewModel = viewModel,
                                host = host,
                                widgetHost = app.widgetHost,
                                onRoute = { overlayRoute = it },
                                onClose = { overlayRoute = OverlayRoute.None },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (application as NexusApp).nowPlaying.start()
        (application as NexusApp).widgetHost.startListening()
        viewModel.refreshApps()
    }

    override fun onPause() {
        super.onPause()
        (application as NexusApp).widgetHost.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as NexusApp).nowPlaying.stop()
    }

    /**
     * A HOME press while the launcher is already foreground arrives as a new
     * intent. That is the signal to close overlays and scroll back to Home.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            overlayRoute = OverlayRoute.None
            homePressCount++
        }
    }

    private val host = object : LauncherHost {
        override val claudeLine: String?
            get() = (application as NexusApp).claudeBridge.homeLine

        override val homePressCount: Int
            get() = this@NexusLauncherActivity.homePressCount

        override fun launchApp(entry: AppEntry, bounds: androidx.compose.ui.geometry.Rect?) {
            val rect = bounds?.let {
                android.graphics.Rect(
                    it.left.toInt(),
                    it.top.toInt(),
                    it.right.toInt(),
                    it.bottom.toInt(),
                )
            }
            val repo = (application as NexusApp).appRepository

            if (viewModel.isLocked(entry)) {
                AppLock.authenticate(
                    activity = this@NexusLauncherActivity,
                    appLabel = entry.label,
                    onSuccess = { repo.launch(entry, rect) },
                )
            } else {
                repo.launch(entry, rect)
            }
        }

        override fun openAppOptions(entry: AppEntry) {
            overlayRoute = OverlayRoute.AppOptions(entry)
        }

        override fun openEditMode() {
            if (overlayRoute == OverlayRoute.None) overlayRoute = OverlayRoute.PageManager
        }

        override fun openSettings() {
            overlayRoute = OverlayRoute.Settings
        }

        override fun openClaudeFeed() {
            overlayRoute = OverlayRoute.ClaudeFeed
        }

        override fun openClock() {
            runCatching {
                startActivity(
                    Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }

        override fun openNotification(card: NotificationCard) {
            runCatching {
                packageManager.getLaunchIntentForPackage(card.packageName)?.let { intent ->
                    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        }

        override fun launchRom(rom: RomEntry) {
            if (!Emulators.launch(this@NexusLauncherActivity, rom)) {
                overlayRoute = OverlayRoute.RomOptions(rom)
            }
        }

        override fun openRomOptions(rom: RomEntry) {
            overlayRoute = OverlayRoute.RomOptions(rom)
        }

        override fun pickRomFolder() {
            runCatching { folderPicker.launch(null) }
        }

        override fun openDeepLink(target: String) {
            val context = this@NexusLauncherActivity
            when {
                target.startsWith("twitch:") ->
                    DeepLinks.twitchStream(context, target.removePrefix("twitch:"))

                target.startsWith("applemusic:") ->
                    DeepLinks.appleMusic(context, target.removePrefix("applemusic:"))

                target.startsWith("plex:") ->
                    DeepLinks.plex(context, target.removePrefix("plex:").ifBlank { null })

                target.startsWith("stream:") -> when (target.removePrefix("stream:")) {
                    "Netflix" -> DeepLinks.netflix(context)
                    "Disney+" -> DeepLinks.disneyPlus(context)
                    else -> DeepLinks.primeVideo(context)
                }

                target.startsWith("file:") -> runCatching {
                    startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(target.removePrefix("file:")))
                            .addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                    )
                }

                target.startsWith("http") -> DeepLinks.web(context, target)
            }
        }

        override fun openSearch() {
            overlayRoute = OverlayRoute.Settings
        }
    }

    /** Opens the system picker so the user can make Nexus their Home app. */
    fun openHomeSettings() {
        runCatching {
            startActivity(
                Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
