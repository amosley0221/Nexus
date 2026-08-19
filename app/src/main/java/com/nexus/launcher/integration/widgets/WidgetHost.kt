package com.nexus.launcher.integration.widgets

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Hosts system AppWidgets — KWGT included — inside the launcher's pages.
 *
 * The host id is fixed for the process; ids for individual widgets are allocated
 * here and persisted in the page's [com.nexus.launcher.domain.WidgetConfig].
 */
class NexusWidgetHost(context: Context) {

    private val appContext = context.applicationContext
    val manager: AppWidgetManager = AppWidgetManager.getInstance(appContext)
    private val host = AppWidgetHost(appContext, HOST_ID)

    fun startListening() = runCatching { host.startListening() }
    fun stopListening() = runCatching { host.stopListening() }

    fun allocateId(): Int = host.allocateAppWidgetId()

    fun deleteId(appWidgetId: Int) {
        runCatching { host.deleteAppWidgetId(appWidgetId) }
    }

    fun providers(): List<AppWidgetProviderInfo> =
        runCatching { manager.installedProviders }.getOrDefault(emptyList())

    fun infoFor(appWidgetId: Int): AppWidgetProviderInfo? =
        runCatching { manager.getAppWidgetInfo(appWidgetId) }.getOrNull()

    fun createView(context: Context, appWidgetId: Int): AppWidgetHostView? {
        val info = infoFor(appWidgetId) ?: return null
        return runCatching { host.createView(context, appWidgetId, info) }.getOrNull()
    }

    /**
     * Some providers need explicit binding permission. Returns true when the
     * widget is already bound, false when the caller must show the system dialog.
     */
    fun bindIfAllowed(appWidgetId: Int, provider: android.content.ComponentName): Boolean =
        runCatching { manager.bindAppWidgetIdIfAllowed(appWidgetId, provider) }.getOrDefault(false)

    fun bindRequestIntent(appWidgetId: Int, provider: android.content.ComponentName): Intent =
        Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
        }

    /** Widgets with a configuration activity must be configured before first use. */
    fun startConfigure(activity: Activity, appWidgetId: Int, requestCode: Int) {
        runCatching {
            host.startAppWidgetConfigureActivityForResult(
                activity,
                appWidgetId,
                0,
                requestCode,
                null,
            )
        }
    }

    /** Tell the provider how much room it has, so responsive widgets reflow. */
    fun updateSize(appWidgetId: Int, widthDp: Int, heightDp: Int) {
        runCatching {
            manager.updateAppWidgetOptions(
                appWidgetId,
                Bundle().apply {
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp)
                },
            )
        }
    }

    companion object {
        const val HOST_ID = 0x4E58 // "NX"
        const val REQUEST_BIND = 9001
        const val REQUEST_CONFIGURE = 9002
    }
}

/** Renders a bound AppWidget inside Compose. */
@Composable
fun AppWidgetSlot(
    host: NexusWidgetHost,
    appWidgetId: Int,
    widthDp: Dp,
    heightDp: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val info = remember(appWidgetId) { host.infoFor(appWidgetId) }
    if (info == null) return

    AndroidView(
        factory = { ctx ->
            host.createView(ctx, appWidgetId) ?: android.widget.FrameLayout(ctx)
        },
        update = { view ->
            host.updateSize(appWidgetId, widthDp.value.toInt(), heightDp.value.toInt())
            if (view is AppWidgetHostView) {
                view.updateAppWidgetSize(
                    Bundle(),
                    widthDp.value.toInt(),
                    heightDp.value.toInt(),
                    widthDp.value.toInt(),
                    heightDp.value.toInt(),
                )
            }
        },
        modifier = modifier,
    )
}
