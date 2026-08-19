package com.nexus.launcher.integration.apps

import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import com.nexus.launcher.domain.AppEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Reads the real installed-app list through LauncherApps (the API a launcher is
 * meant to use — it reports work-profile activities and package changes, unlike
 * a plain PackageManager query) and launches activities with the source bounds
 * Android wants for its app-open animation.
 */
class AppRepository(private val context: Context) {

    private val launcherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    private var callback: LauncherApps.Callback? = null

    suspend fun refresh() = withContext(Dispatchers.IO) {
        val usage = usageByPackage()
        val entries = buildList {
            for (user in profiles()) {
                for (info in runCatching { launcherApps.getActivityList(null, user) }.getOrDefault(emptyList())) {
                    val appInfo = info.applicationInfo
                    add(
                        AppEntry(
                            packageName = info.componentName.packageName,
                            activityName = info.componentName.className,
                            label = info.label?.toString().orEmpty().ifBlank { info.componentName.packageName },
                            icon = runCatching {
                                launcherApps.getActivityIcon(info, context.resources.displayMetrics.densityDpi)
                            }.getOrNull() ?: runCatching { info.getBadgedIcon(0) }.getOrNull(),
                            isGame = isGame(appInfo),
                            isSystem = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                            usageMillis = usage[info.componentName.packageName] ?: 0L,
                        )
                    )
                }
            }
        }
            .distinctBy { it.key }
            .sortedBy { it.label.lowercase() }

        _apps.value = entries
    }

    private fun profiles(): List<UserHandle> =
        runCatching { launcherApps.profiles }.getOrNull()?.takeIf { it.isNotEmpty() }
            ?: listOf(Process.myUserHandle())

    private fun isGame(info: ApplicationInfo): Boolean =
        info.category == ApplicationInfo.CATEGORY_GAME ||
            @Suppress("DEPRECATION")
            (info.flags and ApplicationInfo.FLAG_IS_GAME != 0)

    /** Trailing-30-day foreground time. Silently empty until the user grants access. */
    private fun usageByPackage(): Map<String, Long> {
        if (!hasUsageAccess()) return emptyMap()
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyMap()
        val end = System.currentTimeMillis()
        val start = end - TimeUnit.DAYS.toMillis(30)
        return runCatching {
            manager.queryAndAggregateUsageStats(start, end)
                .mapValues { (_, stats) -> stats.totalTimeInForeground }
        }.getOrDefault(emptyMap())
    }

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? android.app.AppOpsManager
            ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    fun requestUsageAccess() {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /** Launch an app. [sourceBounds] gives the system the icon rect to animate from. */
    fun launch(entry: AppEntry, sourceBounds: Rect? = null) {
        val component = ComponentName(entry.packageName, entry.activityName)
        val launched = runCatching {
            launcherApps.startMainActivity(component, Process.myUserHandle(), sourceBounds, null)
            true
        }.getOrDefault(false)

        if (!launched) {
            runCatching {
                val intent = Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(component)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                intent.sourceBounds = sourceBounds
                context.startActivity(intent)
            }.onFailure { Log.w(TAG, "Could not launch ${entry.key}", it) }
        }
    }

    fun openAppInfo(entry: AppEntry) {
        runCatching {
            launcherApps.startAppDetailsActivity(
                ComponentName(entry.packageName, entry.activityName),
                Process.myUserHandle(),
                null,
                null,
            )
        }
    }

    fun requestUninstall(entry: AppEntry) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_DELETE)
                    .setData(android.net.Uri.parse("package:${entry.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /** Keeps the list live as packages are installed, removed, or changed. */
    fun registerCallback(onChanged: () -> Unit) {
        if (callback != null) return
        val cb = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String?, user: UserHandle?) = onChanged()
            override fun onPackageAdded(packageName: String?, user: UserHandle?) = onChanged()
            override fun onPackageChanged(packageName: String?, user: UserHandle?) = onChanged()
            override fun onPackagesAvailable(
                packageNames: Array<out String>?,
                user: UserHandle?,
                replacing: Boolean,
            ) = onChanged()

            override fun onPackagesUnavailable(
                packageNames: Array<out String>?,
                user: UserHandle?,
                replacing: Boolean,
            ) = onChanged()
        }
        runCatching { launcherApps.registerCallback(cb) }.onSuccess { callback = cb }
    }

    fun unregisterCallback() {
        callback?.let { cb -> runCatching { launcherApps.unregisterCallback(cb) } }
        callback = null
    }

    private companion object {
        const val TAG = "AppRepository"
    }
}
