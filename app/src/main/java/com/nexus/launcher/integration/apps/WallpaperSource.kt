package com.nexus.launcher.integration.apps

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Supplies the system wallpaper for the hub pages to blur behind their content.
 *
 * The bitmap is downscaled hard on the way in: it is only ever seen through a
 * 40dp blur, so full resolution would cost memory for detail that is thrown away.
 */
class WallpaperSource(private val context: Context) {

    private val _wallpaper = MutableStateFlow<Bitmap?>(null)
    val wallpaper: StateFlow<Bitmap?> = _wallpaper.asStateFlow()

    /**
     * Reads the current wallpaper. Silently leaves the flow null when the read
     * is refused — some devices deny it for live or secured wallpapers — and the
     * hub pages fall back to their flat background.
     */
    suspend fun refresh() = withContext(Dispatchers.IO) {
        if (_wallpaper.value != null) return@withContext

        val manager = runCatching { WallpaperManager.getInstance(context) }.getOrNull()
            ?: return@withContext

        val bitmap = runCatching {
            val drawable = manager.drawable ?: return@runCatching null
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                drawable.bitmap.downscaled()
            } else {
                drawable.toBitmap(
                    width = drawable.intrinsicWidth.coerceAtLeast(1),
                    height = drawable.intrinsicHeight.coerceAtLeast(1),
                ).downscaled()
            }
        }.getOrNull()

        _wallpaper.value = bitmap
    }

    /** Drops the cached bitmap so the next refresh re-reads the wallpaper. */
    fun invalidate() {
        _wallpaper.value = null
    }

    private fun Bitmap.downscaled(): Bitmap {
        val longest = maxOf(width, height)
        if (longest <= TARGET_LONGEST_PX) return this
        val scale = TARGET_LONGEST_PX.toFloat() / longest
        return Bitmap.createScaledBitmap(
            this,
            (width * scale).toInt().coerceAtLeast(1),
            (height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }

    private companion object {
        /** Plenty of detail to survive a heavy blur, a fraction of the memory. */
        const val TARGET_LONGEST_PX = 512
    }
}
