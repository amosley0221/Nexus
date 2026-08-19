package com.nexus.launcher.integration.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

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
 * Position comes from the last location the system already has, never a fresh
 * fix: a launcher has no business turning on the GPS, and a coarse cell-tower
 * position from some minutes ago is exact enough to name the temperature.
 */
class WeatherSource(private val context: Context) {

    private val _state = MutableStateFlow<WeatherNow?>(null)
    val state: StateFlow<WeatherNow?> = _state.asStateFlow()

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
            return
        }
        val now = System.currentTimeMillis()
        if (!force && _state.value != null && now - lastFetchAt < REFRESH_INTERVAL_MS) return

        val reading = withContext(Dispatchers.IO) {
            val location = lastKnownLocation() ?: return@withContext null
            fetch(location.latitude, location.longitude)
        }
        if (reading != null) {
            lastFetchAt = now
            _state.value = reading
        }
    }

    private fun lastKnownLocation(): Location? {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null
        // Network first: it is the coarse provider, it is usually warm, and it
        // does not wake the GPS.
        val providers = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
            LocationManager.GPS_PROVIDER,
        )
        return providers.asSequence()
            .mapNotNull { provider ->
                runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
            }
            .maxByOrNull { it.time }
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
