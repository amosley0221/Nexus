package com.nexus.launcher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.nexus.launcher.data.NexusSettings
import com.nexus.launcher.ui.hub.HubScaffold
import com.nexus.launcher.ui.theme.NexusColor
import com.nexus.launcher.ui.theme.NexusRadius
import com.nexus.launcher.ui.theme.NexusType

/**
 * Endpoints and tokens for the integrations that need an account. Each field is
 * stored locally in the launcher's own DataStore and never leaves the device
 * except to the service it names.
 */
@Composable
fun IntegrationSettingsPage(
    settings: NexusSettings,
    modifier: Modifier = Modifier,
    onUpdate: ((NexusSettings) -> NexusSettings) -> Unit,
    onDone: () -> Unit,
) {
    HubScaffold(
        title = "Integrations",
        modifier = modifier,
        trailing = {
            Text(
                text = "Done",
                style = NexusType.CardTitle,
                color = NexusColor.Accent,
                modifier = Modifier.clickable(onClick = onDone),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsSection("PLEX")
            FieldRow(
                label = "Server URL",
                value = settings.plexServerUrl,
                hint = "http://192.168.1.10:32400",
                onChange = { value -> onUpdate { it.copy(plexServerUrl = value) } },
            )
            FieldRow(
                label = "Token",
                value = settings.plexToken,
                hint = "X-Plex-Token",
                secret = true,
                onChange = { value -> onUpdate { it.copy(plexToken = value) } },
            )

            SettingsSection("TWITCH")
            FieldRow(
                label = "Login",
                value = settings.twitchLogin,
                hint = "your channel name",
                onChange = { value -> onUpdate { it.copy(twitchLogin = value) } },
            )
            FieldRow(
                label = "OAuth token",
                value = settings.twitchToken,
                hint = "user:read:follows scope",
                secret = true,
                onChange = { value -> onUpdate { it.copy(twitchToken = value) } },
            )

            SettingsSection("CLAUDE RELAY")
            FieldRow(
                label = "Relay base URL",
                value = settings.claudeRelayUrl,
                hint = "http://mac.local:8787",
                onChange = { value -> onUpdate { it.copy(claudeRelayUrl = value) } },
            )
            Text(
                text = "Claude Code hooks on your desktop POST task events to this relay; " +
                    "Nexus polls /tasks and posts replies to /tasks/{id}/reply. " +
                    "Leave blank to show the sample feed.",
                style = NexusType.Meta,
                color = NexusColor.TextFaint,
                modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
            )
        }
    }
}

@Composable
private fun FieldRow(
    label: String,
    value: String,
    hint: String,
    secret: Boolean = false,
    onChange: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(label, style = NexusType.Meta, color = NexusColor.TextSecondary)
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(NexusRadius.CardSmall))
                .background(NexusColor.Card)
                .padding(horizontal = 12.dp, vertical = 11.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = NexusType.BodySmall.copy(color = NexusColor.TextPrimary),
                cursorBrush = SolidColor(NexusColor.Accent),
                visualTransformation = if (secret) {
                    PasswordVisualTransformation()
                } else {
                    androidx.compose.ui.text.input.VisualTransformation.None
                },
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(hint, style = NexusType.BodySmall, color = NexusColor.TextFaint)
                    }
                    inner()
                },
            )
        }
    }
}
