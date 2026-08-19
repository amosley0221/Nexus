package com.nexus.launcher.integration.discover

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.compose.runtime.Immutable
import com.nexus.companion.INexusOverlay
import com.nexus.companion.INexusOverlayCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Immutable
data class DiscoverOverlayState(
    /** The companion APK is installed on the device. */
    val companionInstalled: Boolean = false,
    /** We are bound to the companion's bridge service. */
    val bridgeBound: Boolean = false,
    /** The companion holds a live binding to the Google app. */
    val overlayConnected: Boolean = false,
    /** The Google app reports a feed ready to draw. */
    val hasContent: Boolean = false,
    /** Why the feed is unavailable, or null when it is working. */
    val unavailableReason: String? = null,

    // Diagnostics. This is a reverse-engineered protocol against an app that
    // gives no error feedback — when the feed does not appear, these are the
    // only way to tell which step failed. Surfaced on the companion's screen.
    /** The Google app accepted the launcher's window. */
    val windowAttached: Boolean = false,
    /** Last status bitmask the Google app reported. -1 = never reported. */
    val lastStatus: Int = -1,
    /** Last scroll position the Google app echoed back. */
    val lastReportedScroll: Float = -1f,
    /** Started/resumed bits last sent to the Google app. */
    val lastActivityState: Int = 0,
) {
    /** True only when the feed can actually be shown. */
    val isUsable: Boolean get() = bridgeBound && overlayConnected
}

/**
 * Launcher-side half of the Discover bridge.
 *
 * Nexus cannot bind the Google app's overlay service itself — the Google app
 * decides whether to serve an overlay based on the calling package — so this
 * binds the companion APK instead and relays the same calls through it.
 *
 * The overlay is not a View that Nexus hosts. The Google app draws the feed into
 * its own window, parented to the launcher window's token, which is why
 * [attachWindow] hands over the activity's window attributes. The launcher's job
 * is only to drive scroll progress as the user swipes toward the Discover page.
 */
class DiscoverOverlayController(private val context: Context) {

    private val _state = MutableStateFlow(DiscoverOverlayState())
    val state: StateFlow<DiscoverOverlayState> = _state.asStateFlow()

    private var overlay: INexusOverlay? = null
    private var bound = false
    private var attachedActivity: Activity? = null

    /**
     * Current started/resumed bits.
     *
     * Android runs onStart and onResume *before* onAttachedToWindow, and the
     * bridge binds asynchronously on top of that — so the first activity-state
     * calls routinely land before there is anything to receive them. Keeping the
     * state here and re-sending it after every successful attach is what stops
     * the Google app from holding a window it thinks belongs to a paused
     * activity, which leaves the feed closed behind a blank page.
     */
    private var activityState: Int = 0

    /** Mirrors the overlay's own scroll so the pager can stay in step with it. */
    private val _overlayProgress = MutableStateFlow(0f)
    val overlayProgress: StateFlow<Float> = _overlayProgress.asStateFlow()

    private val callback = object : INexusOverlayCallback.Stub() {
        override fun overlayScrollChanged(progress: Float) {
            _overlayProgress.value = progress
            _state.value = _state.value.copy(lastReportedScroll = progress)
        }

        override fun overlayStatusChanged(status: Int) {
            _state.value = _state.value.copy(
                hasContent = status and STATUS_ATTACHED != 0,
                lastStatus = status,
            )
        }

        override fun companionStateChanged(connected: Boolean, detail: String?) {
            _state.value = _state.value.copy(
                overlayConnected = connected,
                unavailableReason = detail.takeIf { !connected },
            )
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val bridge = INexusOverlay.Stub.asInterface(service)
            overlay = bridge
            _state.value = _state.value.copy(
                bridgeBound = true,
                overlayConnected = runCatching { bridge.isConnected }.getOrDefault(false),
                unavailableReason = runCatching { bridge.unavailableReason }.getOrNull(),
            )
            // The activity may already be resumed by the time the bind lands.
            attachedActivity?.let { attachWindow(it) }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            overlay = null
            _state.value = _state.value.copy(
                bridgeBound = false,
                overlayConnected = false,
                unavailableReason = "The Nexus Companion service stopped.",
            )
        }

        override fun onNullBinding(name: ComponentName?) {
            overlay = null
            _state.value = _state.value.copy(
                bridgeBound = false,
                unavailableReason = "The Nexus Companion refused the bridge binding.",
            )
        }
    }

