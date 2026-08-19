package com.nexus.companion

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.Process
import android.os.RemoteException
import android.util.Log
import android.view.WindowManager
import com.google.android.libraries.launcherclient.ILauncherOverlay
import com.google.android.libraries.launcherclient.ILauncherOverlayCallback

/**
 * Owns the binding to the Google app's launcher-overlay service — the thing that
 * renders the Discover feed to the left of a home screen.
 *
 * Modelled on the LauncherClient that Lawnchair's Lawnfeed and the other
 * open-source companions use: the same service action, the same `app://` data
 * URI carrying the caller identity and protocol versions, and the same
 * attach/scroll/detach call sequence.
 *
 * The reason this lives in a separate APK at all is that the Google app decides
 * whether to serve an overlay from the *calling package*. A launcher that binds
 * the service directly is refused, so the bind has to originate here and the
 * result is relayed back over [NexusOverlayService].
 */
class GoogleOverlayClient(
    private val context: Context,
    private val listener: Listener,
) {

    interface Listener {
        fun onScrollChanged(progress: Float)
        fun onStatusChanged(status: Int)
        fun onConnectionChanged(connected: Boolean, detail: String?)
    }

    @Volatile
    var overlay: ILauncherOverlay? = null
        private set

    @Volatile
    var unavailableReason: String? = "Not connected to the Google app yet."
        private set

    val isConnected: Boolean get() = overlay != null

    /** Set once a window has been attached, so a reconnect can re-attach it. */
    private var pendingAttach: AttachRequest? = null

    private data class AttachRequest(
        val attrs: WindowManager.LayoutParams,
        val configuration: Configuration,
        val clientOptions: Int,
    )

    private val callback = object : ILauncherOverlayCallback.Stub() {
        override fun overlayScrollChanged(progress: Float) = listener.onScrollChanged(progress)
        override fun overlayStatusChanged(status: Int) = listener.onStatusChanged(status)
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            overlay = ILauncherOverlay.Stub.asInterface(service)
            unavailableReason = null
            Log.i(TAG, "Bound to the Google app overlay service")
            listener.onConnectionChanged(true, null)

            // A reconnect after a Google app update or a process kill has to
            // re-attach the window the launcher gave us, or the feed stays blank.
            pendingAttach?.let { request ->
                attachWindow(request.attrs, request.configuration, request.clientOptions)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            overlay = null
            unavailableReason = "The Google app disconnected the overlay service."
            Log.w(TAG, "Overlay service disconnected")
            listener.onConnectionChanged(false, unavailableReason)
        }

        override fun onBindingDied(name: ComponentName?) {
            overlay = null
            unavailableReason = "The Google app dropped the overlay binding."
            listener.onConnectionChanged(false, unavailableReason)
            // Rebinding immediately after a binding death loops; the launcher
            // retries on its next resume instead.
            runCatching { context.unbindService(this) }
        }

        override fun onNullBinding(name: ComponentName?) {
            overlay = null
            unavailableReason = OVERLAY_REFUSED
            Log.w(TAG, "Google app returned a null binding — overlay refused for this caller")
            listener.onConnectionChanged(false, unavailableReason)
        }
    }

    private var bound = false

    /** Binds the overlay service. Returns false with a reason set on failure. */
    fun connect(): Boolean {
        if (bound) return true

        val googleVersion = googleAppVersion()
        if (googleVersion == null) {
            unavailableReason = "The Google app is not installed."
            listener.onConnectionChanged(false, unavailableReason)
            return false
        }

        val intent = buildBindIntent()
        if (context.packageManager.resolveService(intent, 0) == null) {
            unavailableReason =
                "This Google app build does not expose the launcher overlay service."
            listener.onConnectionChanged(false, unavailableReason)
            return false
        }

        bound = runCatching {
            context.bindService(
                intent,
                connection,
                Context.BIND_AUTO_CREATE or Context.BIND_ADJUST_WITH_ACTIVITY,
            )
        }.getOrDefault(false)

        if (!bound) {
            unavailableReason = OVERLAY_REFUSED
            listener.onConnectionChanged(false, unavailableReason)
        }
        return bound
    }

    fun disconnect() {
        if (!bound) return
        bound = false
        overlay = null
        pendingAttach = null
        runCatching { context.unbindService(connection) }
        listener.onConnectionChanged(false, "Disconnected.")
    }

    /**
     * The Google app keys the overlay off this URI: the authority identifies the
     * calling app and uid, and the query carries the protocol versions the
     * client speaks.
     */
    private fun buildBindIntent(): Intent {
        val data = Uri.parse("app://${context.packageName}:${Process.myUid()}")
            .buildUpon()
            .appendQueryParameter("v", SERVER_VERSION.toString())
            .appendQueryParameter("cv", CLIENT_VERSION.toString())
            .build()

        return Intent(ACTION_WINDOW_OVERLAY)
            .setPackage(GOOGLE_APP_PACKAGE)
            .setData(data)
    }

    private fun googleAppVersion(): Long? = runCatching {
        val info = context.packageManager.getPackageInfo(GOOGLE_APP_PACKAGE, 0)
        info.longVersionCode
    }.getOrNull()

    /**
     * Hands the launcher's window to the overlay.
     *
     * Newer Google app builds take the bundle form, older ones the flat form.
     * Rather than guess from a version code — which has moved around across
     * Google app releases — this tries the current call and falls back when the
     * transaction is rejected.
     */
    fun attachWindow(
        attrs: WindowManager.LayoutParams,
        configuration: Configuration,
        clientOptions: Int,
    ): Boolean {
        val service = overlay ?: run {
            pendingAttach = AttachRequest(attrs, configuration, clientOptions)
            return false
        }
        pendingAttach = AttachRequest(attrs, configuration, clientOptions)

        val bundle = Bundle().apply {
            putParcelable(KEY_LAYOUT_PARAMS, attrs)
            putParcelable(KEY_CONFIGURATION, configuration)
            putInt(KEY_CLIENT_OPTIONS, clientOptions)
        }

        if (runCatching { service.windowAttached2(bundle, callback) }.isSuccess) return true

        return runCatching {
            service.windowAttached(attrs, callback, clientOptions)
            true
        }.getOrElse { error ->
            Log.w(TAG, "windowAttached failed", error)
            unavailableReason = "The Google app refused the overlay window."
            false
        }
    }

    fun detachWindow(isChangingConfigurations: Boolean) {
        pendingAttach = null
        call { windowDetached(isChangingConfigurations) }
    }

    fun startScroll() = call { startScroll() }
    fun onScroll(progress: Float) = call { onScroll(progress) }
    fun endScroll() = call { endScroll() }
    fun openOverlay(options: Int) = call { openOverlay(options) }
    fun closeOverlay(options: Int) = call { closeOverlay(options) }
    fun setActivityState(flags: Int) = call { setActivityState(flags) }
    fun onLauncherPause() = call { onPause() }
    fun onLauncherResume() = call { onResume() }

    fun hasOverlayContent(): Boolean =
        runCatching { overlay?.hasOverlayContent() ?: false }.getOrDefault(false)

    private inline fun call(block: ILauncherOverlay.() -> Unit) {
        val service = overlay ?: return
        try {
            service.block()
        } catch (e: RemoteException) {
            Log.w(TAG, "Overlay call failed", e)
        }
    }

    companion object {
        private const val TAG = "GoogleOverlayClient"

        const val GOOGLE_APP_PACKAGE = "com.google.android.googlequicksearchbox"
        const val ACTION_WINDOW_OVERLAY = "com.android.launcher3.WINDOW_OVERLAY"

        /** Overlay protocol versions this client speaks, as the URI query. */
        private const val SERVER_VERSION = 1
        private const val CLIENT_VERSION = 9

        private const val KEY_LAYOUT_PARAMS = "layout_params"
        private const val KEY_CONFIGURATION = "configuration"
        private const val KEY_CLIENT_OPTIONS = "client_options"

        const val OVERLAY_REFUSED =
            "The Google app would not serve the overlay to this companion. " +
                "Google only grants the Discover feed to companion builds it " +
                "recognises, which a self-signed build is not."

        /** Client option bits sent with the attach call. */
        const val OPTION_OVERLAY_ENABLED = 1 shl 0
        const val OPTION_PREFERS_DARK = 1 shl 1

        /** Activity-state bits for [setActivityState]. */
        const val ACTIVITY_STARTED = 1 shl 0
        const val ACTIVITY_RESUMED = 1 shl 1

        /** Overlay status bit: set when the overlay is attached with content. */
        const val STATUS_ATTACHED = 1 shl 0
    }
}
