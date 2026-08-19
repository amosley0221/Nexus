package com.nexus.launcher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.launcher.NexusApp
import com.nexus.launcher.data.NexusSettings
import com.nexus.launcher.domain.AppEntry
import com.nexus.launcher.domain.BudgetState
import com.nexus.launcher.domain.PageConfig
import com.nexus.launcher.domain.PageKind
import com.nexus.launcher.domain.RomEntry
import com.nexus.launcher.domain.WidgetConfig
import com.nexus.launcher.integration.emulators.Emulators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Owns launcher state: the app list, persisted settings and pages, and the
 * derived collections each screen renders.
 */
class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NexusApp
    private val repo = app.settingsRepository

    val settings: StateFlow<NexusSettings> =
        repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, NexusSettings())

    val pages: StateFlow<List<PageConfig>> =
        repo.pages.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val budget: StateFlow<BudgetState> =
        repo.budget.stateIn(viewModelScope, SharingStarted.Eagerly, BudgetState())

    val roms: StateFlow<List<RomEntry>> =
        repo.roms.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allApps: StateFlow<List<AppEntry>> = app.appRepository.apps

    val nowPlaying = app.nowPlaying.state
    val claudeTasks = app.claudeBridge.tasks
    val claudeLive = app.claudeBridge.live

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** Apps the user has not hidden, which is what every list starts from. */
    val visibleApps: StateFlow<List<AppEntry>> =
        combine(allApps, settings) { apps, config ->
            apps.filterNot { it.key in config.hiddenApps || it.packageName in config.hiddenApps }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Home favourites, in the order the user pinned them. Empty until they pin
     * something — guessing from usage would put apps on Home that were never
     * chosen, and there would be no way to tell a guess from a real pin.
     */
    val favorites: StateFlow<List<AppEntry>> =
        combine(visibleApps, settings) { apps, config ->
            config.favorites.mapNotNull { key -> apps.firstOrNull { it.key == key } }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val games: StateFlow<List<AppEntry>> =
        visibleApps.let { flow ->
            combine(flow, settings) { apps, _ ->
                apps.filter { it.isGame }.sortedByDescending { it.usageMillis }
            }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        }

    val searchResults: StateFlow<List<AppEntry>> =
        combine(visibleApps, searchQuery) { apps, query ->
            if (query.isBlank()) {
                emptyList()
            } else {
                apps.filter { it.label.contains(query, ignoreCase = true) }
                    .sortedBy { entry ->
                        // Prefix matches rank above mid-word matches.
                        if (entry.label.startsWith(query, ignoreCase = true)) 0 else 1
                    }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Newest installs first, for the empty state of the search overscroll. */
    val recentlyInstalled: StateFlow<List<AppEntry>> =
        combine(visibleApps, settings) { apps, _ ->
            apps.takeLast(8).reversed()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Pages the pager actually shows, honouring visibility and the Discover toggle. */
    val activePages: StateFlow<List<PageConfig>> =
        combine(pages, settings) { list, config ->
            list.filter { page ->
                page.visible && (page.kind != PageKind.Discover || config.discoverEnabled)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        refreshApps()
        app.appRepository.registerCallback { refreshApps() }
        viewModelScope.launch {
            settings.collect { config ->
                app.claudeBridge.configure(config.claudeRelayUrl)
                app.claudeBridge.setSamplePreview(config.claudeSamplePreview)
            }
        }
    }

    fun refreshApps() {
        viewModelScope.launch { app.appRepository.refresh() }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSettings(transform: (NexusSettings) -> NexusSettings) {
        viewModelScope.launch { repo.updateSettings(transform) }
    }

    fun toggleFavorite(entry: AppEntry) {
        updateSettings { config ->
            val current = config.favorites
            config.copy(
                favorites = if (entry.key in current) current - entry.key else current + entry.key
            )
        }
    }

    fun setHidden(entry: AppEntry, hidden: Boolean) {
        updateSettings { config ->
            config.copy(
                hiddenApps = if (hidden) config.hiddenApps + entry.key else config.hiddenApps - entry.key
            )
        }
    }

    fun setLocked(entry: AppEntry, locked: Boolean) {
        updateSettings { config ->
            config.copy(
                lockedApps = if (locked) config.lockedApps + entry.key else config.lockedApps - entry.key
            )
        }
    }

    fun isLocked(entry: AppEntry): Boolean {
        val config = settings.value
        return config.appLockEnabled && entry.key in config.lockedApps
    }

    // ---- Pages -------------------------------------------------------------

    fun reorderPages(from: Int, to: Int) {
        viewModelScope.launch {
            repo.updatePages { list ->
                if (from !in list.indices || to !in list.indices) {
                    list
                } else {
                    list.toMutableList().apply { add(to, removeAt(from)) }
                }
            }
        }
    }

    fun togglePageVisible(page: PageConfig) {
        viewModelScope.launch {
            repo.updatePages { list ->
                list.map { if (it.id == page.id) it.copy(visible = !it.visible) else it }
            }
        }
    }

    fun addPage(kind: PageKind) {
        viewModelScope.launch {
            repo.updatePages { list ->
                list + PageConfig(id = UUID.randomUUID().toString(), kind = kind)
            }
        }
    }

    fun setPageAccent(pageId: String, color: Long) {
        viewModelScope.launch {
            repo.updatePages { list ->
                list.map { if (it.id == pageId) it.copy(accentColor = color) else it }
            }
        }
    }

    fun addWidget(pageId: String, type: String, gridX: Int, gridY: Int, appWidgetId: Int = -1) {
        viewModelScope.launch {
            repo.updatePages { list ->
                list.map { page ->
                    if (page.id != pageId) {
                        page
                    } else {
                        page.copy(
                            widgets = page.widgets + WidgetConfig(
                                id = UUID.randomUUID().toString(),
                                type = type,
                                gridX = gridX,
                                gridY = gridY,
                                appWidgetId = appWidgetId,
                            )
                        )
                    }
                }
            }
        }
    }

    fun updateWidget(pageId: String, widget: WidgetConfig, transform: (WidgetConfig) -> WidgetConfig) {
        viewModelScope.launch {
            repo.updatePages { list ->
                list.map { page ->
                    if (page.id != pageId) {
                        page
                    } else {
                        page.copy(widgets = page.widgets.map { if (it.id == widget.id) transform(it) else it })
                    }
                }
            }
        }
    }

    fun removeWidget(pageId: String, widget: WidgetConfig) {
        if (widget.appWidgetId >= 0) app.widgetHost.deleteId(widget.appWidgetId)
        viewModelScope.launch {
            repo.updatePages { list ->
                list.map { page ->
                    if (page.id != pageId) page else page.copy(widgets = page.widgets - widget)
                }
            }
        }
    }

    // ---- ROMs --------------------------------------------------------------

    fun addRomFolder(uri: String) {
        updateSettings { it.copy(romFolderUris = (it.romFolderUris + uri).distinct()) }
        rescanRoms(listOf(uri))
    }

    fun rescanRoms(extraUris: List<String> = emptyList()) {
        viewModelScope.launch {
            val folders = (settings.value.romFolderUris + extraUris).distinct()
            if (folders.isEmpty()) return@launch
            val found = Emulators.scanFolders(app, folders)
            repo.updateRoms { existing ->
                // Preserve any per-ROM choices the user already made.
                found.map { rom ->
                    existing.firstOrNull { it.uri == rom.uri }?.let { previous ->
                        rom.copy(
                            preferredEmulator = previous.preferredEmulator,
                            artUri = previous.artUri ?: rom.artUri,
                        )
                    } ?: rom
                }
            }
        }
    }

    fun setRomEmulator(rom: RomEntry, emulatorId: String) {
        viewModelScope.launch {
            repo.updateRoms { list ->
                list.map { if (it.uri == rom.uri) it.copy(preferredEmulator = emulatorId) else it }
            }
        }
    }
}
