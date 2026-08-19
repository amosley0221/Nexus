package com.nexus.launcher.ui.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.launcher.domain.ClaudeStatus
import com.nexus.launcher.domain.ClaudeTask
import com.nexus.launcher.ui.common.Badge
import com.nexus.launcher.ui.common.NexusProgressBar
import com.nexus.launcher.ui.theme.NexusColor
import com.nexus.launcher.ui.theme.NexusRadius
import com.nexus.launcher.ui.theme.NexusType

/**
 * The Claude activity feed. Task cards render per status: RUNNING with a step
 * checklist and progress bar, NEEDS YOU with the question and actions, DONE
 * dimmed to 75%.
 */
@Composable
fun ClaudeFeedPage(
    tasks: List<ClaudeTask>,
    isLive: Boolean,
    modifier: Modifier = Modifier,
    onReply: (ClaudeTask) -> Unit,
    onOpenOnDesktop: (ClaudeTask) -> Unit,
    onConfigure: () -> Unit,
) {
    val deviceCount = tasks.mapTo(HashSet()) { it.device }.count { it.isNotBlank() }

    HubScaffold(
        title = "✳ Claude",
        modifier = modifier,
        trailing = {
            Text(
                text = if (isLive) "$deviceCount devices" else "relay not set",
                style = NexusType.BodySmall,
                color = if (isLive) NexusColor.TextSecondary else NexusColor.Amber,
                modifier = Modifier.clickable(onClick = onConfigure),
            )
        },
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(tasks, key = { it.id }) { task ->
                ClaudeTaskCard(
                    task = task,
                    onReply = { onReply(task) },
                    onOpenOnDesktop = { onOpenOnDesktop(task) },
                )
            }
        }
    }
}

@Composable
private fun ClaudeTaskCard(
    task: ClaudeTask,
    onReply: () -> Unit,
    onOpenOnDesktop: () -> Unit,
) {
    val (badgeText, badgeBg, badgeFg) = when (task.status) {
        ClaudeStatus.Running -> Triple("RUNNING", NexusColor.ChipActiveBg, NexusColor.Accent)
        ClaudeStatus.NeedsYou -> Triple("NEEDS YOU", NexusColor.AmberBg, NexusColor.Amber)
        ClaudeStatus.Done -> Triple("DONE", NexusColor.GreenBg, NexusColor.Green)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (task.status == ClaudeStatus.Done) 0.75f else 1f)
            .clip(RoundedCornerShape(NexusRadius.CardLarge))
            .background(NexusColor.Card)
            .border(1.dp, NexusColor.Border, RoundedCornerShape(NexusRadius.CardLarge))
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = task.title,
                style = NexusType.CardTitle,
                color = NexusColor.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Badge(text = badgeText, background = badgeBg, foreground = badgeFg, radius = 8.dp)
        }

        if (task.detail.isNotBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(text = task.detail, style = NexusType.Meta, color = NexusColor.TextSecondary)
        }

        when (task.status) {
            ClaudeStatus.Running -> {
                Spacer(Modifier.height(10.dp))
                task.steps.forEach { step ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 2.dp),
                    ) {
                        Text(
                            text = if (step.done) "✓" else "●",
                            style = NexusType.Meta,
                            color = if (step.done) NexusColor.Green else NexusColor.Accent,
                        )
                        Text(
                            text = step.label,
                            style = NexusType.Meta,
                            color = NexusColor.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                NexusProgressBar(progress = task.progress, modifier = Modifier.fillMaxWidth())
            }

            ClaudeStatus.NeedsYou -> {
                if (task.question != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = task.question,
                        style = NexusType.BodySmall,
                        color = NexusColor.TextPrimary,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionButton(
                        text = "Reply",
                        filled = true,
                        modifier = Modifier.weight(1f),
                        onClick = onReply,
                    )
                    ActionButton(
                        text = "Open on Mac",
                        filled = false,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenOnDesktop,
                    )
                }
            }

            ClaudeStatus.Done -> Unit
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    filled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(NexusRadius.Button)
    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (filled) {
                    Modifier.background(NexusColor.Accent)
                } else {
                    Modifier.border(1.dp, NexusColor.Border, shape)
                }
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = NexusType.CardTitle,
            color = if (filled) NexusColor.DeepMaroonText else NexusColor.TextPrimary,
            maxLines = 1,
        )
    }
}
