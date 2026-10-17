package com.ailauncher.app.domain.calendar

import android.icu.text.DateFormat
import android.icu.util.HebrewCalendar
import android.icu.util.TimeZone
import android.icu.util.ULocale
import androidx.annotation.StringRes
import com.ailauncher.app.R
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * v10: Jewish holidays, by the Israeli schedule (one-day Yom Tov) unless
 * [JewishCalendar.holidaysOn]'s `diaspora` is set, plus Israel's national days
 * with their official date-shifting rules.
 */
enum class JewishHoliday(
    @StringRes val nameRes: Int,
    val isYomTov: Boolean = false,
) {
    ROSH_HASHANA(R.string.holiday_rosh_hashana, true),
    TZOM_GEDALIA(R.string.holiday_tzom_gedalia),
    YOM_KIPPUR(R.string.holiday_yom_kippur, true),
    SUKKOT(R.string.holiday_sukkot, true),
    CHOL_HAMOED_SUKKOT(R.string.holiday_chol_hamoed_sukkot),
    HOSHANA_RABBA(R.string.holiday_hoshana_rabba),
    SHEMINI_ATZERET(R.string.holiday_shemini_atzeret, true),
    SIMCHAT_TORAH(R.string.holiday_simchat_torah, true),
    HANUKKAH(R.string.holiday_hanukkah),
    TENTH_OF_TEVET(R.string.holiday_tenth_of_tevet),
    TU_BISHVAT(R.string.holiday_tu_bishvat),
    TAANIT_ESTHER(R.string.holiday_taanit_esther),
    PURIM(R.string.holiday_purim),
    SHUSHAN_PURIM(R.string.holiday_shushan_purim),
    PESACH(R.string.holiday_pesach, true),
    CHOL_HAMOED_PESACH(R.string.holiday_chol_hamoed_pesach),
    SHVII_SHEL_PESACH(R.string.holiday_shvii_shel_pesach, true),
    YOM_HASHOAH(R.string.holiday_yom_hashoah),
    YOM_HAZIKARON(R.string.holiday_yom_hazikaron),
    YOM_HAATZMAUT(R.string.holiday_yom_haatzmaut),
    LAG_BAOMER(R.string.holiday_lag_baomer),
    YOM_YERUSHALAYIM(R.string.holiday_yom_yerushalayim),
    SHAVUOT(R.string.holiday_shavuot, true),
    SHIVA_ASAR_BETAMMUZ(R.string.holiday_17_tammuz),
    TISHA_BAV(R.string.holiday_tisha_bav),
    TU_BAV(R.string.holiday_tu_bav),
}

/** Everything the home screen shows for "today", computed in one pass. */
data class JewishDayInfo(
    val hebrewDate: String,
    val holidaysToday: List<JewishHoliday>,
    /** Next holiday strictly after today, and how many days away; null if none in range. */
    val nextHoliday: Pair<JewishHoliday, Int>?,
    /** Set on the eve of Shabbat/Yom Tov. Epoch millis. */
    val candleLighting: Long?,
    /** Set on the last day of a Shabbat/Yom Tov run. Epoch millis. */
    val havdalah: Long?,
    /** Between candle lighting and nightfall of a Shabbat/Yom Tov. */
    val isRestPeriodNow: Boolean,
)

/**
 * v10: Hebrew dates and holidays via android.icu's HebrewCalendar (API 24+,
 * so no extra dependency), and Shabbat/Yom Tov times via [SolarCalculator].
 *
 * ICU month numbering: TISHRI=0 … ELUL=12, where 5 is Adar I and only exists
 * in leap years; 6 is "Adar" in a regular year and Adar II in a leap year —
 * which is exactly where Purim falls in both cases.
 */
object JewishCalendar {
    private val UTC: TimeZone = TimeZone.getTimeZone("UTC")

    data class HebrewDate(
        val year: Int,
        val month: Int,
        val day: Int,
    )

