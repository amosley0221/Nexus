package com.nexus.launcher.ui.overlay

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.launcher.NexusLauncherActivity
import com.nexus.launcher.domain.AppEntry
import com.nexus.launcher.domain.PageConfig
import com.nexus.launcher.domain.RomEntry
import com.nexus.launcher.NexusApp
import com.nexus.launcher.integration.emulators.Emulators
import com.nexus.launcher.integration.notifications.NexusNotificationListener
import com.nexus.launcher.integration.widgets.NexusWidgetHost
import com.nexus.launcher.ui.LauncherHost
import com.nexus.launcher.ui.LauncherViewModel
import com.nexus.launcher.ui.common.OptionsSheet
import com.nexus.launcher.ui.common.SheetAction
import com.nexus.launcher.ui.edit.GalleryEntry
import com.nexus.launcher.ui.edit.PageEditor
import com.nexus.launcher.ui.edit.PageManager
import com.nexus.launcher.ui.edit.WidgetGallery
import com.nexus.launcher.ui.hub.ClaudeFeedPage
import com.nexus.launcher.ui.settings.AppToggleListPage
import com.nexus.launcher.ui.settings.IntegrationSettingsPage
import com.nexus.launcher.ui.settings.SettingsPage

/** Which full-screen overlay is showing above the pager. */
sealed interface OverlayRoute {
    data object None : OverlayRoute
    data object PageManager : OverlayRoute
    data class PageEditor(val pageId: String) : OverlayRoute
    data class WidgetGallery(val pageId: String) : OverlayRoute
    data object Settings : OverlayRoute
    data object Integrations : OverlayRoute
    data object AppLock : OverlayRoute
    data object HiddenApps : OverlayRoute
    data object Favorites : OverlayRoute
    data object ClaudeFeed : OverlayRoute
    data class AppOptions(val entry: AppEntry) : OverlayRoute
    data class RomOptions(val rom: RomEntry) : OverlayRoute
}

