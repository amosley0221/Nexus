package com.nexus.launcher.ui.common

import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.nexus.launcher.domain.AppEntry

/**
 * Process-wide cache of rasterised app icons.
 *
 * Turning a Drawable into a bitmap is not free, and doing it inside composition
 * meant paying that cost for every row the first time it scrolled into view —
 * one of the reasons the list stuttered. Rasterising once and keeping the result
 * means a row that scrolls back into view costs a map lookup.
 */
object IconCache {

    /** Icons are small; a few hundred entries is well under a megabyte. */
    private val cache = LruCache<String, ImageBitmap>(512)

    fun get(entry: AppEntry, sizePx: Int): ImageBitmap? {
        val key = "${entry.key}@$sizePx"
        cache.get(key)?.let { return it }

        val drawable = entry.icon ?: return null
        val bitmap = runCatching {
            drawable.toBitmap(width = sizePx, height = sizePx).asImageBitmap()
        }.getOrNull() ?: return null

        cache.put(key, bitmap)
        return bitmap
    }

    /** Dropped when the app list is rebuilt, so a changed icon is picked up. */
    fun clear() = cache.evictAll()
}