    fun connect() {
        val installed = isCompanionInstalled()
        _state.value = _state.value.copy(
            companionInstalled = installed,
            unavailableReason = if (installed) _state.value.unavailableReason else NOT_INSTALLED,
        )
        if (!installed || bound) return

        val intent = Intent(ACTION_OVERLAY_BRIDGE).setPackage(COMPANION_PACKAGE)
        bound = runCatching {
            context.bindService(
                intent,
                connection,
                Context.BIND_AUTO_CREATE or Context.BIND_ADJUST_WITH_ACTIVITY,
            )
        }.getOrElse { error ->
            Log.w(TAG, "Could not bind the companion bridge", error)
            false
        }

        if (!bound) {
            _state.value = _state.value.copy(
                unavailableReason = "Could not bind the Nexus Companion service. " +
                    "Install it from the same build as Nexus — the bridge is " +
                    "signature-checked.",
            )
        }
    }

    fun disconnect() {
        attachedActivity = null
        if (!bound) return
        bound = false
        overlay = null
        runCatching { context.unbindService(connection) }
        _state.value = _state.value.copy(bridgeBound = false, overlayConnected = false)
    }

    fun isCompanionInstalled(): Boolean =
        runCatching { context.packageManager.getPackageInfo(COMPANION_PACKAGE, 0) }.isSuccess

    /**
     * Gives the overlay the launcher window to draw into. Safe to call on every
     * resume — the Google app treats a repeat attach as a refresh.
     */
    fun attachWindow(activity: Activity) {
        attachedActivity = activity
        val bridge = overlay ?: return
        val attrs = activity.window?.attributes ?: return
        // Without a window token the Google app has nothing to parent its
        // window to, and the attach is silently dropped.
        if (attrs.token == null) return

        val attached = runCatching {
            bridge.attachWindow(
                attrs,
                activity.resources.configuration,
                CLIENT_OPTIONS,
                callback,
            )
        }.getOrElse { error ->
            Log.w(TAG, "attachWindow failed", error)
            false
        }

        if (attached) {
            // Re-assert everything the overlay was told before it had a window.
            applyActivityState()
            refreshContentFlag()
        } else {
            _state.value = _state.value.copy(
                unavailableReason = runCatching { bridge.unavailableReason }.getOrNull()
                    ?: "The Google app did not accept the overlay window.",
            )
        }
        _state.value = _state.value.copy(windowAttached = attached)
    }

    private fun applyActivityState() {
        call { setActivityState(activityState) }
        _state.value = _state.value.copy(lastActivityState = activityState)
    }

    fun detachWindow(isChangingConfigurations: Boolean) {
        attachedActivity = null
        call { detachWindow(isChangingConfigurations) }
    }

    fun startScroll() = call { startScroll() }

    /** [progress] runs 0f (Discover hidden) to 1f (fully open). */
    fun onScroll(progress: Float) = call { onScroll(progress.coerceIn(0f, 1f)) }

    fun endScroll() = call { endScroll() }

    fun openOverlay() = call { openOverlay(OPTION_ANIMATE) }

    fun closeOverlay() = call { closeOverlay(OPTION_ANIMATE) }

    fun onActivityStarted() {
        activityState = ACTIVITY_STARTED
        applyActivityState()
    }

    fun onActivityResumed() {
        activityState = ACTIVITY_STARTED or ACTIVITY_RESUMED
        applyActivityState()
        call { onLauncherResume() }
        refreshContentFlag()
    }

    fun onActivityPaused() {
        activityState = ACTIVITY_STARTED
        applyActivityState()
        call { onLauncherPause() }
    }

    fun onActivityStopped() {
        activityState = 0
        applyActivityState()
    }

    private fun refreshContentFlag() {
        val bridge = overlay ?: return
        val hasContent = runCatching { bridge.hasOverlayContent() }.getOrDefault(false)
        _state.value = _state.value.copy(hasContent = hasContent)
    }

    private inline fun call(block: INexusOverlay.() -> Unit) {
        val bridge = overlay ?: return
        runCatching { bridge.block() }
            .onFailure { Log.w(TAG, "Overlay bridge call failed", it) }
    }

    companion object {
        private const val TAG = "DiscoverOverlay"

        const val COMPANION_PACKAGE = "com.nexus.companion"
        const val ACTION_OVERLAY_BRIDGE = "com.nexus.companion.OVERLAY_BRIDGE"

        private const val NOT_INSTALLED =
            "Nexus Companion is not installed. It ships as a second APK alongside Nexus."

        /** Enable the overlay; ask for the dark treatment to match the design. */
        private const val CLIENT_OPTIONS = (1 shl 0) or (1 shl 1)

        private const val OPTION_ANIMATE = 1

        private const val ACTIVITY_STARTED = 1 shl 0
        private const val ACTIVITY_RESUMED = 1 shl 1

        private const val STATUS_ATTACHED = 1 shl 0
    }
}
