package com.nexus.launcher.integration.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume

/** Current conditions for the Home date line. */
@Immutable
data class WeatherNow(
    val temperature: Int,
    val symbol: String,
    val description: String,
) {
    /** What the date line shows: "☀️ 93°". */
    val short: String get() = "$symbol $temperature°"
}

/**
 * Current conditions from Open-Meteo, which needs no API key and no account —
 * the launcher ships no secret and the user registers nothing.
 *
 * Position is whatever fix the system already has; only when there is none at
 * all does it ask for one, and then a single coarse one, never a stream.
 */
class WeatherSource(private val context: Context) {

    private val _state = MutableStateFlow<WeatherNow?>(null)
    val state: StateFlow<WeatherNow?> = _state.asStateFlow()

    /** Plain-language account of the last attempt, for the settings row. */
    private val _status = MutableStateFlow("Off")
    val status: StateFlow<String> = _status.asStateFlow()

    private var lastFetchAt = 0L

    val hasLocationPermission: Boolean
        get() = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Fetches if the cached reading has gone stale. [force] skips that check —
     * used when the user has just granted location and expects to see something.
     */
    suspend fun refresh(force: Boolean = false) {
        if (!hasLocationPermission) {
            _state.value = null
            _status.value = "Location permission not granted"
            return
        }
        val now = System.currentTimeMillis()
        if (!force && _state.value != null && now - lastFetchAt < REFRESH_INTERVAL_MS) return

        val location = resolveLocation()
        if (location == null) {
            _status.value = "No position available yet — open a maps app once"
            return
        }

        val reading = withContext(Dispatchers.IO) {
            fetch(location.latitude, location.longitude)
        }
        if (reading == null) {
            _status.value = "Could not reach the weather service"
            return
        }

        lastFetchAt = now
        _state.value = reading
        _status.value = "${reading.description} · ${reading.temperature}°"
    }

    /** Marks the line as switched off, so the settings row stops explaining itself. */
    fun markDisabled() {
        _status.value = "Off"
    }

    private suspend fun resolveLocation(): Location? {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        // Network first: it is the coarse provider, it is usually warm, and it
        // does not wake the GPS.
        val providers = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
            LocationManager.GPS_PROVIDER,
        ).filter { provider -> runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false) }

        val cached = providers.asSequence()
            .mapNotNull { provider ->
                runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
            }
            .maxByOrNull { it.time }
        if (cached != null) return cached

        // Nothing cached — a phone that has not used location in a while. Ask
        // for one fix, coarse, with a deadline, and give up quietly if it does
        // not arrive.
        val provider = providers.firstOrNull() ?: return null
        return withTimeoutOrNull(CURRENT_FIX_TIMEOUT_MS) {
            suspendCancellableCoroutine<Location?> { continuation ->
                val signal = CancellationSignal()
                continuation.invokeOnCancellation { runCatching { signal.cancel() } }
                runCatching {
                    manager.getCurrentLocation(
                        provider,
                        signal,
                        ContextCompat.getMainExecutor(context),
                    ) { location ->
                        if (continuation.isActive) continuation.resume(location)
                    }
                }.onFailure {
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        }
    }

    private fun fetch(latitude: Double, longitude: Double): WeatherNow? {
        val unit = if (usesFahrenheit()) "fahrenheit" else "celsius"
        val url = URL(
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=${"%.3f".format(Locale.US, latitude)}" +
                "&longitude=${"%.3f".format(Locale.US, longitude)}" +
                "&current=temperature_2m,weather_code" +
                "&temperature_unit=$unit",
        )

        val body = runCatching {
            (url.openConnection() as HttpURLConnection).run {
                connectTimeout = 8_000
                readTimeout = 8_000
                requestMethod = "GET"
                try {
                    if (responseCode != HttpURLConnection.HTTP_OK) return@run null
                    inputStream.bufferedReader().use { it.readText() }
                } finally {
                    disconnect()
                }
            }
        }.getOrNull() ?: return null

        return runCatching {
            val current = JSONObject(body).getJSONObject("current")
            val code = current.getInt("weather_code")
            WeatherNow(
                temperature = Math.round(current.getDouble("temperature_2m")).toInt(),
                symbol = symbolFor(code),
                description = describe(code),
            )
        }.getOrNull()
    }

    /** The three countries that still read temperatures in Fahrenheit. */
    private fun usesFahrenheit(): Boolean =
        Locale.getDefault().country in setOf("US", "LR", "MM")

    private companion object {
        const val REFRESH_INTERVAL_MS = 30 * 60 * 1000L
        const val CURRENT_FIX_TIMEOUT_MS = 12_000L

        /** WMO weather codes, as Open-Meteo reports them. */
        fun symbolFor(code: Int): String = when (code) {
            0 -> "☀️"
            1, 2 -> "🌤️"
            3 -> "☁️"
            45, 48 -> "🌫️"
            in 51..57 -> "🌦️"
            in 61..67 -> "🌧️"
            in 71..77 -> "🌨️"
            in 80..82 -> "🌦️"
            85, 86 -> "🌨️"
            in 95..99 -> "⛈️"
            else -> "🌡️"
        }

        fun describe(code: Int): String = when (code) {
            0 -> "Clear"
            1, 2 -> "Partly cloudy"
            3 -> "Overcast"
            45, 48 -> "Fog"
            in 51..57 -> "Drizzle"
            in 61..67 -> "Rain"
            in 71..77 -> "Snow"
            in 80..82 -> "Showers"
            85, 86 -> "Snow showers"
            in 95..99 -> "Thunderstorms"
            else -> "Weather"
        }
    }
}
