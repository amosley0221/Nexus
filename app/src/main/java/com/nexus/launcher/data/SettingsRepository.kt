package com.nexus.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nexus.launcher.domain.BudgetState
import com.nexus.launcher.domain.PageConfig
import com.nexus.launcher.domain.PageKind
import com.nexus.launcher.domain.RomEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "nexus")

/**
 * Single source of truth for persisted launcher state. Each blob is stored as
 * JSON under its own key so adding a field never invalidates the others.
 */
class SettingsRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val keySettings = stringPreferencesKey("settings_json")
    private val keyPages = stringPreferencesKey("pages_json")
    private val keyBudget = stringPreferencesKey("budget_json")
    private val keyRoms = stringPreferencesKey("roms_json")

    val settings: Flow<NexusSettings> = context.dataStore.data.map { prefs ->
        decode(prefs, keySettings, NexusSettings())
    }

    val pages: Flow<List<PageConfig>> = context.dataStore.data.map { prefs ->
        prefs[keyPages]?.let { runCatching { json.decodeFromString<List<PageConfig>>(it) }.getOrNull() }
            ?: defaultPages()
    }

    val budget: Flow<BudgetState> = context.dataStore.data.map { prefs ->
        decode(prefs, keyBudget, SampleData.budget())
    }

    val roms: Flow<List<RomEntry>> = context.dataStore.data.map { prefs ->
        prefs[keyRoms]?.let { runCatching { json.decodeFromString<List<RomEntry>>(it) }.getOrNull() }
            ?: emptyList()
    }

    private inline fun <reified T> decode(prefs: Preferences, key: Preferences.Key<String>, fallback: T): T =
        prefs[key]?.let { runCatching { json.decodeFromString<T>(it) }.getOrNull() } ?: fallback

    suspend fun updateSettings(transform: (NexusSettings) -> NexusSettings) {
        context.dataStore.edit { prefs ->
            val current = decode(prefs, keySettings, NexusSettings())
            prefs[keySettings] = json.encodeToString(transform(current))
        }
    }

    suspend fun updatePages(transform: (List<PageConfig>) -> List<PageConfig>) {
        context.dataStore.edit { prefs ->
            val current = prefs[keyPages]
                ?.let { runCatching { json.decodeFromString<List<PageConfig>>(it) }.getOrNull() }
                ?: defaultPages()
            prefs[keyPages] = json.encodeToString(transform(current))
        }
    }

    suspend fun updateBudget(transform: (BudgetState) -> BudgetState) {
        context.dataStore.edit { prefs ->
            val current = decode(prefs, keyBudget, SampleData.budget())
            prefs[keyBudget] = json.encodeToString(transform(current))
        }
    }

    suspend fun updateRoms(transform: (List<RomEntry>) -> List<RomEntry>) {
        context.dataStore.edit { prefs ->
            val current = prefs[keyRoms]
                ?.let { runCatching { json.decodeFromString<List<RomEntry>>(it) }.getOrNull() }
                ?: emptyList()
            prefs[keyRoms] = json.encodeToString(transform(current))
        }
    }

    companion object {
        fun defaultPages(): List<PageConfig> = listOf(
            PageKind.Discover,
            PageKind.Home,
            PageKind.Games,
            PageKind.Media,
            PageKind.Music,
            PageKind.Budget,
            PageKind.Files,
        ).map { kind ->
            PageConfig(
                id = kind.name.lowercase(),
                kind = kind,
                accentColor = when (kind) {
                    PageKind.Games -> 0xFF8B5CF6
                    PageKind.Media -> 0xFFE8A33D
                    PageKind.Music -> 0xFF7DE0A3
                    PageKind.Budget -> 0xFF8AB0F0
                    PageKind.Files -> 0xFFE0A835
                    else -> 0xFFF6B1A4
                },
            )
        }
    }
}
