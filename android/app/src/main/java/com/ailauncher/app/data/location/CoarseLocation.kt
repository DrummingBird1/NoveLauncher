package com.ailauncher.app.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat

/**
 * v10: the last-known coarse location, shared by the weather line and the
 * Jewish-calendar times (extracted from WeatherService). Never requests a fix —
 * it only reads what the OS already has cached — and never leaves the device
 * from here; WeatherService is the only caller that sends coordinates anywhere.
 */
object CoarseLocation {
    /** Tel Aviv — used when location permission is denied or nothing is cached. */
    const val DEFAULT_LAT = 32.0853
    const val DEFAULT_LON = 34.7818

    @SuppressLint("MissingPermission")
    fun lastKnown(context: Context): Pair<Double, Double>? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        for (p in providers) {
            try {
                val loc = lm.getLastKnownLocation(p) ?: continue
                return loc.latitude to loc.longitude
            } catch (_: SecurityException) {
                return null
            } catch (_: Exception) {}
        }
        return null
    }

    fun lastKnownOrDefault(context: Context): Pair<Double, Double> =
        lastKnown(context) ?: (DEFAULT_LAT to DEFAULT_LON)
}
