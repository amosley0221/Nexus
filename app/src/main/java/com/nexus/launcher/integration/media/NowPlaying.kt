package com.nexus.launcher.integration.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.view.KeyEvent
import androidx.compose.runtime.Immutable
import com.nexus.launcher.integration.notifications.NexusNotificationListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Immutable
data class NowPlaying(
    val title: String,
    val artist: String,
    val album: String,
    val artwork: Bitmap?,
    val isPlaying: Boolean,
    val positionMillis: Long,
    val durationMillis: Long,
    val appPackage: String,
) {
    val progress: Float
        get() = if (durationMillis <= 0) 0f else (positionMillis.toFloat() / durationMillis).coerceIn(0f, 1f)
}

/**
 * Reads the active MediaSession for the Music hub's now-playing card. Uses the
 * notification-listener component as the caller identity, which is how a
 * non-system app is allowed to see other apps' sessions.
 */
class NowPlayingController(private val context: Context) {

    private val _state = MutableStateFlow<NowPlaying?>(null)
    val state: StateFlow<NowPlaying?> = _state.asStateFlow()

    private val manager =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager

    private val listenerComponent =
        ComponentName(context, NexusNotificationListener::class.java)

    private var controller: MediaController? = null
    private var callback: MediaController.Callback? = null

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        bind(controllers.orEmpty())
    }

    fun start() {
        val mgr = manager ?: return
        val existing = runCatching { mgr.getActiveSessions(listenerComponent) }.getOrNull()
        if (existing != null) {
            bind(existing)
            runCatching { mgr.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent) }
        }
    }

    fun stop() {
        manager?.let { runCatching { it.removeOnActiveSessionsChangedListener(sessionsListener) } }
        detach()
    }

    private fun bind(controllers: List<MediaController>) {
        // Prefer whatever is actually playing; otherwise keep the first session
        // so the card shows the last thing that was open.
        val next = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers.firstOrNull()

        if (next?.sessionToken == controller?.sessionToken) {
            publish()
            return
        }
        detach()
        controller = next ?: return

        val cb = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
            override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
            override fun onSessionDestroyed() {
                detach()
                _state.value = null
            }
        }
        callback = cb
        runCatching { next.registerCallback(cb) }
        publish()
    }

    private fun detach() {
        val c = controller
        val cb = callback
        if (c != null && cb != null) runCatching { c.unregisterCallback(cb) }
        controller = null
        callback = null
    }

    private fun publish() {
        val c = controller
        if (c == null) {
            _state.value = null
            return
        }
        val md = c.metadata
        val ps = c.playbackState
        val title = md?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        if (title.isBlank() && md == null) {
            _state.value = null
            return
        }
        _state.value = NowPlaying(
            title = title.ifBlank { "Nothing playing" },
            artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty(),
            album = md?.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty(),
            artwork = md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART),
            isPlaying = ps?.state == PlaybackState.STATE_PLAYING,
            positionMillis = ps?.position ?: 0L,
            durationMillis = md?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
            appPackage = c.packageName,
        )
    }

    fun playPause() = sendKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    fun next() = sendKey(KeyEvent.KEYCODE_MEDIA_NEXT)
    fun previous() = sendKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)

    private fun sendKey(code: Int) {
        val c = controller ?: return
        runCatching {
            c.dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
            c.dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_UP, code))
        }
    }
}
