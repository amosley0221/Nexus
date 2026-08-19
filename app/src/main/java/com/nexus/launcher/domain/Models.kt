package com.nexus.launcher.domain

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/** A launchable activity discovered through LauncherApps. */
@Immutable
data class AppEntry(
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: Drawable?,
    val isGame: Boolean = false,
    val isSystem: Boolean = false,
    /** Foreground time in ms over the trailing window, when UsageStats is granted. */
    val usageMillis: Long = 0L,
) {
    val key: String get() = "$packageName/$activityName"

    /** Section letter for the A–Z index; anything non-alphabetic buckets under '#'. */
    val sortLetter: Char
        get() = label.firstOrNull { it.isLetterOrDigit() }
            ?.uppercaseChar()
            ?.takeIf { it in 'A'..'Z' }
            ?: '#'
}

/**
 * A shortcut an app publishes about itself — a Twitch channel that is live, a
 * conversation in Messages, a recent destination in Maps. Apps declare these
 * statically in their manifest or push them dynamically at runtime.
 */
@Immutable
data class AppShortcut(
    val id: String,
    val packageName: String,
    val label: String,
    val icon: Drawable?,
)

/** The pages the pager can show, in their default left-to-right order. */
@Serializable
enum class PageKind {
    Discover, Home, Games, Media, Music, Budget, Files, Blank;

    val defaultTitle: String
        get() = when (this) {
            Discover -> "Discover"
            Home -> "Home"
            Games -> "Games"
            Media -> "Media"
            Music -> "Music"
            Budget -> "Budget"
            Files -> "Files"
            Blank -> "Blank"
        }

    val defaultDescription: String
        get() = when (this) {
            Discover -> "Google feed · companion app"
            Home -> "Clock, favourites, A–Z"
            Games -> "Installed · emulators · cloud"
            Media -> "Plex · local · streaming · live"
            Music -> "Apple Music · now playing"
            Budget -> "Spending ring · transactions"
            Files -> "Work · school · downloads"
            Blank -> "Free widget canvas"
        }
}

/** A page as configured by the user: order, visibility, and widget layout. */
@Serializable
data class PageConfig(
    val id: String,
    val kind: PageKind,
    val title: String = kind.defaultTitle,
    val description: String = kind.defaultDescription,
    val visible: Boolean = true,
    val accentColor: Long = 0xFFF6B1A4,
    val widgets: List<WidgetConfig> = emptyList(),
)

@Serializable
data class WidgetConfig(
    val id: String,
    val type: String,
    val size: String = "M",
    val enabled: Boolean = true,
    val gridX: Int = 0,
    val gridY: Int = 0,
    val spanX: Int = 2,
    val spanY: Int = 1,
    /** Set when this slot hosts a system AppWidget (KWGT and friends). */
    val appWidgetId: Int = -1,
)

@Serializable
data class BudgetCategory(
    val name: String,
    val spentCents: Long,
    val limitCents: Long,
)

@Serializable
data class Transaction(
    val id: String,
    val name: String,
    val category: String,
    val amountCents: Long,
    val epochMillis: Long,
)

@Serializable
data class BudgetState(
    val totalLimitCents: Long = 250_000,
    val periodEndEpochDay: Long = 0,
    val categories: List<BudgetCategory> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
) {
    val spentCents: Long get() = transactions.filter { it.amountCents < 0 }.sumOf { -it.amountCents }
    val remainingCents: Long get() = (totalLimitCents - spentCents).coerceAtLeast(0)
    val fractionUsed: Float
        get() = if (totalLimitCents <= 0) 0f else (spentCents.toFloat() / totalLimitCents).coerceIn(0f, 1f)
}

/** A ROM discovered by scanning a SAF folder grant. */
@Serializable
data class RomEntry(
    val uri: String,
    val displayName: String,
    val system: String,
    val sizeBytes: Long = 0,
    val artUri: String? = null,
    val preferredEmulator: String? = null,
)

@Serializable
data class ClaudeTask(
    val id: String,
    val title: String,
    val status: ClaudeStatus,
    val detail: String = "",
    val progress: Float = 0f,
    val steps: List<ClaudeStep> = emptyList(),
    val device: String = "",
    val question: String? = null,
)

@Serializable
enum class ClaudeStatus { Running, NeedsYou, Done }

@Serializable
data class ClaudeStep(val label: String, val done: Boolean)

/** A live notification surfaced by the pull-down overscroll cards. */
@Immutable
data class NotificationCard(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val whenMillis: Long,
    val icon: Drawable?,
)
