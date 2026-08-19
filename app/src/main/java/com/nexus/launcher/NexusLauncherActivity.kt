package com.nexus.launcher

import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.nexus.launcher.data.StatusBarGesture
import com.nexus.launcher.domain.AppEntry
import com.nexus.launcher.domain.NotificationCard
import com.nexus.launcher.domain.RomEntry
import com.nexus.launcher.integration.apps.AppLock
import com.nexus.launcher.integration.claude.ClaudeHomeLine
import com.nexus.launcher.integration.emulators.Emulators
import com.nexus.launcher.integration.media.DeepLinks
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.launcher.ui.common.DefaultLauncherCard
import com.nexus.launcher.ui.common.LocalWallpaperBitmap
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

    /** Bumped on resume to re-run checks that depend on system state. */
    private var resumeTick by mutableIntStateOf(0)

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
        handleIntent(intent)

        setContent {
            val profile by rememberWindowProfile(this)
            val clock = rememberClockText()

            // Read once and share: every hub page blurs the same bitmap.
            val wallpaperBitmap by app.wallpaperSource.wallpaper.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) { app.wallpaperSource.refresh() }
            val wallpaperImage = remember(wallpaperBitmap) {
                wallpaperBitmap?.asImageBitmap()
            }

            NexusTheme(windowProfile = profile) {
                CompositionLocalProvider(LocalWallpaperBitmap provides wallpaperImage) {
                    val settings by viewModel.settings.collectAsStateWithLifecycle()

                    // Re-checked on every resume: the user may have made Nexus
                    // the default from system settings while we were away.
                    val needsDefaultPrompt = !settings.defaultLauncherPromptSeen &&
                        !isDefaultHome(resumeTick)

                    // Re-applied on resume too: returning from another app, or
                    // from the system shade, restores the bars behind our back.
                    LaunchedEffect(settings.hideStatusBar, settings.statusBarGesture, resumeTick) {
                        applySystemBars(settings.hideStatusBar, settings.statusBarGesture)
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        LauncherScreen(
                            viewModel = viewModel,
                            clock = clock,
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

                        if (needsDefaultPrompt) {
                            DefaultLauncherCard(
                                modifier = Modifier.fillMaxSize(),
                                onSetDefault = {
                                    viewModel.updateSettings {
                                        it.copy(defaultLauncherPromptSeen = true)
                                    }
                                    openHomeSettings()
                                },
                                onLater = {
                                    viewModel.updateSettings {
                                        it.copy(defaultLauncherPromptSeen = true)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Applies the status-bar preference to the window.
     *
     * [StatusBarGesture] decides how a hidden bar comes back:
     * [StatusBarGesture.SwipeDownTopEdge] lets a swipe from the top edge pull it
     * in transiently, [StatusBarGesture.Never] keeps it hidden until the
     * preference itself changes, and [StatusBarGesture.Always] keeps the bar on
     * regardless of the hide toggle.
     */
    private fun applySystemBars(hide: Boolean, gesture: StatusBarGesture) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)

        if (!hide || gesture == StatusBarGesture.Always) {
            controller.show(WindowInsetsCompat.Type.statusBars())
            return
        }

        controller.systemBarsBehavior = if (gesture == StatusBarGesture.SwipeDownTopEdge) {
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        }
        controller.hide(WindowInsetsCompat.Type.statusBars())
    }

    /**
     * True when Nexus is the activity the system would start for HOME. Keyed on
     * [resumeTick] so the check re-runs after a trip to system settings.
     */
    private fun isDefaultHome(@Suppress("UNUSED_PARAMETER") tick: Int): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = runCatching {
            packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }.getOrNull()
        return resolved?.activityInfo?.packageName == packageName
    }

    /**
     * The window token the overlay parents itself to only exists once the window
     * is attached. Attaching here rather than waiting for onResume is why the
     * feed is ready on the first swipe instead of after a trip through recents.
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        (application as NexusApp).discoverOverlay.attachWindow(this)
    }

    override fun onDetachedFromWindow() {
        (application as NexusApp).discoverOverlay.detachWindow(isChangingConfigurations)
        super.onDetachedFromWindow()
    }

    override fun onStart() {
        super.onStart()
        val overlay = (application as NexusApp).discoverOverlay
        overlay.connect()
        overlay.onActivityStarted()
    }

    override fun onResume() {
        super.onResume()
        val app = application as NexusApp
        app.nowPlaying.start()
        app.widgetHost.startListening()
        viewModel.refreshApps()

        // The window token only exists once the window is attached, so the
        // overlay attach happens here rather than in onCreate.
        app.discoverOverlay.attachWindow(this)
        app.discoverOverlay.onActivityResumed()
        resumeTick++
    }

    override fun onPause() {
        super.onPause()
        val app = application as NexusApp
        app.widgetHost.stopListening()
        app.discoverOverlay.onActivityPaused()
    }

    override fun onStop() {
        super.onStop()
        (application as NexusApp).discoverOverlay.onActivityStopped()
    }

    override fun onDestroy() {
        val app = application as NexusApp
        app.discoverOverlay.detachWindow(isChangingConfigurations)
        app.discoverOverlay.disconnect()
        super.onDestroy()
        app.nowPlaying.stop()
    }

    /**
     * A HOME press while the launcher is already foreground arrives as a new
     * intent. That is the signal to close overlays and scroll back to Home.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == ACTION_OPEN_SETTINGS) {
            overlayRoute = OverlayRoute.Settings
            return
        }
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            overlayRoute = OverlayRoute.None
            homePressCount++
        }
    }

    private val host = object : LauncherHost {
        override val claudeLine: ClaudeHomeLine?
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

    companion object {
        /** Sent by [NexusSettingsActivity] to open the settings overlay. */
        const val ACTION_OPEN_SETTINGS = "com.nexus.launcher.OPEN_SETTINGS"
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
