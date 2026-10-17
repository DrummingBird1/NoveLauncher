package com.ailauncher.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ailauncher.app.R
import com.ailauncher.app.domain.calendar.JewishDayInfo
import com.ailauncher.app.domain.models.ClockSettings
import com.ailauncher.app.domain.models.ClockStyle
import com.ailauncher.app.domain.models.JewishCalendarSettings
import com.ailauncher.app.ui.theme.launcherFontFamily
import com.ailauncher.app.ui.theme.parseHexColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// v10: the clock block above the home grid, split out of HomeScreen.kt.

@Composable
fun ClockHeader(
    clock: ClockSettings,
    weather: com.ailauncher.app.data.api.WeatherData? = null,
    onClick: () -> Unit,
) {
    val time = remember { mutableStateOf("") }
    val date = remember { mutableStateOf("") }
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]

    LaunchedEffect(clock.use24Hour, clock.showSeconds, locale) {
        // Locale-aware date pattern: getBestDateTimePattern picks the right ordering
        // and connectors per locale (e.g. Hebrew "5 בינואר", English "January 5",
        // French "5 janvier").
        val datePattern =
            android.text.format.DateFormat
                .getBestDateTimePattern(locale, "EEEEdMMMM")
        while (true) {
            val now = Date()
            val timePattern =
                buildString {
                    append(if (clock.use24Hour) "HH:mm" else "hh:mm a")
                    if (clock.showSeconds) append(":ss")
                }
            time.value = SimpleDateFormat(timePattern, locale).format(now)
            date.value = SimpleDateFormat(datePattern, locale).format(now)
            kotlinx.coroutines.delay(if (clock.showSeconds) 1000L else 30_000L)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp, bottom = 16.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (clock.clockStyle == ClockStyle.ANALOG) {
            AnalogClock(
                sizeDp = clock.analogSize,
                handsColor = parseHexColor(clock.analogColor, 0xFFFFFFFF),
                accentColor = parseHexColor(clock.analogAccentColor, 0xFF7C7CFF),
            )
        } else {
            Text(
                text = time.value,
                fontSize = clock.fontSize.sp,
                color = parseHexColor(clock.fontColor).copy(alpha = clock.opacity),
                letterSpacing = (-2).sp,
                fontFamily = launcherFontFamily(clock.font),
            )
        }
        if (clock.showDate) {
            Text(
                text = date.value,
                fontSize = clock.dateFontSize.sp,
                color = parseHexColor(clock.fontColor).copy(alpha = clock.opacity * 0.7f),
                fontFamily = launcherFontFamily(clock.font),
            )
        }
        // v8: weather row beneath the clock when WeatherService has data.
        weather?.let { w ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${w.icon} ${kotlin.math.round(w.temperature).toInt()}° · ${stringResource(w.descriptionRes)}",
                fontSize = 14.sp,
                color = parseHexColor(clock.fontColor).copy(alpha = clock.opacity * 0.85f),
                fontFamily = launcherFontFamily(clock.font),
            )
        }
    }
}

/**
 * v8: minimal analog clock used when [ClockSettings.clockStyle] == ANALOG.
 * Updates once per minute. Hour/minute hands use [handsColor]; second hand
 * (and centre dot) use [accentColor].
 */
