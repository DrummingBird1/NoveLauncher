package com.ailauncher.app.ui.screens

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.ailauncher.app.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ailauncher.app.data.update.UpdateChecker
import com.ailauncher.app.domain.calendar.JewishDayInfo
import com.ailauncher.app.ui.findLauncherActivity
import com.ailauncher.app.ui.launchApp
import com.ailauncher.app.ui.widgets.WidgetHostController
import androidx.core.graphics.drawable.toBitmap
import com.ailauncher.app.domain.models.*
import com.ailauncher.app.ui.LauncherViewModel
import com.ailauncher.app.ui.LocalAppWidgetHost
import com.ailauncher.app.ui.theme.launcherFontFamily
import com.ailauncher.app.ui.theme.parseHexColor
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    gridColumns: Int,
    onSwipeUp: (() -> Unit)? = null,
    onSwipeDown: (() -> Unit)? = null,
    onDoubleTap: (() -> Unit)? = null,
    onOpenSearch: () -> Unit = {}
) {
    val appState by viewModel.appState.collectAsState()
    val appearance by viewModel.appearance.collectAsState()
    val widgetSlots by viewModel.widgets.collectAsState()
    val security by viewModel.security.collectAsState()
    val pages by viewModel.pagesSettings.collectAsState()
    val shabbatActive by viewModel.shabbatModeActive.collectAsStateWithLifecycle()
    val jewishDay by viewModel.jewishDay.collectAsStateWithLifecycle()
    val update by viewModel.update.collectAsState()
    val context = LocalContext.current
    // v10: Shabbat mode steps social/entertainment/games/shopping aside.
    val topApps = remember(appState.rankedApps, shabbatActive) { viewModel.homeSuggestions(appState.rankedApps, shabbatActive) }

    // v8: Bottom dock from PageLayoutSettings.dockApps for the home page.
    val homeLayout = pages.pageLayouts["home"] ?: com.ailauncher.app.domain.models.PageLayoutSettings()
    val dockPackages = homeLayout.dockApps
    val dockApps = remember(appState.rankedApps, dockPackages) {
        dockPackages.mapNotNull { pkg ->
            appState.rankedApps.firstOrNull { it.app.packageName == pkg && it.app.profile == AppProfile.PERSONAL }
        }
    }

    // Clock editing mode
    var showClockSettings by remember { mutableStateOf(false) }
    // v8: Long-press menu target
    var longPressTarget by remember { mutableStateOf<RankedApp?>(null) }

    // v9: honour the SmartControl haptic setting (previously read but never applied).
    // Fire one LongPress haptic when the long-press menu opens.
    val smartControl by viewModel.smartControl.collectAsState()
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(longPressTarget) {
        if (longPressTarget != null && smartControl.hapticFeedback) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // v9.4 FIX: pointerInput must be keyed by something stable. Keying it by a
    // callback lambda (a fresh closure on every recomposition) cancels and
    // restarts the gesture-detector coroutine mid-touch, which misfired as a
    // phantom swipe-up. Key on Unit and read the latest callbacks via
    // rememberUpdatedState instead.
    val latestOnSwipeUp = rememberUpdatedState(onSwipeUp)
    val latestOnSwipeDown = rememberUpdatedState(onSwipeDown)
    val latestOnDoubleTap = rememberUpdatedState(onDoubleTap)
    val gridState = rememberLazyGridState()
    Column(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns),
            state = gridState,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .statusBarsPadding()
                .testTag("home_grid")
                .pointerInput(Unit) {
                    // v10: home swipes are read from the raw pointer stream in the
                    // Initial pass, without consuming anything. A regular drag
                    // detector never saw them: on Android 12+ the grid's stretch
                    // overscroll consumes the drag first. Swipe down only counts when
                    // the grid is at its top, swipe up when it can't scroll further,
                    // so normal scrolling of a long home screen still works.
                    val threshold = 48.dp.toPx()
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val atTop = !gridState.canScrollBackward
                        val atBottom = !gridState.canScrollForward
                        var dx = 0f
                        var dy = 0f
                        var fired = false
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            dx += change.position.x - change.previousPosition.x
                            dy += change.position.y - change.previousPosition.y
                            if (!fired && kotlin.math.abs(dy) > threshold && kotlin.math.abs(dy) > 1.5f * kotlin.math.abs(dx)) {
                                fired = true
                                if (dy > 0 && atTop) latestOnSwipeDown.value?.invoke()
                                else if (dy < 0 && atBottom) latestOnSwipeUp.value?.invoke()
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    // Empty-space double tap. Icons consume their own taps first.
                    detectTapGestures(onDoubleTap = { latestOnDoubleTap.value?.invoke() })
                }
        ) {
        // Clock — clickable to edit
        item(span = { GridItemSpan(maxLineSpan) }) {
            val weather by viewModel.weather.collectAsState()
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                ClockHeader(appearance.clock, weather, onClick = { showClockSettings = true })
                JewishCalendarLines(jewishDay, appearance.jewishCalendar, shabbatActive, appearance.clock)
            }
        }

        // Toolbar
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                // v8: Global search button — fast access to apps + contacts + web.
                IconButton(onClick = onOpenSearch, modifier = Modifier.testTag("home_search")) {
                    Icon(Icons.Rounded.Search, stringResource(R.string.action_search), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) }) {
                    Icon(Icons.Rounded.Settings, stringResource(R.string.action_settings), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!security.isLayoutLocked) {
                    IconButton(onClick = { context.findLauncherActivity()?.launchWidgetPicker() }) {
                        Icon(Icons.Rounded.Widgets, stringResource(R.string.action_widgets), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // v10: opt-in update check found a newer release.
        update?.let { info ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                UpdateBanner(info, onOpen = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(info.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (_: Exception) {}
                }, onDismiss = { viewModel.dismissUpdate() })
            }
        }

        // Widgets — keyed by widgetId so scrolling preserves widget state.
        items(
            items = widgetSlots,
            key = { it.widgetId },
            span = { GridItemSpan(maxLineSpan) }
        ) { slot ->
            WidgetView(
                widgetId = slot.widgetId,
                heightDp = (slot.spanY * WidgetHostController.ROW_HEIGHT_DP).toInt(),
                onRemove = if (!security.isLayoutLocked) { { viewModel.removeWidget(slot.widgetId) } } else null,
                onResize = if (!security.isLayoutLocked) {
                    { viewModel.resizeWidget(slot.widgetId, slot.spanX, if (slot.spanY >= 4) 1 else slot.spanY + 1) }
                } else null
            )
        }

        // Section
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(stringResource(R.string.recommended_now), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }

        // Top apps
        items(topApps, key = { it.app.key }) { rankedApp ->
            HomeAppItem(
                rankedApp,
                appearance.iconSizeDp,
                appearance.fontSizeSp,
                appearance.iconShape,
                appearance.appFont,
                onClick = {
                    // v10: a tap here is the "this suggestion was right" signal.
                    viewModel.recordSuggestionTap(rankedApp.app.packageName)
                    launchApp(context, rankedApp.app)
                },
                onLongClick = { longPressTarget = rankedApp }
            )
        }
        }  // LazyVerticalGrid

        // v8: Bottom dock — up to 5 pinned apps from PageLayoutSettings.dockApps.
        if (dockApps.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                dockApps.take(5).forEach { rankedApp ->
                    HomeAppItem(
                        rankedApp = rankedApp,
                        iconSizeDp = appearance.iconSizeDp,
                        fontSizeSp = appearance.fontSizeSp,
                        iconShape = appearance.iconShape,
                        font = appearance.appFont,
                        onClick = { launchApp(context, rankedApp.app) },
                        onLongClick = { longPressTarget = rankedApp }
                    )
                }
            }
        }
    }  // Column

    // v8: Long-press menu — shortcuts, hide, lock, app info, uninstall.
    longPressTarget?.let { target ->
        AppActionsSheet(target = target, viewModel = viewModel, onDismiss = { longPressTarget = null })
    }

    // Quick clock settings dialog
    if (showClockSettings) {
        val clock = appearance.clock
        AlertDialog(
            onDismissRequest = { showClockSettings = false },
            title = { Text(stringResource(R.string.clock_settings_title)) },
            text = {
                Column {
                    SliderSetting(stringResource(R.string.label_size), clock.fontSize.toFloat(), 24f..120f, "${clock.fontSize}") { newSize ->
                        viewModel.updateAppearance { a -> a.copy(clock = a.clock.copy(fontSize = newSize.toInt())) }
                    }
                    SwitchSetting(stringResource(R.string.clock_24_hour), clock.use24Hour) { v ->
                        viewModel.updateAppearance { a -> a.copy(clock = a.clock.copy(use24Hour = v)) }
                    }
                    SwitchSetting(stringResource(R.string.clock_show_date), clock.showDate) { v ->
                        viewModel.updateAppearance { a -> a.copy(clock = a.clock.copy(showDate = v)) }
                    }
                    SwitchSetting(stringResource(R.string.jc_show_hebrew_date), appearance.jewishCalendar.showHebrewDate) { v ->
                        viewModel.updateAppearance { a -> a.copy(jewishCalendar = a.jewishCalendar.copy(showHebrewDate = v)) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showClockSettings = false }) { Text(stringResource(R.string.action_close)) } }
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
    clock: ClockSettings
) {
    if (day == null) return
    val context = LocalContext.current
    val color = parseHexColor(clock.fontColor).copy(alpha = clock.opacity * 0.8f)
    val font = launcherFontFamily(clock.font)
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val timeFormat = remember(clock.use24Hour, locale) { calendarTimeFormat(clock.use24Hour, locale) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 8.dp)) {
        if (shabbatActive) {
            val today = java.time.LocalDate.now().dayOfWeek
            val isShabbat = today == java.time.DayOfWeek.SATURDAY || today == java.time.DayOfWeek.FRIDAY
            CalendarLine(stringResource(if (isShabbat) R.string.jc_shabbat_shalom else R.string.jc_chag_sameach), MaterialTheme.colorScheme.primary, font, emphasis = true)
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
                            if (days == 1) stringResource(R.string.jc_holiday_tomorrow, name)
                            else pluralStringResource(R.plurals.jc_holiday_in_days, days, name, days),
                            color, font
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
internal fun calendarTimeFormat(use24Hour: Boolean, locale: Locale) =
    SimpleDateFormat(if (use24Hour) "HH:mm" else "h:mm a", locale)

@Composable
private fun CalendarLine(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    font: androidx.compose.ui.text.font.FontFamily?,
    emphasis: Boolean = false
) {
    Text(
        text,
        fontSize = if (emphasis) 15.sp else 13.sp,
        color = color,
        fontFamily = font,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 2.dp)
    )
}

@Composable
private fun UpdateBanner(info: UpdateChecker.UpdateInfo, onOpen: () -> Unit, onDismiss: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    ) {
        Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.SystemUpdate, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.update_available, info.version), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), fontSize = 14.sp)
            TextButton(onClick = onOpen) { Text(stringResource(R.string.update_download)) }
            IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, stringResource(R.string.action_close), modifier = Modifier.size(18.dp)) }
        }
    }
}

