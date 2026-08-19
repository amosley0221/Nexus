package com.nexus.launcher.ui.common

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.nexus.launcher.ui.theme.NexusColor

/**
 * The wallpaper the hub pages blur behind their content, provided once at the
 * root so each page does not re-read it.
 */
val LocalWallpaperBitmap: ProvidableCompositionLocal<ImageBitmap?> =
    compositionLocalOf { null }

/**
 * The surface every non-Home page sits on: the wallpaper, blurred and darkened
 * until it reads as a solid hub background with only a hint of the picture left.
 *
 * Painted across the whole window, system-bar areas included — callers apply
 * insets to their *content*, never to this. Falls back to flat
 * [NexusColor.HubBackground] where the wallpaper is unavailable or a render-time
 * blur would be too costly, but never to transparent: an unpainted band would
 * show the sharp Home wallpaper through the page.
 */
@Composable
fun HubBackground(
    wallpaper: ImageBitmap?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize().background(NexusColor.HubBackground)) {
        // RenderEffect-backed Modifier.blur is a no-op below API 31, so on older
        // devices the flat background above is the whole treatment.
        if (wallpaper != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Image(
                painter = BitmapPainter(wallpaper),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(BLUR_RADIUS),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NexusColor.HubBackground.copy(alpha = SCRIM_ALPHA))
            )
        }

        content()
    }
}

private val BLUR_RADIUS = 40.dp
private const val SCRIM_ALPHA = 0.78f