/** Routes the overlay stack. Back always steps one level toward None. */
@Composable
fun LauncherOverlay(
    route: OverlayRoute,
    viewModel: LauncherViewModel,
    host: LauncherHost,
    widgetHost: NexusWidgetHost,
    onRoute: (OverlayRoute) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val pages by viewModel.pages.collectAsStateWithLifecycle()
    val apps by viewModel.allApps.collectAsStateWithLifecycle()
    val appRepository = remember(context) {
        (context.applicationContext as NexusApp).appRepository
    }
    val discoverState by remember(context) {
        (context.applicationContext as NexusApp).discoverOverlay.state
    }.collectAsStateWithLifecycle()

    val discoverDiagnostics = buildString {
        append(if (discoverState.companionInstalled) "companion ✓" else "companion ✗")
        append(if (discoverState.bridgeBound) " · bridge ✓" else " · bridge ✗")
        append(if (discoverState.overlayConnected) " · google ✓" else " · google ✗")
        append(if (discoverState.windowAttached) " · window ✓" else " · window ✗")
        append(if (discoverState.hasContent) " · content ✓" else " · content ✗")
        append("\nstatus=")
        append(if (discoverState.lastStatus < 0) "never" else discoverState.lastStatus.toString())
        append(" · scroll=")
        append(
            if (discoverState.lastReportedScroll < 0f) "never"
            else "%.2f".format(discoverState.lastReportedScroll)
        )
        append(" · activity=")
        append(discoverState.lastActivityState)
        discoverState.unavailableReason?.let {
            append("\n")
            append(it)
        }
    }

    BackHandler(enabled = route != OverlayRoute.None) {
        when (route) {
            is OverlayRoute.PageEditor -> onRoute(OverlayRoute.PageManager)
            is OverlayRoute.WidgetGallery -> onRoute(OverlayRoute.PageEditor(route.pageId))
            OverlayRoute.Integrations, OverlayRoute.AppLock,
            OverlayRoute.HiddenApps, OverlayRoute.Favorites -> onRoute(OverlayRoute.Settings)
            else -> onClose()
        }
    }

    when (route) {
        OverlayRoute.None -> Unit

        OverlayRoute.PageManager -> PageManager(
            pages = pages,
            modifier = Modifier.fillMaxSize(),
            onReorder = viewModel::reorderPages,
            onToggleVisible = viewModel::togglePageVisible,
            onEditPage = { onRoute(OverlayRoute.PageEditor(it.id)) },
            onAddTemplate = viewModel::addPage,
            onOpenSettings = { onRoute(OverlayRoute.Settings) },
            onDone = onClose,
        )

        is OverlayRoute.PageEditor -> {
            val page: PageConfig? = pages.firstOrNull { it.id == route.pageId }
            if (page == null) {
                onRoute(OverlayRoute.PageManager)
            } else {
                PageEditor(
                    page = page,
                    modifier = Modifier.fillMaxSize(),
                    onToggleWidget = { widget ->
                        viewModel.updateWidget(page.id, widget) { it.copy(enabled = !it.enabled) }
                    },
                    onResizeWidget = { widget, size ->
                        viewModel.updateWidget(page.id, widget) { it.copy(size = size) }
                    },
                    onRemoveWidget = { viewModel.removeWidget(page.id, it) },
                    onAccentChange = { viewModel.setPageAccent(page.id, it) },
                    onAddWidget = { onRoute(OverlayRoute.WidgetGallery(page.id)) },
                    onDone = { onRoute(OverlayRoute.PageManager) },
                )
            }
        }

        is OverlayRoute.WidgetGallery -> {
            // Installed AppWidget providers appear alongside the built-in blocks.
            val providers = remember(widgetHost) {
                widgetHost.providers().map { info ->
                    GalleryEntry(
                        type = "appwidget:${info.provider.flattenToShortString()}",
                        name = info.loadLabel(context.packageManager) ?: info.provider.className,
                        category = "System",
                        sizes = "${info.minWidth}×${info.minHeight} dp",
                        providerPackage = info.provider.packageName,
                    )
                }
            }
            WidgetGallery(
                appWidgets = providers,
                modifier = Modifier.fillMaxSize(),
                onPick = { entry ->
                    val appWidgetId = if (entry.providerPackage != null) {
                        widgetHost.allocateId()
                    } else {
                        -1
                    }
                    viewModel.addWidget(
                        pageId = route.pageId,
                        type = entry.type,
                        gridX = 0,
                        gridY = 0,
                        appWidgetId = appWidgetId,
                    )
                    onRoute(OverlayRoute.PageEditor(route.pageId))
                },
                onDone = { onRoute(OverlayRoute.PageEditor(route.pageId)) },
            )
        }

        OverlayRoute.Settings -> SettingsPage(
            settings = settings,
            usageAccessGranted = appRepository.hasUsageAccess(),
            notificationAccessGranted = NexusNotificationListener.isEnabled(context),
            modifier = Modifier.fillMaxSize(),
            onUpdate = viewModel::updateSettings,
            onOpenAppLock = { onRoute(OverlayRoute.AppLock) },
            onOpenHiddenApps = { onRoute(OverlayRoute.HiddenApps) },
            onOpenFavorites = { onRoute(OverlayRoute.Favorites) },
            onRequestUsageAccess = { appRepository.requestUsageAccess() },
            onRequestNotificationAccess = { NexusNotificationListener.requestAccess(context) },
            onPickRomFolder = { host.pickRomFolder() },
            onOpenIntegrations = { onRoute(OverlayRoute.Integrations) },
            onSetDefaultLauncher = {
                (context as? NexusLauncherActivity)?.openHomeSettings()
            },
            discoverDiagnostics = discoverDiagnostics,
            onDone = onClose,
        )

        OverlayRoute.Integrations -> IntegrationSettingsPage(
            settings = settings,
            modifier = Modifier.fillMaxSize(),
            onUpdate = viewModel::updateSettings,
            onDone = { onRoute(OverlayRoute.Settings) },
        )

        OverlayRoute.AppLock -> AppToggleListPage(
            title = "App lock",
            subtitle = "Locked apps ask for your fingerprint before opening from Nexus",
            apps = apps,
            selected = settings.lockedApps,
            iconPack = settings.iconPack,
            lockedStyle = true,
            modifier = Modifier.fillMaxSize(),
            onToggle = viewModel::setLocked,
            onDone = { onRoute(OverlayRoute.Settings) },
        )

        OverlayRoute.HiddenApps -> AppToggleListPage(
            title = "Hidden apps",
            subtitle = "Hidden apps never appear in lists or search",
            apps = apps,
            selected = settings.hiddenApps,
            iconPack = settings.iconPack,
            modifier = Modifier.fillMaxSize(),
            onToggle = viewModel::setHidden,
            onDone = { onRoute(OverlayRoute.Settings) },
        )

        OverlayRoute.Favorites -> {
            val favoriteKeys = remember(settings.favorites) { settings.favorites.toSet() }
            AppToggleListPage(
                title = "Home favourites",
                subtitle = "Shown on Home in the order you add them",
                apps = apps,
                selected = favoriteKeys,
                iconPack = settings.iconPack,
                modifier = Modifier.fillMaxSize(),
                onToggle = { entry, _ -> viewModel.toggleFavorite(entry) },
                onDone = { onRoute(OverlayRoute.Settings) },
            )
        }

        OverlayRoute.ClaudeFeed -> {
            val tasks by viewModel.claudeTasks.collectAsStateWithLifecycle()
            val live by viewModel.claudeLive.collectAsStateWithLifecycle()
            ClaudeFeedPage(
                tasks = tasks,
                isLive = live,
                modifier = Modifier.fillMaxSize(),
                onReply = { },
                onOpenOnDesktop = { },
                onConfigure = { onRoute(OverlayRoute.Integrations) },
            )
        }

        is OverlayRoute.AppOptions -> {
            val entry = route.entry
            val isFavorite = entry.key in settings.favorites
            val isLocked = entry.key in settings.lockedApps
            OptionsSheet(
                title = entry.label,
                meta = entry.packageName,
                artSeed = entry.packageName,
                primaryLabel = "Open ${entry.label}",
                onPrimary = {
                    onClose()
                    host.launchApp(entry, null)
                },
                actions = listOf(
                    SheetAction(
                        label = if (isFavorite) "Remove from Home favourites" else "Add to Home favourites",
                    ) {
                        viewModel.toggleFavorite(entry)
                        onClose()
                    },
                    SheetAction(label = if (isLocked) "Unlock app" else "Lock with fingerprint") {
                        viewModel.setLocked(entry, !isLocked)
                        onClose()
                    },
                    SheetAction(label = "Hide from app list") {
                        viewModel.setHidden(entry, true)
                        onClose()
                    },
                    SheetAction(label = "App info") {
                        appRepository.openAppInfo(entry)
                        onClose()
                    },
                    SheetAction(label = "Uninstall", destructive = true) {
                        appRepository.requestUninstall(entry)
                        onClose()
                    },
                ),
                onDismiss = onClose,
            )
        }

        is OverlayRoute.RomOptions -> {
            val rom = route.rom
            val systemLabel = remember(rom.system) {
                Emulators.systems.firstOrNull { it.id == rom.system }?.label ?: rom.system
            }
            val candidates = remember(rom.system) {
                Emulators.installedTargets(context).filter { rom.system in it.systems }
            }
            val current = remember(rom.preferredEmulator, candidates) {
                rom.preferredEmulator?.let { id -> candidates.firstOrNull { it.id == id } }
                    ?: Emulators.defaultTargetFor(context, rom.system)
            }

            OptionsSheet(
                title = rom.displayName,
                meta = "$systemLabel · ${formatSize(rom.sizeBytes)}",
                artSeed = rom.displayName,
                primaryLabel = current?.let { "▶ Play — opens in ${it.label}" }
                    ?: "▶ Play — choose an emulator",
                onPrimary = {
                    onClose()
                    Emulators.launch(context, rom, current)
                },
                actions = buildList {
                    add(
                        SheetAction(
                            label = "Resume save state",
                            detail = "Handled by the emulator itself",
                        ) { onClose() }
                    )
                    if (candidates.isEmpty()) {
                        add(
                            SheetAction(
                                label = "No emulator installed for $systemLabel",
                                detail = "Install one, then reopen this sheet",
                            ) { onClose() }
                        )
                    } else {
                        candidates.forEach { target ->
                            add(
                                SheetAction(
                                    label = "Default emulator: ${target.label}",
                                    detail = if (target.id == current?.id) "Currently selected" else null,
                                ) {
                                    viewModel.setRomEmulator(rom, target.id)
                                    onClose()
                                }
                            )
                        }
                    }
                    add(SheetAction(label = "Rescan ROM folders") {
                        viewModel.rescanRoms()
                        onClose()
                    })
                },
                onDismiss = onClose,
            )
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576 -> "%.0f MB".format(bytes / 1_048_576.0)
    bytes > 0 -> "${bytes / 1024} KB"
    else -> "unknown size"
}