@Composable
fun ClockHeader(
    clock: ClockSettings,
    weather: com.ailauncher.app.data.api.WeatherData? = null,
    onClick: () -> Unit
) {
    val time = remember { mutableStateOf("") }
    val date = remember { mutableStateOf("") }
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]

    LaunchedEffect(clock.use24Hour, clock.showSeconds, locale) {
        // Locale-aware date pattern: getBestDateTimePattern picks the right ordering
        // and connectors per locale (e.g. Hebrew "5 בינואר", English "January 5",
        // French "5 janvier").
        val datePattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM")
        while (true) {
            val now = Date()
            val timePattern = buildString {
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
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (clock.clockStyle == ClockStyle.ANALOG) {
            AnalogClock(
                sizeDp = clock.analogSize,
                handsColor = parseHexColor(clock.analogColor, 0xFFFFFFFF),
                accentColor = parseHexColor(clock.analogAccentColor, 0xFF7C7CFF)
            )
        } else {
            Text(
                text = time.value,
                fontSize = clock.fontSize.sp,
                color = parseHexColor(clock.fontColor).copy(alpha = clock.opacity),
                letterSpacing = (-2).sp,
                fontFamily = launcherFontFamily(clock.font)
            )
        }
        if (clock.showDate) {
            Text(
                text = date.value,
                fontSize = clock.dateFontSize.sp,
                color = parseHexColor(clock.fontColor).copy(alpha = clock.opacity * 0.7f),
                fontFamily = launcherFontFamily(clock.font)
            )
        }
        // v8: weather row beneath the clock when WeatherService has data.
        weather?.let { w ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${w.icon} ${kotlin.math.round(w.temperature).toInt()}° · ${stringResource(w.descriptionRes)}",
                fontSize = 14.sp,
                color = parseHexColor(clock.fontColor).copy(alpha = clock.opacity * 0.85f),
                fontFamily = launcherFontFamily(clock.font)
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
    accentColor: androidx.compose.ui.graphics.Color
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
            center = androidx.compose.ui.geometry.Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.02f)
        )
        // Hour ticks
        for (i in 0 until 12) {
            val angle = Math.toRadians((i * 30 - 90).toDouble())
            val sx = cx + kotlin.math.cos(angle).toFloat() * r * 0.85f
            val sy = cy + kotlin.math.sin(angle).toFloat() * r * 0.85f
            val ex = cx + kotlin.math.cos(angle).toFloat() * r * 0.95f
            val ey = cy + kotlin.math.sin(angle).toFloat() * r * 0.95f
            drawLine(handsColor.copy(alpha = 0.5f),
                androidx.compose.ui.geometry.Offset(sx, sy),
                androidx.compose.ui.geometry.Offset(ex, ey),
                strokeWidth = r * 0.02f)
        }
        // Hour hand
        val hours = cal.get(Calendar.HOUR) + cal.get(Calendar.MINUTE) / 60f
        val hAngle = Math.toRadians((hours * 30f - 90f).toDouble())
        drawLine(handsColor,
            androidx.compose.ui.geometry.Offset(cx, cy),
            androidx.compose.ui.geometry.Offset(
                cx + kotlin.math.cos(hAngle).toFloat() * r * 0.5f,
                cy + kotlin.math.sin(hAngle).toFloat() * r * 0.5f
            ),
            strokeWidth = r * 0.04f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round)
        // Minute hand
        val minutes = cal.get(Calendar.MINUTE) + cal.get(Calendar.SECOND) / 60f
        val mAngle = Math.toRadians((minutes * 6f - 90f).toDouble())
        drawLine(handsColor,
            androidx.compose.ui.geometry.Offset(cx, cy),
            androidx.compose.ui.geometry.Offset(
                cx + kotlin.math.cos(mAngle).toFloat() * r * 0.75f,
                cy + kotlin.math.sin(mAngle).toFloat() * r * 0.75f
            ),
            strokeWidth = r * 0.03f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round)
        // Second hand
        val seconds = cal.get(Calendar.SECOND).toFloat()
        val sAngle = Math.toRadians((seconds * 6f - 90f).toDouble())
        drawLine(accentColor,
            androidx.compose.ui.geometry.Offset(cx, cy),
            androidx.compose.ui.geometry.Offset(
                cx + kotlin.math.cos(sAngle).toFloat() * r * 0.85f,
                cy + kotlin.math.sin(sAngle).toFloat() * r * 0.85f
            ),
            strokeWidth = r * 0.012f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round)
        // Centre dot
        drawCircle(accentColor, radius = r * 0.05f, center = androidx.compose.ui.geometry.Offset(cx, cy))
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeAppItem(
    rankedApp: RankedApp,
    iconSizeDp: Int,
    fontSizeSp: Int,
    iconShape: IconShape,
    font: LauncherFont,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val shape = when (iconShape) {
        IconShape.CIRCLE -> CircleShape
        IconShape.SQUARE -> RoundedCornerShape(0.dp)
        IconShape.ROUNDED_SQUARE -> RoundedCornerShape(14.dp)
        IconShape.SQUIRCLE -> RoundedCornerShape(18.dp)
        IconShape.TEARDROP -> RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
        IconShape.HEXAGON -> RoundedCornerShape(8.dp)
        IconShape.DIAMOND -> RoundedCornerShape(8.dp)
        IconShape.SHIELD -> RoundedCornerShape(12.dp)
        IconShape.LEAF -> RoundedCornerShape(16.dp, 4.dp, 16.dp, 4.dp)
        IconShape.BLOB -> RoundedCornerShape(20.dp)
        IconShape.CLOVER -> RoundedCornerShape(12.dp)
        IconShape.OCTAGON -> RoundedCornerShape(10.dp)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(4.dp)
    ) {
        val icon = rankedApp.app.icon
        // v9: hand off to the process-lifetime IconCache so page swipes and
        // re-Composition don't re-rasterise the bitmap each time.
        val bitmap = icon?.let { drw ->
            com.ailauncher.app.ui.LocalIconCache.current.getOrLoad(rankedApp.app.key) { drw }
        }
        Box(Modifier.size(iconSizeDp.dp)) {
            if (bitmap != null) {
                Image(bitmap = bitmap, contentDescription = rankedApp.app.label, modifier = Modifier.size(iconSizeDp.dp).clip(shape))
            } else {
                Box(Modifier.size(iconSizeDp.dp).clip(shape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Text(rankedApp.app.label.take(1), fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
            // v9.1: notification badge — GetRankedAppsUseCase already zeroes this
            // out when badges are off or snoozed, so a plain count>0 check is enough.
            if (rankedApp.notificationCount > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (rankedApp.notificationCount > 9) "9+" else rankedApp.notificationCount.toString(),
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(rankedApp.app.label, fontSize = fontSizeSp.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, fontFamily = launcherFontFamily(font), modifier = Modifier.width(72.dp))
        if (rankedApp.weightScore > 0.5f) {
            Spacer(Modifier.height(2.dp))
            Box(Modifier.width((rankedApp.weightScore * 32).dp).height(2.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = rankedApp.weightScore)))
        }
    }
}

/**
 * A hosted third-party widget. v10: reports its real on-screen size to the
 * widget (AppWidgetHostView.updateAppWidgetSize), so providers pick the right
 * responsive layout instead of guessing, and can be cycled through four heights.
 */
@Composable
fun WidgetView(widgetId: Int, heightDp: Int, onRemove: (() -> Unit)?, onResize: (() -> Unit)? = null) {
    // v8 FIX: key the AndroidView on widgetId so scrolling in/out of the viewport
    // reuses the same hostView. Without the key, LazyGrid recomposition rebuilds
    // the widget view on every scroll, blanking third-party widgets.
    val host = LocalAppWidgetHost.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    var sizeDp by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    Box(Modifier.fillMaxWidth()) {
        androidx.compose.runtime.key(widgetId) {
            AndroidView(
                factory = { ctx ->
                    val mgr = AppWidgetManager.getInstance(ctx)
                    val info = mgr.getAppWidgetInfo(widgetId)
                    if (info != null) host.createView(ctx, widgetId, info).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                    } else FrameLayout(ctx)
                },
                update = { view ->
                    val (w, h) = sizeDp ?: return@AndroidView
                    val hostView = view as? android.appwidget.AppWidgetHostView ?: return@AndroidView
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            hostView.updateAppWidgetSize(android.os.Bundle(), listOf(android.util.SizeF(w.toFloat(), h.toFloat())))
                        } else {
                            @Suppress("DEPRECATION")
                            hostView.updateAppWidgetSize(null, w, h, w, h)
                        }
                    } catch (_: Exception) {}
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightDp.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .onSizeChanged { px ->
                        val dp = with(density) { px.width.toDp().value.toInt() to px.height.toDp().value.toInt() }
                        if (dp != sizeDp) sizeDp = dp
                    }
            )
        }
        Row(Modifier.align(Alignment.TopEnd)) {
            onResize?.let {
                IconButton(onClick = it) {
                    Icon(Icons.Rounded.Height, stringResource(R.string.action_resize_widget), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
            onRemove?.let {
                IconButton(onClick = it) {
                    Icon(Icons.Rounded.Close, stringResource(R.string.action_remove), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
