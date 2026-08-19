package com.nexus.launcher.ui.layout

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class WidthClass { Compact, Medium, Expanded }
enum class HeightClass { Compact, Medium, Expanded }

/** Physical fold state, as reported by Jetpack WindowManager. */
enum class FoldPosture {
    /** Cover screen, or an unfolded device held flat with no hinge reported. */
    Folded,

    /** Inner screen, fully flat. */
    Unfolded,

    /** Half-open (tabletop / book). Treated as unfolded but hinge-aware. */
    HalfOpen,
}

/**
 * Everything layout code needs to reflow, derived at runtime. Deliberately
 * contains no device model checks and no hardcoded pixel sizes — Fold 7 and
 * Fold 8 (and anything else) fall out of the same measurements.
 */
@Immutable
data class WindowProfile(
    val widthDp: Dp,
    val heightDp: Dp,
    val posture: FoldPosture,
    /** Hinge midpoint measured from the window's left edge, when one is reported. */
    val hingePositionDp: Dp? = null,
    val isHingeVertical: Boolean = true,
) {
    val widthClass: WidthClass = when {
        widthDp < 600.dp -> WidthClass.Compact
        widthDp < 840.dp -> WidthClass.Medium
        else -> WidthClass.Expanded
    }

    val heightClass: HeightClass = when {
        heightDp < 480.dp -> HeightClass.Compact
        heightDp < 900.dp -> HeightClass.Medium
        else -> HeightClass.Expanded
    }

    val isLandscape: Boolean = widthDp > heightDp

    /** Near-square inner screens (Fold 7 ≈ 1.11, Fold 8 similar) sit around 1.0. */
    val aspectRatio: Float =
        max(widthDp.value, heightDp.value) / max(1f, min(widthDp.value, heightDp.value))

    /** True on the big inner display, in any rotation. */
    val isLargeScreen: Boolean = widthClass != WidthClass.Compact

    /** Home shows a two-column favourites grid once there is room for it. */
    val homeColumns: Int = when {
        isLargeScreen -> 2
        isLandscape -> 1 // folded landscape keeps one list, clock moves to the left
        else -> 1
    }

    /** Folded landscape pins the clock block to the left of the scrolling list. */
    val useSideClock: Boolean = isLandscape && !isLargeScreen

    /** Hub grids: 2 columns folded, 3 when the inner screen is open. */
    val hubGridColumns: Int = if (isLargeScreen) 3 else 2

    /** Hub pages split into two panes when there is real horizontal room. */
    val useTwoPaneHub: Boolean = widthClass == WidthClass.Expanded ||
        (widthClass == WidthClass.Medium && isLandscape)

    /**
     * Widget grid, computed from available space rather than device: the handoff
     * calls for a 4x2-class grid folded and 6x3-class unfolded, which is what
     * these cell sizes produce at the corresponding widths.
     */
    val widgetGridColumns: Int = (widthDp.value / WIDGET_CELL_DP).roundToInt().coerceIn(4, 8)
    val widgetGridRows: Int = (heightDp.value / WIDGET_CELL_DP).roundToInt().coerceIn(2, 8)

    /** Horizontal page padding scales with the window instead of being fixed. */
    val pagePadding: Dp = when (widthClass) {
        WidthClass.Compact -> 22.dp
        WidthClass.Medium -> 28.dp
        WidthClass.Expanded -> 36.dp
    }

    val homePadding: Dp = when (widthClass) {
        WidthClass.Compact -> 26.dp
        WidthClass.Medium -> 34.dp
        WidthClass.Expanded -> 44.dp
    }

    /**
     * Clock size shrinks on short windows (folded landscape, cover screen) so the
     * favourites list keeps its rows. The design's 78sp is the compact-portrait case.
     */
    val clockScale: Float = when {
        heightClass == HeightClass.Compact -> 0.62f
        isLargeScreen && !isLandscape -> 1.15f
        isLargeScreen -> 1.0f
        else -> 1.0f
    }

    companion object {
        private const val WIDGET_CELL_DP = 88f

        val Default = WindowProfile(
            widthDp = 376.dp,
            heightDp = 602.dp,
            posture = FoldPosture.Folded,
        )
    }
}
