package com.nexus.launcher.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nexus.launcher.ui.theme.NexusColor
import com.nexus.launcher.ui.theme.NexusRadius
import com.nexus.launcher.ui.theme.NexusType

/**
 * One-time prompt shown when Nexus is running but is not the device's home app.
 * Dismissing it with "Later" hides it for good — it is a nudge, not a gate.
 */
@Composable
fun DefaultLauncherCard(
    modifier: Modifier = Modifier,
    onSetDefault: () -> Unit,
    onLater: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NexusColor.HubBackground)
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(NexusRadius.CardLarge))
                .background(NexusColor.Card)
                .border(1.dp, NexusColor.Border, RoundedCornerShape(NexusRadius.CardLarge))
                .padding(24.dp),
        ) {
            Text(
                text = "Make Nexus your home",
                style = NexusType.PageTitle,
                color = NexusColor.Accent,
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Nexus replaces your home screen. Until it is your default, " +
                    "the home button will keep opening your old launcher.",
                style = NexusType.Body,
                color = NexusColor.TextSecondary,
            )

            Spacer(Modifier.height(22.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(NexusRadius.Button))
                    .background(NexusColor.Accent)
                    .clickable(onClick = onSetDefault)
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Set as default",
                    style = NexusType.CardTitle,
                    color = NexusColor.DeepMaroonText,
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Later",
                style = NexusType.CardTitleSemi,
                color = NexusColor.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onLater)
                    .padding(vertical = 13.dp),
            )
        }
    }
}
