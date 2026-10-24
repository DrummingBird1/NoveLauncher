package com.ailauncher.app.data

import android.content.Context
import com.ailauncher.app.AppLocales
import com.ailauncher.app.data.location.CoarseLocation
import com.ailauncher.app.domain.calendar.JewishCalendar
import com.ailauncher.app.domain.calendar.JewishDayInfo
import com.ailauncher.app.domain.models.JewishCalendarSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * v10: binds the pure [JewishCalendar] math to this device — time zone, app
 * locale and last-known coarse location (default city if location is denied).
 */
@Singleton
class JewishCalendarService
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun dayInfo(
            settings: JewishCalendarSettings,
            now: Long = System.currentTimeMillis(),
        ): JewishDayInfo {
            val (lat, lon) = CoarseLocation.lastKnownOrDefault(context)
            return JewishCalendar.dayInfo(
                nowMillis = now,
                zone = ZoneId.systemDefault(),
                lat = lat,
                lon = lon,
                locale = AppLocales.locale(context),
                candleLightingMinutes = settings.candleLightingMinutes,
                diaspora = settings.diaspora,
            )
        }

        /** True while Shabbat mode is on *and* it is currently Shabbat or Yom Tov. */
        fun isShabbatModeActive(
            settings: JewishCalendarSettings,
            now: Long = System.currentTimeMillis(),
        ): Boolean {
            if (!settings.shabbatMode) return false
            val (lat, lon) = CoarseLocation.lastKnownOrDefault(context)
            return JewishCalendar.isRestPeriodNow(now, ZoneId.systemDefault(), lat, lon, settings.candleLightingMinutes, settings.diaspora)
        }
    }