    fun hebrewDate(date: LocalDate): HebrewDate {
        val cal = HebrewCalendar(UTC)
        // Noon UTC: the civil (daytime) date, unaffected by the sunset day boundary.
        cal.timeInMillis =
            date
                .atTime(12, 0)
                .atZone(ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli()
        return HebrewDate(cal.get(HebrewCalendar.YEAR), cal.get(HebrewCalendar.MONTH), cal.get(HebrewCalendar.DAY_OF_MONTH))
    }

    fun toGregorian(
        year: Int,
        month: Int,
        day: Int,
    ): LocalDate {
        val cal = HebrewCalendar(UTC)
        cal.clear()
        cal.set(year, month, day, 12, 0)
        return Instant.ofEpochMilli(cal.timeInMillis).atZone(ZoneId.of("UTC")).toLocalDate()
    }

    fun isLeapYear(year: Int): Boolean = ((7 * year + 1) % 19) < 7

    /** Long-form Hebrew date in the given locale ("ט״ו בניסן תשפ״ו" / "15 Nisan 5786"). */
    fun format(
        date: LocalDate,
        locale: Locale,
    ): String {
        val uLocale = ULocale("${locale.language}@calendar=hebrew")
        val df = DateFormat.getDateInstance(DateFormat.LONG, uLocale)
        df.timeZone = UTC
        return df.format(
            java.util.Date(
                date
                    .atTime(12, 0)
                    .atZone(ZoneId.of("UTC"))
                    .toInstant()
                    .toEpochMilli(),
            ),
        )
    }

    fun holidaysOn(
        date: LocalDate,
        diaspora: Boolean = false,
    ): List<JewishHoliday> {
        val h = hebrewDate(date)
        val dow = date.dayOfWeek
        val out = mutableListOf<JewishHoliday>()
        val (m, d) = h.month to h.day
        when (m) {
            HebrewCalendar.TISHRI -> {
                if (d == 1 || d == 2) out += JewishHoliday.ROSH_HASHANA
                // Fast of Gedalia moves to Sunday when 3 Tishrei is Shabbat.
                if ((d == 3 && dow != DayOfWeek.SATURDAY) || (d == 4 && dow == DayOfWeek.SUNDAY)) out += JewishHoliday.TZOM_GEDALIA
                if (d == 10) out += JewishHoliday.YOM_KIPPUR
                if (d == 15 || (diaspora && d == 16)) out += JewishHoliday.SUKKOT
                if (d in (if (diaspora) 17 else 16)..20) out += JewishHoliday.CHOL_HAMOED_SUKKOT
                if (d == 21) out += JewishHoliday.HOSHANA_RABBA
                // In Israel Shemini Atzeret and Simchat Torah are the same day; abroad, two days.
                if (d == 22) out += JewishHoliday.SHEMINI_ATZERET
                if ((!diaspora && d == 22) || (diaspora && d == 23)) out += JewishHoliday.SIMCHAT_TORAH
            }

            HebrewCalendar.TEVET -> {
                if (d == 10) out += JewishHoliday.TENTH_OF_TEVET
            }

            HebrewCalendar.SHEVAT -> {
                if (d == 15) out += JewishHoliday.TU_BISHVAT
            }

            HebrewCalendar.ADAR -> {
                // Ta'anit Esther moves back to Thursday when 13 Adar is Shabbat.
                if ((d == 13 && dow != DayOfWeek.SATURDAY) || (d == 11 && dow == DayOfWeek.THURSDAY)) out += JewishHoliday.TAANIT_ESTHER
                if (d == 14) out += JewishHoliday.PURIM
                if (d == 15) out += JewishHoliday.SHUSHAN_PURIM
            }

            HebrewCalendar.NISAN -> {
                if (d == 15 || (diaspora && d == 16)) out += JewishHoliday.PESACH
                if (d in (if (diaspora) 17 else 16)..20) out += JewishHoliday.CHOL_HAMOED_PESACH
                if (d == 21 || (diaspora && d == 22)) out += JewishHoliday.SHVII_SHEL_PESACH
                if (isYomHaShoah(d, dow)) out += JewishHoliday.YOM_HASHOAH
            }

            HebrewCalendar.IYAR -> {
                val (zikaron, atzmaut) = memorialAndIndependenceDays(h.year)
                if (date == zikaron) out += JewishHoliday.YOM_HAZIKARON
                if (date == atzmaut) out += JewishHoliday.YOM_HAATZMAUT
                if (d == 18) out += JewishHoliday.LAG_BAOMER
                if (d == 28) out += JewishHoliday.YOM_YERUSHALAYIM
            }

            HebrewCalendar.SIVAN -> {
                if (d == 6 || (diaspora && d == 7)) out += JewishHoliday.SHAVUOT
            }

            HebrewCalendar.TAMUZ -> {
                if ((d == 17 && dow != DayOfWeek.SATURDAY) ||
                    (d == 18 && dow == DayOfWeek.SUNDAY)
                ) {
                    out += JewishHoliday.SHIVA_ASAR_BETAMMUZ
                }
            }

            HebrewCalendar.AV -> {
                if ((d == 9 && dow != DayOfWeek.SATURDAY) || (d == 10 && dow == DayOfWeek.SUNDAY)) out += JewishHoliday.TISHA_BAV
                if (d == 15) out += JewishHoliday.TU_BAV
            }
        }
        // Hanukkah: 8 days from 25 Kislev (Kislev has 29 or 30 days, so count days, not dates).
        val hanukkahStart = toGregorian(h.year, HebrewCalendar.KISLEV, 25) // Kislev and Tevet share a Hebrew year
        val sinceStart = ChronoUnit.DAYS.between(hanukkahStart, date)
        if (sinceStart in 0..7) out += JewishHoliday.HANUKKAH
        return out
    }

    /** 27 Nisan; moved to Thursday if it falls on Friday, to Monday if on Sunday. */
    private fun isYomHaShoah(
        day: Int,
        dow: DayOfWeek,
    ): Boolean =
        when (day) {
            27 -> dow != DayOfWeek.FRIDAY && dow != DayOfWeek.SUNDAY
            26 -> dow == DayOfWeek.THURSDAY
            28 -> dow == DayOfWeek.MONDAY
            else -> false
        }

    /**
     * Yom HaZikaron / Yom HaAtzma'ut, per the Knesset's rules: 5 Iyar on Friday
     * or Shabbat → Independence Day moves back to Thursday; 5 Iyar on Monday →
     * it moves forward to Tuesday. Memorial Day is always the day before.
     */
    fun memorialAndIndependenceDays(hebrewYear: Int): Pair<LocalDate, LocalDate> {
        val fifth = toGregorian(hebrewYear, HebrewCalendar.IYAR, 5)
        val atzmaut =
            when (fifth.dayOfWeek) {
                DayOfWeek.FRIDAY -> fifth.minusDays(1)
                DayOfWeek.SATURDAY -> fifth.minusDays(2)
                DayOfWeek.MONDAY -> fifth.plusDays(1)
                else -> fifth
            }
        return atzmaut.minusDays(1) to atzmaut
    }

    fun isRestDay(
        date: LocalDate,
        diaspora: Boolean = false,
    ): Boolean = date.dayOfWeek == DayOfWeek.SATURDAY || holidaysOn(date, diaspora).any { it.isYomTov }

    /** Between candle lighting and nightfall of a Shabbat or Yom Tov — the window Shabbat mode covers. */
    fun isRestPeriodNow(
        nowMillis: Long,
        zone: ZoneId,
        lat: Double,
        lon: Double,
        candleLightingMinutes: Int,
        diaspora: Boolean = false,
    ): Boolean {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        return restPeriod(
            nowMillis,
            isRestDay(today, diaspora),
            isRestDay(today.plusDays(1), diaspora),
            SolarCalculator.sunset(today, lat, lon, zone),
            SolarCalculator.sunset(today, lat, lon, zone, SolarCalculator.ZENITH_TZEIT),
            candleLightingMinutes,
        )
    }

    private fun restPeriod(
        now: Long,
        restToday: Boolean,
        restTomorrow: Boolean,
        sunset: Long?,
        tzeit: Long?,
        candleLightingMinutes: Int,
    ): Boolean =
        when {
            restToday && tzeit != null && now < tzeit -> true
            restTomorrow && sunset != null && now >= sunset - candleLightingMinutes * 60_000L -> true
            else -> false
        }

    fun dayInfo(
        nowMillis: Long,
        zone: ZoneId,
        lat: Double,
        lon: Double,
        locale: Locale,
        candleLightingMinutes: Int,
        diaspora: Boolean = false,
    ): JewishDayInfo {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val tomorrow = today.plusDays(1)
        val restToday = isRestDay(today, diaspora)
        val restTomorrow = isRestDay(tomorrow, diaspora)
        val sunset = SolarCalculator.sunset(today, lat, lon, zone)
        val tzeit = SolarCalculator.sunset(today, lat, lon, zone, SolarCalculator.ZENITH_TZEIT)
        val candle = if (restTomorrow && !restToday) sunset?.minus(candleLightingMinutes * 60_000L) else null
        val havdalah = if (restToday && !restTomorrow) tzeit else null

        val restNow = restPeriod(nowMillis, restToday, restTomorrow, sunset, tzeit, candleLightingMinutes)

        val next =
            (1..90)
                .asSequence()
                .map { offset -> offset to holidaysOn(today.plusDays(offset.toLong()), diaspora) }
                .firstOrNull { it.second.isNotEmpty() }
                ?.let { (offset, list) -> list.first() to offset }

        return JewishDayInfo(
            hebrewDate = format(today, locale),
            holidaysToday = holidaysOn(today, diaspora),
            nextHoliday = next,
            candleLighting = candle,
            havdalah = havdalah,
            isRestPeriodNow = restNow,
        )
    }
}
