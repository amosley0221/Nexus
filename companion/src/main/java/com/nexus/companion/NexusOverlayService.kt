package com.nexus.companion

import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Binder
import android.os.Process
import android.os.IBinder
import android.os.RemoteCallbackList
import android.util.Log
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The launcher-facing half of the bridge. Nexus binds this, and every call is
 * relayed to the Google app through [GoogleOverlayClient]; callbacks travel back
 * the same way.
 *
 * Access is gated twice over: the service declares a signature-level permission
 * in the manifest, and every incoming call re-checks that the caller really is
 * the Nexus launcher package. A companion that hands the overlay window token to
 * an arbitrary caller would be handing over the launcher's window.
 */
class NexusOverlayService : Service() {

    private lateinit var client: GoogleOverlayClient
    private val callbacks = RemoteCallbackList<INexusOverlayCallback>()

    override fun onCreate() {
        super.onCreate()
        client = GoogleOverlayClient(applicationContext, clientListener)
        client.connect()
        state.value = currentState()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        client.disconnect()
        callbacks.kill()
        super.onDestroy()
        state.value = ConnectionState(false, "Companion service stopped.")
    }

    private val clientListener = object : GoogleOverlayClient.Listener {
        override fun onScrollChanged(progress: Float) = broadcast { overlayScrollChanged(progress) }

        override fun onStatusChanged(status: Int) = broadcast { overlayStatusChanged(status) }

        override fun onConnectionChanged(connected: Boolean, detail: String?) {
            state.value = ConnectionState(connected, detail)
            broadcast { companionStateChanged(connected, detail) }
        }
    }

    private inline fun broadcast(block: INexusOverlayCallback.() -> Unit) {
        val count = callbacks.beginBroadcast()
        try {
            repeat(count) { index ->
                runCatching { callbacks.getBroadcastItem(index).block() }
            }
        } finally {
            callbacks.finishBroadcast()
        }
    }

    private fun currentState() = ConnectionState(client.isConnected, client.unavailableReason)

    /**
     * Rejects any caller that is not the Nexus launcher signed with this
     * companion's own key.
     *
     * Both halves matter. The package check alone would let anyone who
     * side-loads an app claiming that package name through; the signature check
     * alone would let any other app of ours through. Attaching the overlay hands
     * over the launcher's window token, so neither is worth being loose about.
     */
    private fun requireLauncherCaller() {
        val uid = Binder.getCallingUid()
        if (uid == Process.myUid()) return

        val callers = packageManager.getPackagesForUid(uid).orEmpty()
        if (LAUNCHER_PACKAGE !in callers) {
            Log.w(TAG, "Rejected overlay call from uid $uid (${callers.joinToString()})")
            throw SecurityException("Only $LAUNCHER_PACKAGE may use the Nexus overlay bridge")
        }

        @Suppress("DEPRECATION")
        val signatureMatch = packageManager.checkSignatures(uid, Process.myUid())
        if (signatureMatch != PackageManager.SIGNATURE_MATCH) {
            Log.w(TAG, "Rejected overlay call: caller signature does not match (uid $uid)")
            throw SecurityException(
                "$LAUNCHER_PACKAGE must be signed with the same key as the companion"
            )
        }
    }

    private val binder = object : INexusOverlay.Stub() {

        override fun attachWindow(
            attrs: WindowManager.LayoutParams?,
            configuration: Configuration?,
            clientOptions: Int,
            callback: INexusOverlayCallback?,
        ): Boolean {
            requireLauncherCaller()
            if (attrs == null || configuration == null) return false

            if (callback != null) callbacks.register(callback)
            if (!client.isConnected) client.connect()

            return client.attachWindow(attrs, configuration, clientOptions)
        }

        override fun detachWindow(isChangingConfigurations: Boolean) {
            requireLauncherCaller()
            client.detachWindow(isChangingConfigurations)
        }

        override fun startScroll() {
            requireLauncherCaller()
            client.startScroll()
        }

        override fun onScroll(progress: Float) {
            requireLauncherCaller()
            client.onScroll(progress)
        }

        override fun endScroll() {
            requireLauncherCaller()
            client.endScroll()
        }

        override fun openOverlay(options: Int) {
            requireLauncherCaller()
            client.openOverlay(options)
        }

        override fun closeOverlay(options: Int) {
            requireLauncherCaller()
            client.closeOverlay(options)
        }

        override fun setActivityState(flags: Int) {
            requireLauncherCaller()
            client.setActivityState(flags)
        }

        override fun onLauncherPause() {
            requireLauncherCaller()
            client.onLauncherPause()
        }

        override fun onLauncherResume() {
            requireLauncherCaller()
            client.onLauncherResume()
        }

        override fun hasOverlayContent(): Boolean {
            requireLauncherCaller()
            return client.hasOverlayContent()
        }

        override fun isConnected(): Boolean {
            requireLauncherCaller()
            return client.isConnected
        }

        override fun getUnavailableReason(): String? {
            requireLauncherCaller()
            return client.unavailableReason
        }
    }

    data class ConnectionState(val connected: Boolean, val detail: String?)

    companion object {
        private const val TAG = "NexusOverlayService"

        const val ACTION_OVERLAY_BRIDGE = "com.nexus.companion.OVERLAY_BRIDGE"
        const val LAUNCHER_PACKAGE = "com.nexus.launcher"

        /** Observed by [CompanionActivity] to show live status. */
        private val state = MutableStateFlow(ConnectionState(false, "Starting up…"))
        val connectionState: StateFlow<ConnectionState> = state.asStateFlow()
    }
}
