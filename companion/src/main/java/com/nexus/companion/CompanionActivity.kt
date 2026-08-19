package com.nexus.companion

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Accent = Color(0xFFF6B1A4)
private val Background = Color(0xFF0C0E14)
private val Card = Color(0xFF141A24)
private val Border = Color(0xFF232A36)
private val TextPrimary = Color(0xFFE8EDF4)
private val TextSecondary = Color(0xFF8A94A6)
private val Green = Color(0xFF7DE0A3)
private val Amber = Color(0xFFE0A835)

/**
 * Status and setup screen. The companion has no UI of its own in normal use —
 * this exists so that when the feed does not appear, the reason is visible
 * somewhere instead of the Discover page simply staying blank.
 */
class CompanionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Accent,
                    background = Background,
                    surface = Card,
                )
            ) {
                CompanionScreen(
                    googleAppInstalled = isInstalled(GoogleOverlayClient.GOOGLE_APP_PACKAGE),
                    launcherInstalled = isInstalled(NexusOverlayService.LAUNCHER_PACKAGE),
                    onOpenLauncher = { openLauncher() },
                )
            }
        }
    }

    private fun isInstalled(packageName: String): Boolean =
        runCatching { packageManager.getPackageInfo(packageName, 0) }.isSuccess

    private fun openLauncher() {
        runCatching {
            packageManager.getLaunchIntentForPackage(NexusOverlayService.LAUNCHER_PACKAGE)
                ?.let { startActivity(it) }
        }
    }
}

@Composable
private fun CompanionScreen(
    googleAppInstalled: Boolean,
    launcherInstalled: Boolean,
    onOpenLauncher: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state by NexusOverlayService.connectionState.collectAsState()

    // Binding the service here starts it, which is what actually attempts the
    // Google connection and populates the status below.
    var bound by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                bound = true
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                bound = false
            }
        }
        val intent = Intent(context, NexusOverlayService::class.java)
        runCatching { context.bindService(intent, connection, Context.BIND_AUTO_CREATE) }
        onDispose { runCatching { context.unbindService(connection) } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            text = "Nexus Companion",
            color = Accent,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Bridges the Google Discover feed into the Nexus launcher.",
            color = TextSecondary,
            fontSize = 14.sp,
        )

        Spacer(Modifier.height(24.dp))

        StatusRow(
            label = "Google app",
            ok = googleAppInstalled,
            detail = if (googleAppInstalled) "Installed" else "Not installed",
        )
        StatusRow(
            label = "Nexus launcher",
            ok = launcherInstalled,
            detail = if (launcherInstalled) "Installed" else "Not installed",
        )
        StatusRow(
            label = "Overlay service",
            ok = state.connected,
            detail = if (state.connected) "Connected" else "Unavailable",
        )

        if (!state.connected && state.detail != null) {
            Spacer(Modifier.height(16.dp))
            InfoCard(title = "Why the feed is not showing", body = state.detail.orEmpty())
        }

        Spacer(Modifier.height(20.dp))

        InfoCard(
            title = "Setup",
            body = "1. Install this companion and Nexus from the same build — the " +
                "bridge between them is signature-checked, so a mismatched pair " +
                "will not connect.\n" +
                "2. Set Nexus as your home app.\n" +
                "3. In Nexus Settings → Apps, keep the Discover feed toggle on.\n" +
                "4. Swipe right from Home.",
        )

        Spacer(Modifier.height(14.dp))

        InfoCard(
            title = "Known limitation",
            body = "The Google app only serves its overlay to companion packages " +
                "it recognises. Self-signed builds like this one are typically " +
                "refused, and the overlay service returns no binding. Everything " +
                "on the Nexus side is wired and will light up if the Google app " +
                "accepts this package — but that acceptance is Google's to give.",
            accent = Amber,
        )

        if (launcherInstalled) {
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Accent)
                    .clickable(onClick = onOpenLauncher)
                    .padding(vertical = 13.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Open Nexus",
                    color = Color(0xFF5C1F16),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatusRow(label: String, ok: Boolean, detail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (ok) Green else Amber)
        )
        Spacer(Modifier.size(12.dp))
        Text(text = label, color = TextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(text = detail, color = if (ok) Green else Amber, fontSize = 13.sp)
    }
}

@Composable
private fun InfoCard(title: String, body: String, accent: Color = Accent) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Card)
            .border(1.dp, Border, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text(text = title, color = accent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(text = body, color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
    }
}
