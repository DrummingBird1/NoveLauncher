package com.ailauncher.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ailauncher.app.R
import com.ailauncher.app.data.JewishCalendarService
import com.ailauncher.app.domain.calendar.JewishDayInfo
import com.ailauncher.app.domain.models.AppearanceSettings
import com.ailauncher.app.domain.models.JewishCalendarSettings
import com.ailauncher.app.ui.location.DefaultCityNote
import com.ailauncher.app.ui.location.rememberLocationPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * v10: Hebrew date, holidays, Shabbat times and Shabbat mode, with a live
 * preview of what the home screen will show today.
 */
@Composable
fun JewishCalendarSection(
    appearance: AppearanceSettings,
    service: JewishCalendarService,
    onUpdate: (AppearanceSettings) -> Unit,
) {
    val s = appearance.jewishCalendar

    fun update(n: JewishCalendarSettings) = onUpdate(appearance.copy(jewishCalendar = n))
    val context = LocalContext.current
    // Shabbat times and Shabbat mode follow the location; switching either on asks for it once.
    val location = rememberLocationPermission()
    val preview by produceState<JewishDayInfo?>(null, s, location.granted) {
        value = withContext(Dispatchers.Default) { service.dayInfo(s) }
    }
    val timeFormat = calendarTimeFormat(appearance.clock.use24Hour, context.resources.configuration.locales[0])

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.jc_preview_title), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    preview?.let { day ->
                        Text(day.hebrewDate, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                        if (day.holidaysToday.isNotEmpty()) {
                            Text(
                                day.holidaysToday.joinToString(" · ") { context.getString(it.nameRes) },
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        day.nextHoliday?.let { (h, days) ->
                            val name = context.getString(h.nameRes)
                            Text(
                                if (days == 1) {
                                    stringResource(R.string.jc_holiday_tomorrow, name)
                                } else {
                                    pluralStringResource(R.plurals.jc_holiday_in_days, days, name, days)
                                },
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        day.candleLighting?.let {
                            Text(
                                stringResource(R.string.jc_candle_lighting, timeFormat.format(Date(it))),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        day.havdalah?.let {
                            Text(
                                stringResource(R.string.jc_havdalah, timeFormat.format(Date(it))),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        item { SectionLabel(stringResource(R.string.jc_section_display)) }
        item { SwitchSetting(stringResource(R.string.jc_show_hebrew_date), s.showHebrewDate) { update(s.copy(showHebrewDate = it)) } }
        item { SwitchSetting(stringResource(R.string.jc_show_holidays), s.showHolidays) { update(s.copy(showHolidays = it)) } }
        item {
            SwitchSetting(stringResource(R.string.jc_show_shabbat_times), s.showShabbatTimes) {
                update(s.copy(showShabbatTimes = it))
                if (it) location.requestIfNeverAsked()
            }
        }
        item { SwitchSetting(stringResource(R.string.jc_diaspora), s.diaspora) { update(s.copy(diaspora = it)) } }

        item { SectionLabel(stringResource(R.string.jc_candle_minutes)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(18, 20, 30, 40).forEach { m ->
                    FilterChip(
                        selected = s.candleLightingMinutes == m,
                        onClick = { update(s.copy(candleLightingMinutes = m)) },
                        label = { Text("$m") },
                    )
                }
            }
        }

        item { SectionLabel(stringResource(R.string.jc_shabbat_mode)) }
        item {
            SwitchSetting(stringResource(R.string.jc_shabbat_mode), s.shabbatMode) {
                update(s.copy(shabbatMode = it))
                if (it) location.requestIfNeverAsked()
            }
        }
        item { Text(stringResource(R.string.jc_shabbat_mode_desc), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { Text(stringResource(R.string.jc_location_note), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { DefaultCityNote(location) }
    }
}