@Composable
fun AnalogClock(
    sizeDp: Int,
    handsColor: androidx.compose.ui.graphics.Color,
    accentColor: androidx.compose.ui.graphics.Color,
) {
    val currentTime = remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime.value = Date()
            kotlinx.coroutines.delay(1000L)
        }
    }
    val cal = remember(currentTime.value) { Calendar.getInstance().apply { time = currentTime.value } }
    androidx.compose.foundation.Canvas(modifier = Modifier.size(sizeDp.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = kotlin.math.min(cx, cy)
        // Dial outline
        drawCircle(
            color = handsColor.copy(alpha = 0.25f),
            radius = r * 0.97f,
            center =
                androidx.compose.ui.geometry
                    .Offset(cx, cy),
            style =
                androidx.compose.ui.graphics.drawscope
                    .Stroke(width = r * 0.02f),
        )
        // Hour ticks
        for (i in 0 until 12) {
            val angle = Math.toRadians((i * 30 - 90).toDouble())
            val sx = cx + kotlin.math.cos(angle).toFloat() * r * 0.85f
            val sy = cy + kotlin.math.sin(angle).toFloat() * r * 0.85f
            val ex = cx + kotlin.math.cos(angle).toFloat() * r * 0.95f
            val ey = cy + kotlin.math.sin(angle).toFloat() * r * 0.95f
            drawLine(
                handsColor.copy(alpha = 0.5f),
                androidx.compose.ui.geometry
                    .Offset(sx, sy),
                androidx.compose.ui.geometry
                    .Offset(ex, ey),
                strokeWidth = r * 0.02f,
            )
        }
        // Hour hand
        val hours = cal.get(Calendar.HOUR) + cal.get(Calendar.MINUTE) / 60f
        val hAngle = Math.toRadians((hours * 30f - 90f).toDouble())
        drawLine(
            handsColor,
            androidx.compose.ui.geometry
                .Offset(cx, cy),
            androidx.compose.ui.geometry.Offset(
                cx + kotlin.math.cos(hAngle).toFloat() * r * 0.5f,
                cy + kotlin.math.sin(hAngle).toFloat() * r * 0.5f,
            ),
            strokeWidth = r * 0.04f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        // Minute hand
        val minutes = cal.get(Calendar.MINUTE) + cal.get(Calendar.SECOND) / 60f
        val mAngle = Math.toRadians((minutes * 6f - 90f).toDouble())
        drawLine(
            handsColor,
            androidx.compose.ui.geometry
                .Offset(cx, cy),
            androidx.compose.ui.geometry.Offset(
                cx + kotlin.math.cos(mAngle).toFloat() * r * 0.75f,
                cy + kotlin.math.sin(mAngle).toFloat() * r * 0.75f,
            ),
            strokeWidth = r * 0.03f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        // Second hand
        val seconds = cal.get(Calendar.SECOND).toFloat()
        val sAngle = Math.toRadians((seconds * 6f - 90f).toDouble())
        drawLine(
            accentColor,
            androidx.compose.ui.geometry
                .Offset(cx, cy),
            androidx.compose.ui.geometry.Offset(
                cx + kotlin.math.cos(sAngle).toFloat() * r * 0.85f,
                cy + kotlin.math.sin(sAngle).toFloat() * r * 0.85f,
            ),
            strokeWidth = r * 0.012f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        // Centre dot
        drawCircle(
            accentColor,
            radius = r * 0.05f,
            center =
                androidx.compose.ui.geometry
                    .Offset(cx, cy),
        )
    }
}

/**
 * v10: Hebrew date, today's holiday or the next one, candle-lighting /
 * Shabbat-ends times and the Shabbat-mode greeting — each line opt-in from
 * Settings → Jewish calendar. Styled like the date line under the clock.
 */
@Composable
fun JewishCalendarLines(
    day: JewishDayInfo?,
    settings: JewishCalendarSettings,
    shabbatActive: Boolean,
    clock: ClockSettings,
) {
    if (day == null) return
    val context = LocalContext.current
    val color = parseHexColor(clock.fontColor).copy(alpha = clock.opacity * 0.8f)
    val font = launcherFontFamily(clock.font)
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val timeFormat = remember(clock.use24Hour, locale) { calendarTimeFormat(clock.use24Hour, locale) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 8.dp)) {
        if (shabbatActive) {
            val today =
                java.time.LocalDate
                    .now()
                    .dayOfWeek
            val isShabbat = today == java.time.DayOfWeek.SATURDAY || today == java.time.DayOfWeek.FRIDAY
            CalendarLine(
                stringResource(if (isShabbat) R.string.jc_shabbat_shalom else R.string.jc_chag_sameach),
                MaterialTheme.colorScheme.primary,
                font,
                emphasis = true,
            )
        }
        if (settings.showHebrewDate) CalendarLine(day.hebrewDate, color, font)
        if (settings.showHolidays) {
            if (day.holidaysToday.isNotEmpty()) {
                CalendarLine(day.holidaysToday.joinToString(" · ") { context.getString(it.nameRes) }, color, font)
            } else {
                day.nextHoliday?.let { (holiday, days) ->
                    if (days <= 30) {
                        val name = stringResource(holiday.nameRes)
                        CalendarLine(
                            if (days == 1) {
                                stringResource(R.string.jc_holiday_tomorrow, name)
                            } else {
                                pluralStringResource(R.plurals.jc_holiday_in_days, days, name, days)
                            },
                            color,
                            font,
                        )
                    }
                }
            }
        }
        if (settings.showShabbatTimes) {
            day.candleLighting?.let { CalendarLine(stringResource(R.string.jc_candle_lighting, timeFormat.format(Date(it))), color, font) }
            day.havdalah?.let { CalendarLine(stringResource(R.string.jc_havdalah, timeFormat.format(Date(it))), color, font) }
        }
    }
}

/**
 * Candle-lighting / Shabbat-end times in the launcher clock's own 12/24-hour
 * setting, not the system's — they sit right under that clock.
 */
internal fun calendarTimeFormat(
    use24Hour: Boolean,
    locale: Locale,
) = SimpleDateFormat(if (use24Hour) "HH:mm" else "h:mm a", locale)

@Composable
private fun CalendarLine(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    font: androidx.compose.ui.text.font.FontFamily?,
    emphasis: Boolean = false,
) {
    Text(
        text,
        fontSize = if (emphasis) 15.sp else 13.sp,
        color = color,
        fontFamily = font,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 2.dp),
    )
}
