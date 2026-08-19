package com.nexus.launcher.integration.emulators

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.nexus.launcher.domain.RomEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A retro system Nexus can recognise from a ROM's file extension. */
data class RomSystem(val id: String, val label: String, val extensions: Set<String>)

/** How to hand a ROM URI to a particular emulator app. */
data class EmulatorTarget(
    val id: String,
    val label: String,
    val packageName: String,
    val activity: String? = null,
    val systems: Set<String>,
)

object Emulators {

    val systems = listOf(
        RomSystem("snes", "SNES", setOf("smc", "sfc", "fig", "swc")),
        RomSystem("nes", "NES", setOf("nes", "fds", "unf")),
        RomSystem("gba", "GBA", setOf("gba", "agb")),
        RomSystem("gb", "GB/GBC", setOf("gb", "gbc")),
        RomSystem("n64", "N64", setOf("n64", "z64", "v64")),
        RomSystem("psp", "PSP", setOf("iso", "cso", "chd", "pbp")),
        RomSystem("ps1", "PS1", setOf("bin", "cue", "pbp", "img")),
        RomSystem("nds", "NDS", setOf("nds", "dsi")),
        RomSystem("gc", "GameCube", setOf("gcm", "gcz", "rvz")),
        RomSystem("wii", "Wii", setOf("wbfs", "wad")),
        RomSystem("md", "Genesis", setOf("md", "gen", "smd", "bin")),
    )

    /**
     * Per-emulator launch table. Most Android emulators accept a plain
     * ACTION_VIEW with the ROM URI; the ones that want an explicit component get
     * one, and RetroArch is the catch-all fallback for anything unmapped.
     */
    val targets = listOf(
        EmulatorTarget(
            id = "retroarch",
            label = "RetroArch",
            packageName = "com.retroarch",
            systems = systems.map { it.id }.toSet(),
        ),
        EmulatorTarget(
            id = "retroarch64",
            label = "RetroArch (64-bit)",
            packageName = "com.retroarch.aarch64",
            systems = systems.map { it.id }.toSet(),
        ),
        EmulatorTarget(
            id = "snes9x",
            label = "Snes9x EX+",
            packageName = "com.explusalpha.Snes9xPlus",
            systems = setOf("snes"),
        ),
        EmulatorTarget(
            id = "ppsspp",
            label = "PPSSPP",
            packageName = "org.ppsspp.ppsspp",
            activity = "org.ppsspp.ppsspp.PpssppActivity",
            systems = setOf("psp"),
        ),
        EmulatorTarget(
            id = "ppsspp_gold",
            label = "PPSSPP Gold",
            packageName = "org.ppsspp.ppssppgold",
            activity = "org.ppsspp.ppsspp.PpssppActivity",
            systems = setOf("psp"),
        ),
        EmulatorTarget(
            id = "m64plusfz",
            label = "M64Plus FZ",
            packageName = "org.mupen64plusae.v3.fzurita",
            systems = setOf("n64"),
        ),
        EmulatorTarget(
            id = "dolphin",
            label = "Dolphin",
            packageName = "org.dolphinemu.dolphinemu",
            systems = setOf("gc", "wii"),
        ),
        EmulatorTarget(
            id = "drastic",
            label = "DraStic",
            packageName = "com.dsemu.drastic",
            systems = setOf("nds"),
        ),
        EmulatorTarget(
            id = "duckstation",
            label = "DuckStation",
            packageName = "com.github.stenzek.duckstation",
            systems = setOf("ps1"),
        ),
        EmulatorTarget(
            id = "myboy",
            label = "My Boy!",
            packageName = "com.fastemulator.gba",
            systems = setOf("gba", "gb"),
        ),
    )

    fun systemForExtension(extension: String): RomSystem? {
        val ext = extension.lowercase().removePrefix(".")
        // Prefer a system that claims the extension uniquely over a shared one
        // like "bin", which several systems use.
        val matches = systems.filter { ext in it.extensions }
        return matches.minByOrNull { it.extensions.size }
    }

    fun installedTargets(context: Context): List<EmulatorTarget> {
        val pm = context.packageManager
        return targets.filter { target ->
            runCatching { pm.getPackageInfo(target.packageName, 0) }.isSuccess
        }
    }

    fun defaultTargetFor(context: Context, systemId: String): EmulatorTarget? {
        val installed = installedTargets(context)
        return installed.firstOrNull { systemId in it.systems && it.systems.size < systems.size }
            ?: installed.firstOrNull { systemId in it.systems }
    }

    /**
     * Scan every granted SAF folder for ROMs. Recurses one level into
     * per-system subfolders, which is how most ROM libraries are organised.
     */
    suspend fun scanFolders(context: Context, folderUris: List<String>): List<RomEntry> =
        withContext(Dispatchers.IO) {
            buildList {
                for (uriString in folderUris) {
                    val root = runCatching {
                        DocumentFile.fromTreeUri(context, Uri.parse(uriString))
                    }.getOrNull() ?: continue
                    collect(root, this, depth = 0)
                }
            }.distinctBy { it.uri }.sortedBy { it.displayName.lowercase() }
        }

    private fun collect(dir: DocumentFile, into: MutableList<RomEntry>, depth: Int) {
        if (depth > 2) return
        val children = runCatching { dir.listFiles() }.getOrNull() ?: return
        for (child in children) {
            if (child.isDirectory) {
                collect(child, into, depth + 1)
                continue
            }
            val name = child.name ?: continue
            val ext = name.substringAfterLast('.', "")
            if (ext.isBlank()) continue
            val system = systemForExtension(ext) ?: continue
            into += RomEntry(
                uri = child.uri.toString(),
                displayName = name.substringBeforeLast('.').replace('_', ' ').trim(),
                system = system.id,
                sizeBytes = child.length(),
            )
        }
    }

    /**
     * Fire the ROM at its emulator. Returns false when nothing could handle it,
     * so the UI can prompt the user to pick an emulator instead of failing silently.
     */
    fun launch(context: Context, rom: RomEntry, explicit: EmulatorTarget? = null): Boolean {
        val target = explicit
            ?: rom.preferredEmulator?.let { id -> targets.firstOrNull { it.id == id } }
            ?: defaultTargetFor(context, rom.system)

        val uri = Uri.parse(rom.uri)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/octet-stream")
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
            if (target != null) {
                `package` = target.packageName
                if (target.activity != null) {
                    component = ComponentName(target.packageName, target.activity)
                }
            }
        }

        if (runCatching { context.startActivity(intent); true }.getOrDefault(false)) return true

        // Component or package-scoped launch failed — offer the system chooser.
        val open = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/octet-stream")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return runCatching {
            context.startActivity(Intent.createChooser(open, "Open ROM with").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)
    }
}
