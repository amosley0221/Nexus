package com.nexus.launcher.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.nexus.launcher.data.IconPackMode
import com.nexus.launcher.domain.AppEntry
import com.nexus.launcher.ui.theme.NexusColor

/**
 * Renders an app icon in the active pack.
 *
 * - [IconPackMode.NexusBlush] draws the design's line glyph on the blush
 *   squircle, matching the mock exactly.
 * - [IconPackMode.SystemIcons] shows the app's own icon, masked to the same
 *   squircle so rows stay visually uniform.
 */
@Composable
fun AppIcon(
    entry: AppEntry,
    size: Dp,
    mode: IconPackMode,
    modifier: Modifier = Modifier,
) {
    when (mode) {
        IconPackMode.SystemIcons, IconPackMode.ThirdParty -> {
            val bitmap = remember(entry.key, size) {
                entry.icon?.let { drawable ->
                    runCatching { drawable.toBitmap(width = 144, height = 144) }.getOrNull()
                }
            }
            if (bitmap != null) {
                Image(
                    painter = BitmapPainter(bitmap.asImageBitmap()),
                    contentDescription = entry.label,
                    contentScale = ContentScale.Crop,
                    modifier = modifier
                        .size(size)
                        .clip(RoundedCornerShape(size * 0.34f)),
                )
            } else {
                BlushIcon(entry.label, size, modifier)
            }
        }

        IconPackMode.NexusBlush -> BlushIcon(entry.label, size, modifier)
    }
}

/**
 * The default "Nexus Blush" pack. Every app gets a 24x24-viewBox line glyph
 * stroked in the deep maroon, picked by keyword where a name is recognisable and
 * otherwise falling back to a lettered tile.
 */
@Composable
fun BlushIcon(label: String, size: Dp, modifier: Modifier = Modifier) {
    val glyph = remember(label) { GlyphSet.forLabel(label) }
    Squircle(size = size, modifier = modifier) {
        if (glyph != null) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .size(size * 0.53f)
                    .padding(0.dp)
            ) {
                val scale = this.size.minDimension / 24f
                val stroke = Stroke(
                    width = 1.8f * scale,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round,
                )
                glyph.draw(this, scale, stroke)
            }
        } else {
            androidx.compose.material3.Text(
                text = label.firstOrNull()?.uppercase() ?: "?",
                color = NexusColor.Glyph,
                style = com.nexus.launcher.ui.theme.NexusType.CardTitle.copy(
                    fontSize = (size.value * 0.42f).sp
                ),
            )
        }
    }
}

/** A single line glyph, drawn from primitives rather than a vector asset. */
fun interface Glyph {
    fun draw(scope: DrawScope, scale: Float, stroke: Stroke)
}

private object GlyphSet {

    private val glyphColor = NexusColor.Glyph

    private fun circle(cx: Float, cy: Float, r: Float): Glyph = Glyph { scope, s, stroke ->
        scope.drawCircle(glyphColor, r * s, androidx.compose.ui.geometry.Offset(cx * s, cy * s), style = stroke)
    }

