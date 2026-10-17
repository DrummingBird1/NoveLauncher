package com.ailauncher.app.domain.calendar

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * v10: sunrise/sunset-type events from the classic "Almanac for Computers"
 * solar-position algorithm (the same one most zmanim calculators use). Pure
 * math, no network, accurate to about a minute at Israel's latitudes.
 *
 * [zenith] is the sun's angle from straight up at the moment of the event:
 * 90.833° is official sunset (centre of the disc 50' below the horizon, i.e.
 * accounting for refraction and the disc's radius); 98.5° is the sun 8.5°
 * below the horizon, the common definition of nightfall (tzeit hakochavim)
 * used for the end of Shabbat.
 */
object SolarCalculator {
    const val ZENITH_OFFICIAL = 90.833
    const val ZENITH_TZEIT = 98.5

    /** Epoch millis of sunset (or the given zenith crossing in the evening), null in polar day/night. */
    fun sunset(
        date: LocalDate,
        lat: Double,
        lon: Double,
        zone: ZoneId,
        zenith: Double = ZENITH_OFFICIAL,
    ): Long? = event(date, lat, lon, zone, zenith, rising = false)

    fun sunrise(
        date: LocalDate,
        lat: Double,
        lon: Double,
        zone: ZoneId,
        zenith: Double = ZENITH_OFFICIAL,
    ): Long? = event(date, lat, lon, zone, zenith, rising = true)

    private fun event(
        date: LocalDate,
        lat: Double,
        lon: Double,
        zone: ZoneId,
        zenith: Double,
        rising: Boolean,
    ): Long? {
        val n = date.dayOfYear
        val lngHour = lon / 15.0
        val t = n + ((if (rising) 6.0 else 18.0) - lngHour) / 24.0
        val m = 0.9856 * t - 3.289
        val l = norm360(m + 1.916 * sinD(m) + 0.020 * sinD(2 * m) + 282.634)
        var ra = norm360(Math.toDegrees(atan(0.91764 * tanD(l))))
        ra += floor(l / 90.0) * 90.0 - floor(ra / 90.0) * 90.0 // same quadrant as L
        ra /= 15.0
        val sinDec = 0.39782 * sinD(l)
        val cosDec = cos(asin(sinDec))
        val cosH = (cosD(zenith) - sinDec * sinD(lat)) / (cosDec * cosD(lat))
        if (cosH > 1 || cosH < -1) return null
        val h = (if (rising) 360.0 - Math.toDegrees(acos(cosH)) else Math.toDegrees(acos(cosH))) / 15.0
        val localMeanTime = h + ra - 0.06571 * t - 6.622
        val ut = ((localMeanTime - lngHour) % 24.0 + 24.0) % 24.0

        // UT is a time of day; attach it to the UTC calendar day, then shift by a
        // day if that lands on a neighbouring *local* date (far-east/west longitudes).
        val utcMidnight = date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        var millis = utcMidnight + (ut * 3_600_000).toLong()
        val local = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), zone).toLocalDate()
        if (local.isAfter(date)) millis -= DAY_MS
        if (local.isBefore(date)) millis += DAY_MS
        return millis
    }

    private const val DAY_MS = 86_400_000L

    private fun norm360(x: Double) = ((x % 360.0) + 360.0) % 360.0

    private fun sinD(d: Double) = sin(Math.toRadians(d))

    private fun cosD(d: Double) = cos(Math.toRadians(d))

    private fun tanD(d: Double) = tan(Math.toRadians(d))
}
