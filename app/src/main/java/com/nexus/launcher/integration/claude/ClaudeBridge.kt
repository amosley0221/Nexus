package com.nexus.launcher.integration.claude

import android.util.Log
import com.nexus.launcher.data.SampleData
import com.nexus.launcher.domain.ClaudeStatus
import com.nexus.launcher.domain.ClaudeTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/**
 * Client for the desktop-side relay described in the handoff: Claude Code hooks
 * post task events to a small service, and the phone polls it for the home pill,
 * the activity feed, and the widget.
 *
 * No relay is configured out of the box, so the feed falls back to the sample
 * tasks — that keeps the screens reviewable on-device before the desktop half
 * exists. Point [relayUrl] at a real endpoint in Nexus Settings to go live.
 */
class ClaudeBridge(private val scope: CoroutineScope) {

    private val json = Json { ignoreUnknownKeys = true }

    private val _tasks = MutableStateFlow(SampleData.claudeTasks())
    val tasks: StateFlow<List<ClaudeTask>> = _tasks.asStateFlow()

    private val _live = MutableStateFlow(false)
    val live: StateFlow<Boolean> = _live.asStateFlow()

    private var pollJob: Job? = null
    private var relayUrl: String = ""

    /** The single line shown on Home; null hides it, as the design requires. */
    val homeLine: String?
        get() = _tasks.value.firstOrNull { it.status == ClaudeStatus.Running }?.let { task ->
            "✳ Claude: ${task.title.lowercase()} ${(task.progress * 100).toInt()}% ›"
        } ?: _tasks.value.firstOrNull { it.status == ClaudeStatus.NeedsYou }?.let { task ->
            "✳ Claude: needs you — ${task.title.lowercase()} ›"
        }

    fun configure(url: String) {
        if (url == relayUrl) return
        relayUrl = url
        pollJob?.cancel()
        if (url.isBlank()) {
            _live.value = false
            _tasks.value = SampleData.claudeTasks()
            return
        }
        pollJob = scope.launch {
            while (isActive) {
                poll()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun poll() = withContext(Dispatchers.IO) {
        val body = runCatching {
            val connection = (URL("${relayUrl.trimEnd('/')}/tasks").openConnection() as HttpURLConnection).apply {
                connectTimeout = 5_000
                readTimeout = 5_000
                requestMethod = "GET"
            }
            connection.use { it.inputStream.bufferedReader().readText() }
        }.getOrElse {
            Log.d(TAG, "Claude relay unreachable: ${it.message}")
            _live.value = false
            return@withContext
        }

        val parsed = runCatching { json.decodeFromString<List<ClaudeTask>>(body) }.getOrNull()
        if (parsed != null) {
            _tasks.value = parsed
            _live.value = true
        }
    }

    /** Answer a NEEDS YOU task from the phone. */
    fun reply(taskId: String, text: String) {
        if (relayUrl.isBlank()) return
        scope.launch(Dispatchers.IO) {
            runCatching {
                val connection = (URL("${relayUrl.trimEnd('/')}/tasks/$taskId/reply")
                    .openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 5_000
                    readTimeout = 5_000
                    setRequestProperty("Content-Type", "application/json")
                }
                connection.use { conn ->
                    conn.outputStream.write(
                        json.encodeToString(mapOf("reply" to text)).toByteArray()
                    )
                    conn.responseCode
                }
            }.onFailure { Log.w(TAG, "Reply failed", it) }
            poll()
        }
    }

    private inline fun <T> HttpURLConnection.use(block: (HttpURLConnection) -> T): T =
        try {
            block(this)
        } finally {
            disconnect()
        }

    private companion object {
        const val TAG = "ClaudeBridge"
        const val POLL_INTERVAL_MS = 20_000L
    }
}
