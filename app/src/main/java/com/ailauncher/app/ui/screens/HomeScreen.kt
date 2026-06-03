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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ailauncher.app.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import com.ailauncher.app.domain.models.*
import com.ailauncher.app.ui.LauncherActivity
import com.ailauncher.app.ui.LauncherViewModel
import com.ailauncher.app.ui.LocalAppWidgetHost
import com.ailauncher.app.ui.theme.launcherFontFamily
import com.ailauncher.app.ui.theme.parseHexColor
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(viewModel: LauncherViewModel, gridColumns: Int, onSwipeUp: (() -> Unit)? = null) {
    val appState by viewModel.appState.collectAsState()
    val appearance by viewModel.appearance.collectAsState()
    val widgetSlots by viewModel.widgets.collectAsState()
    val security by viewModel.security.collectAsState()
    val pages by viewModel.pagesSettings.collectAsState()
    val context = LocalContext.current
    val topApps = appState.rankedApps.take(8)

    // v8: Bottom dock from PageLayoutSettings.dockApps for the home page.
    val homeLayout = pages.pageLayouts["home"] ?: com.ailauncher.app.domain.models.PageLayoutSettings()
    val dockPackages = homeLayout.dockApps
    val dockApps = remember(appState.rankedApps, dockPackages) {
        dockPackages.mapNotNull { pkg -> appState.rankedApps.firstOrNull { it.app.packageName == pkg } }
    }

    // Clock editing mode
    var showClockSettings by remember { mutableStateOf(false) }
    // v8: Global search overlay
    var showGlobalSearch by remember { mutableStateOf(false) }
    // v8: Long-press menu target
    var longPressTarget by remember { mutableStateOf<RankedApp?>(null) }

    Column(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .statusBarsPadding()
                .pointerInput(onSwipeUp) {
                    if (onSwipeUp != null) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount < -45f) onSwipeUp()
                        }
                    }
                }
        ) {
        // Clock — clickable to edit
        item(span = { GridItemSpan(maxLineSpan) }) {
            val weather by viewModel.weather.collectAsState()
            ClockHeader(appearance.clock, weather, onClick = { showClockSettings = true })
        }

        // Toolbar
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                // v8: Global search button — fast access to apps + contacts + web.
                IconButton(onClick = { showGlobalSearch = true }) {
                    Icon(Icons.Rounded.Search, stringResource(R.string.action_search), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) }) {
                    Icon(Icons.Rounded.Settings, stringResource(R.string.action_settings), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!security.isLayoutLocked) {
                    IconButton(onClick = { (context as? LauncherActivity)?.launchWidgetPicker() }) {
                        Icon(Icons.Rounded.Widgets, stringResource(R.string.action_widgets), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Widgets — keyed by widgetId so scrolling preserves widget state.
        items(
            items = widgetSlots,
            key = { it.widgetId },
            span = { GridItemSpan(maxLineSpan) }
        ) { slot ->
            WidgetView(
                slot.widgetId,
                slot.spanY * 80,
                if (!security.isLayoutLocked) { { viewModel.removeWidget(slot.widgetId) } } else null
            )
        }

        // Section
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(stringResource(R.string.recommended_now), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }

        // Top apps
        items(topApps, key = { it.app.packageName }) { rankedApp ->
            HomeAppItem(
                rankedApp,
                appearance.iconSizeDp,
                appearance.fontSizeSp,
                appearance.iconShape,
                appearance.appFont,
                onClick = { launchApp(context, rankedApp.app.packageName) },
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
                        onClick = { launchApp(context, rankedApp.app.packageName) },
                        onLongClick = { longPressTarget = rankedApp }
                    )
                }
            }
        }
    }  // Column

    // v8: Long-press menu — hide, lock, app info, uninstall.
    longPressTarget?.let { target ->
        AppActionsSheet(
            target = target,
            onDismiss = { longPressTarget = null },
            onHide = { viewModel.hideApp(target.app.packageName); longPressTarget = null },
            onPrivate = { viewModel.moveToPrivateFolder(target.app.packageName); longPressTarget = null },
            onLockToggle = { viewModel.toggleAppLock(target.app.packageName); longPressTarget = null },
            onAppInfo = {
                try {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(android.net.Uri.parse("package:${target.app.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (_: Exception) {}
                longPressTarget = null
            },
            onUninstall = {
                try {
                    context.startActivity(
                        Intent(Intent.ACTION_DELETE)
                            .setData(android.net.Uri.parse("package:${target.app.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (_: Exception) {}
                longPressTarget = null
            }
        )
    }

    // v8: Global search overlay
    if (showGlobalSearch) {
        GlobalSearchScreen(onDismiss = { showGlobalSearch = false })
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
                }
            },
            confirmButton = { TextButton(onClick = { showClockSettings = false }) { Text(stringResource(R.string.action_close)) } }
        )
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
            com.ailauncher.app.ui.LocalIconCache.current.getOrLoad(rankedApp.app.packageName) { drw }
        }
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = rankedApp.app.label, modifier = Modifier.size(iconSizeDp.dp).clip(shape))
        } else {
            Box(Modifier.size(iconSizeDp.dp).clip(shape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                Text(rankedApp.app.label.take(1), fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
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

@Composable
fun WidgetView(widgetId: Int, heightDp: Int, onRemove: (() -> Unit)?) {
    // v8 FIX: key the AndroidView on widgetId so scrolling in/out of the viewport
    // reuses the same hostView. Without the key, LazyGrid recomposition rebuilds
    // the widget view on every scroll, blanking third-party widgets.
    val host = LocalAppWidgetHost.current
    Box(Modifier.fillMaxWidth()) {
        androidx.compose.runtime.key(widgetId) {
            AndroidView(
                factory = { ctx ->
                    val mgr = AppWidgetManager.getInstance(ctx)
                    val info = mgr.getAppWidgetInfo(widgetId)
                    if (info != null) host.createView(ctx, widgetId, info).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        )
                    } else FrameLayout(ctx)
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = heightDp.dp).clip(RoundedCornerShape(16.dp))
            )
        }
        onRemove?.let {
            IconButton(onClick = it, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Rounded.Close, stringResource(R.string.action_remove), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/**
 * v8 FIX (critical security regression): every UI screen previously called
 * `launchApp(context, pkg)` which went straight to PackageManager, completely
 * bypassing AppLockManager. Locked apps could be opened from Home/Apps/Personal
 * with no PIN/biometric prompt — the entire CredentialDialog flow in
 * LauncherActivity was dead code.
 *
 * Now we walk the context chain looking for a LauncherActivity. If found, the
 * launch goes through `launchAppWithLockCheck` which branches on lock method.
 * Fallback (e.g. when called from SettingsActivity) is a direct launch.
 */
/**
 * v8: Bottom-sheet menu that appears on long-press of an app icon.
 * Provides hide, move-to-private, toggle lock, app info, uninstall.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionsSheet(
    target: RankedApp,
    onDismiss: () -> Unit,
    onHide: () -> Unit,
    onPrivate: () -> Unit,
    onLockToggle: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp)) {
            Text(
                target.app.label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            AppActionRow(Icons.Rounded.VisibilityOff, stringResource(R.string.action_hide_app), onHide)
            AppActionRow(Icons.Rounded.Shield, stringResource(R.string.action_move_private), onPrivate)
            AppActionRow(Icons.Rounded.Lock, stringResource(R.string.action_toggle_lock), onLockToggle)
            AppActionRow(Icons.Rounded.Info, stringResource(R.string.action_app_info), onAppInfo)
            if (!target.app.isSystemApp) {
                AppActionRow(Icons.Rounded.Delete, stringResource(R.string.action_uninstall), onUninstall)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AppActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurface)
    }
}

fun launchApp(context: Context, packageName: String) {
    val activity = generateSequence<Context>(context) {
        (it as? android.content.ContextWrapper)?.baseContext
    }.filterIsInstance<LauncherActivity>().firstOrNull()
    if (activity != null) {
        activity.launchAppWithLockCheck(packageName)
        return
    }
    context.packageManager.getLaunchIntentForPackage(packageName)?.let {
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(it)
    }
}
