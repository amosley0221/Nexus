package com.nexus.launcher.ui.common

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Dashed outline for the design's "add" affordances — the ROM scan cell, the
 * add-widget slot, and the blank-page drop targets.
 */
fun Modifier.dashedBorder(
    color: Color,
    radius: Dp,
    strokeWidth: Dp = 1.dp,
    dash: Dp = 5.dp,
    gap: Dp = 4.dp,
): Modifier = this.drawBehind {
    val stroke = Stroke(
        width = strokeWidth.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), gap.toPx()), 0f),
    )
    val inset = strokeWidth.toPx() / 2f
    drawRoundRect(
        color = color,
        topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
        size = Size(size.width - strokeWidth.toPx(), size.height - strokeWidth.toPx()),
        cornerRadius = CornerRadius(radius.toPx(), radius.toPx()),
        style = stroke,
    )
}