    private fun roundRect(x: Float, y: Float, w: Float, h: Float, r: Float): Glyph =
        Glyph { scope, s, stroke ->
            scope.drawRoundRect(
                color = glyphColor,
                topLeft = androidx.compose.ui.geometry.Offset(x * s, y * s),
                size = androidx.compose.ui.geometry.Size(w * s, h * s),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * s, r * s),
                style = stroke,
            )
        }

    private fun combine(vararg parts: Glyph): Glyph = Glyph { scope, s, stroke ->
        parts.forEach { it.draw(scope, s, stroke) }
    }

    private fun line(x1: Float, y1: Float, x2: Float, y2: Float): Glyph = Glyph { scope, s, stroke ->
        scope.drawLine(
            glyphColor,
            androidx.compose.ui.geometry.Offset(x1 * s, y1 * s),
            androidx.compose.ui.geometry.Offset(x2 * s, y2 * s),
            strokeWidth = stroke.width,
            cap = stroke.cap,
        )
    }

    // Glyphs transcribed from the SVGs in the design document.
    private val music = combine(
        circle(7f, 17.5f, 2.6f),
        circle(17.4f, 15.4f, 2.6f),
        line(9.6f, 17.5f, 9.6f, 6.6f),
        line(9.6f, 6.6f, 20f, 4.4f),
        line(20f, 4.4f, 20f, 15.4f),
    )
    private val calendar = combine(
        roundRect(3.5f, 5f, 17f, 15.5f, 3f),
        line(3.5f, 10f, 20.5f, 10f),
        line(8.5f, 3f, 8.5f, 7f),
        line(15.5f, 3f, 15.5f, 7f),
    )
    private val camera = combine(
        roundRect(3.5f, 7.5f, 17f, 12.5f, 3f),
        circle(12f, 13.5f, 3.4f),
        line(9f, 7.5f, 10.4f, 5f),
        line(13.6f, 5f, 15f, 7.5f),
    )
    private val browser = combine(
        circle(12f, 12f, 8.5f),
        line(3.5f, 12f, 20.5f, 12f),
        Glyph { scope, s, stroke ->
            scope.drawOval(
                glyphColor,
                androidx.compose.ui.geometry.Offset(8f * s, 3.5f * s),
                androidx.compose.ui.geometry.Size(8f * s, 17f * s),
                style = stroke,
            )
        },
    )
    private val chat = combine(
        roundRect(3f, 5f, 18f, 13f, 4f),
        line(8f, 20f, 8f, 17.5f),
    )
    private val folder = combine(
        roundRect(3.5f, 6.5f, 17f, 13f, 2.5f),
        line(3.5f, 10f, 20.5f, 10f),
    )
    private val mail = combine(
        roundRect(3f, 5.5f, 18f, 13f, 2.5f),
        line(3f, 7f, 12f, 13.5f),
        line(12f, 13.5f, 21f, 7f),
    )
    private val phone = combine(
        roundRect(6.5f, 2.5f, 11f, 19f, 3f),
        line(10.5f, 18.5f, 13.5f, 18.5f),
    )
    private val settings = combine(
        circle(12f, 12f, 3.2f),
        circle(12f, 12f, 8.4f),
    )
    private val play = combine(
        circle(12f, 12f, 8.5f),
        line(10f, 8.5f, 16f, 12f),
        line(16f, 12f, 10f, 15.5f),
        line(10f, 8.5f, 10f, 15.5f),
    )
    private val gamepad = combine(
        roundRect(3f, 8f, 18f, 9.5f, 4.7f),
        line(8f, 11f, 8f, 14.5f),
        line(6.25f, 12.75f, 9.75f, 12.75f),
        circle(15.5f, 11.5f, 1f),
        circle(17.8f, 14f, 1f),
    )
    private val store = combine(
        roundRect(4f, 7f, 16f, 13.5f, 3f),
        line(8.5f, 7f, 8.5f, 5f),
        line(15.5f, 7f, 15.5f, 5f),
    )
    private val map = combine(
        line(4f, 6f, 9.5f, 4f),
        line(9.5f, 4f, 14.5f, 6.5f),
        line(14.5f, 6.5f, 20f, 4.5f),
        line(20f, 4.5f, 20f, 18f),
        line(20f, 18f, 14.5f, 20f),
        line(14.5f, 20f, 9.5f, 17.5f),
        line(9.5f, 17.5f, 4f, 19.5f),
        line(4f, 19.5f, 4f, 6f),
    )
    private val clock = combine(
        circle(12f, 12f, 8.5f),
        line(12f, 7f, 12f, 12f),
        line(12f, 12f, 15.5f, 14f),
    )
    private val bank = combine(
        line(4f, 10f, 4f, 17f),
        line(9.3f, 10f, 9.3f, 17f),
        line(14.7f, 10f, 14.7f, 17f),
        line(20f, 10f, 20f, 17f),
        line(3f, 19.5f, 21f, 19.5f),
        line(3f, 8f, 12f, 4f),
        line(12f, 4f, 21f, 8f),
    )
    private val video = combine(
        roundRect(3f, 6f, 13f, 12f, 3f),
        line(16f, 11f, 21f, 8f),
        line(21f, 8f, 21f, 16f),
        line(21f, 16f, 16f, 13f),
    )
    private val book = combine(
        line(4f, 5f, 4f, 19f),
        line(4f, 5f, 12f, 6.5f),
        line(12f, 6.5f, 20f, 5f),
        line(20f, 5f, 20f, 19f),
        line(20f, 19f, 12f, 20.5f),
        line(12f, 20.5f, 4f, 19f),
        line(12f, 6.5f, 12f, 20.5f),
    )

    private val byKeyword: List<Pair<List<String>, Glyph>> = listOf(
        listOf("music", "spotify", "audio", "sound", "podcast", "tidal") to music,
        listOf("calendar", "agenda", "cal") to calendar,
        listOf("camera", "photo", "cam") to camera,
        listOf("chrome", "browser", "firefox", "edge", "opera", "internet", "web") to browser,
        listOf("discord", "message", "chat", "sms", "whatsapp", "telegram", "signal", "slack") to chat,
        listOf("file", "drive", "folder", "storage", "dropbox") to folder,
        listOf("gallery", "photos", "pictures") to camera,
        listOf("mail", "gmail", "inbox", "outlook") to mail,
        listOf("phone", "dial", "call", "contacts") to phone,
        listOf("setting", "config", "tools") to settings,
        listOf("youtube", "netflix", "video", "plex", "twitch", "disney", "prime", "player") to play,
        listOf("game", "play store", "steam", "emulator", "retro") to gamepad,
        listOf("store", "shop", "market", "amazon") to store,
        listOf("map", "navigation", "waze", "gps") to map,
        listOf("clock", "alarm", "timer", "watch") to clock,
        listOf("bank", "budget", "pay", "wallet", "money", "finance", "revolut") to bank,
        listOf("meet", "zoom", "teams", "record") to video,
        listOf("book", "read", "kindle", "note", "docs") to book,
    )

    fun forLabel(label: String): Glyph? {
        val lower = label.lowercase()
        return byKeyword.firstOrNull { (keys, _) -> keys.any { it in lower } }?.second
    }
}

/** A file-type tile as used on the Files hub. */
@Composable
fun FileTypeTile(extension: String, size: Dp = 36.dp, modifier: Modifier = Modifier) {
    val (bg, fg) = when (extension.uppercase()) {
        "PDF" -> NexusColor.PdfBg to NexusColor.PdfFg
        "XLS", "XLSX", "CSV" -> NexusColor.XlsBg to NexusColor.XlsFg
        "DOC", "DOCX", "TXT", "MD" -> NexusColor.DocBg to NexusColor.DocFg
        "PPT", "PPTX" -> NexusColor.PptBg to NexusColor.PptFg
        else -> NexusColor.Card to NexusColor.TextSecondary
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Text(
            text = extension.uppercase().take(4),
            style = com.nexus.launcher.ui.theme.NexusType.Badge.copy(fontSize = 10.sp),
            color = fg,
        )
    }
}
