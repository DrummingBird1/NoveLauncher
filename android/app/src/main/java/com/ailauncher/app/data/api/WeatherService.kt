package com.ailauncher.app.data.api

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.ailauncher.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * v7 FIX #2-3: Direct HTTP + kotlinx.serialization JsonObject parsing.
 * No Retrofit needed for a single endpoint. No need for Scalars converter.
 *
 * Open-Meteo API integration (free, no key required).
 *
 * v9: [descriptionRes] is a String resource id (not a localised String) so the same
 * cached/serialized WeatherData renders correctly after a locale change.
 */
@Serializable
data class WeatherData(
    val temperature: Double,
    val weatherCode: Int,
    val windSpeed: Double,
    val isDay: Int,
    @StringRes val descriptionRes: Int
) {
    val icon: String get() = weatherIconFromCode(weatherCode, isDay == 1)

    companion object {
        @StringRes
        fun descriptionResFromCode(code: Int): Int = when (code) {
            0 -> R.string.weather_clear
            1, 2, 3 -> R.string.weather_partly_cloudy
            45, 48 -> R.string.weather_fog
            in 51..57 -> R.string.weather_drizzle
            in 61..67 -> R.string.weather_rain
            in 71..77, 85, 86 -> R.string.weather_snow
            in 80..82 -> R.string.weather_showers
            in 95..99 -> R.string.weather_storm
            else -> R.string.weather_unknown
        }

        fun weatherIconFromCode(code: Int, isDay: Boolean): String = when (code) {
            0 -> if (isDay) "☀️" else "🌙"
            1, 2 -> if (isDay) "⛅" else "☁️"
            3 -> "☁️"
            45, 48 -> "🌫️"
            in 51..57 -> "🌦️"
            in 61..67 -> "🌧️"
            in 71..77, 85, 86 -> "❄️"
            in 80..82 -> "⛈️"
            in 95..99 -> "⛈️"
            else -> "🌡️"
        }
    }
}

@Singleton
class WeatherService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @SuppressLint("MissingPermission")
    suspend fun fetchWeather(lat: Double? = null, lon: Double? = null): WeatherData? = withContext(Dispatchers.IO) {
        val coords = if (lat != null && lon != null) lat to lon else getCurrentLocation() ?: (32.0853 to 34.7818)

        try {
            val urlStr = "https://api.open-meteo.com/v1/forecast?" +
                    "latitude=${coords.first}&longitude=${coords.second}" +
                    "&current=temperature_2m,weather_code,wind_speed_10m,is_day" +
                    "&timezone=auto"

            val conn = URL(urlStr).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                if (conn.responseCode !in 200..299) return@withContext null

                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val root = json.parseToJsonElement(body).jsonObject
                val current = root["current"]?.jsonObject ?: return@withContext null

                val temp = current["temperature_2m"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return@withContext null
                val code = current["weather_code"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val wind = current["wind_speed_10m"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                val isDay = current["is_day"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1

                WeatherData(
                    temperature = temp,
                    weatherCode = code,
                    windSpeed = wind,
                    isDay = isDay,
                    descriptionRes = WeatherData.descriptionResFromCode(code)
                )
            } finally {
                conn.disconnect()
            }
        } catch (_: Exception) { null }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation(): Pair<Double, Double>? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        for (p in providers) {
            try {
                val loc = lm.getLastKnownLocation(p) ?: continue
                return loc.latitude to loc.longitude
            } catch (_: SecurityException) { return null }
            catch (_: Exception) {}
        }
        return null
    }
}
