package com.nexus.launcher.ui.layout

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import kotlinx.coroutines.flow.map

/**
 * Observes real window metrics and hinge state. Configuration drives width and
 * height (so rotation and multi-window are handled for free); WindowInfoTracker
 * supplies the fold posture and hinge position.
 */
@Composable
fun rememberWindowProfile(activity: Activity): State<WindowProfile> {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val widthDp = configuration.screenWidthDp.dp
    val heightDp = configuration.screenHeightDp.dp

    val foldFlow = remember(activity) {
        WindowInfoTracker.getOrCreate(activity)
            .windowLayoutInfo(activity)
            .map { info -> info.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull() }
    }
    val fold = foldFlow.collectAsState(initial = null).value

    val posture = when {
        fold == null -> if (widthDp >= 600.dp) FoldPosture.Unfolded else FoldPosture.Folded
        fold.state == FoldingFeature.State.HALF_OPENED -> FoldPosture.HalfOpen
        else -> FoldPosture.Unfolded
    }

    val hingeDp = fold?.bounds?.let { with(density) { it.centerX().toDp() } }
    val hingeVertical = fold?.orientation == FoldingFeature.Orientation.VERTICAL

    return remember(widthDp, heightDp, posture, hingeDp, hingeVertical) {
        androidx.compose.runtime.mutableStateOf(
            WindowProfile(
                widthDp = widthDp,
                heightDp = heightDp,
                posture = posture,
                hingePositionDp = hingeDp,
                isHingeVertical = hingeVertical,
            )
        )
    }
}
