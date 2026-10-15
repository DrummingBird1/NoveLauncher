package com.ailauncher.app.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.ailauncher.app.data.wallpaper.WallpaperStore
import com.ailauncher.app.domain.models.BuiltInWallpaper
import com.ailauncher.app.domain.models.PagesSettings
import kotlinx.coroutines.launch
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AppShortcut
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ailauncher.app.R
import com.ailauncher.app.data.iconpack.IconPackManager
import com.ailauncher.app.domain.models.AppearanceSettings
import com.ailauncher.app.domain.models.HiddenAppsSettings
import com.ailauncher.app.domain.models.NewsSettings
import com.ailauncher.app.domain.models.NewsSource
import com.ailauncher.app.domain.models.WallpaperEntry
import com.ailauncher.app.domain.models.WallpaperMode
import com.ailauncher.app.domain.models.WallpaperSettings

/**
 * v9: extracted from SettingsActivity.kt. Smaller SettingsPages bundled in one
 * file because each is short on its own: HIDDEN_APPS, WALLPAPER, NEWS_SOURCES,
 * ABOUT, ICON_PACKS.
 */

@Composable
fun NewsSourcesSection(settings: NewsSettings, onUpdate: (NewsSettings) -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SectionLabel(stringResource(R.string.section_choose_news_sources)) }
        items(NewsSource.ALL_SOURCES) { source ->
            val enabled = source.id in settings.enabledSourceIds
            Card(
                Modifier.fillMaxWidth().clickable {
                    val newIds = settings.enabledSourceIds.toMutableSet()
                    if (enabled) newIds.remove(source.id) else newIds.add(source.id)
                    onUpdate(settings.copy(enabledSourceIds = newIds))
                },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = enabled, onCheckedChange = null)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(source.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                        Text(source.language.uppercase(), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (settings.customSources.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.section_custom_news_sources)) }
            items(settings.customSources) { source ->
                val enabled = source.id in settings.enabledSourceIds
                Card(
                    Modifier.fillMaxWidth().clickable {
                        val newIds = settings.enabledSourceIds.toMutableSet()
                        if (enabled) newIds.remove(source.id) else newIds.add(source.id)
                        onUpdate(settings.copy(enabledSourceIds = newIds))
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = enabled, onCheckedChange = null)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(source.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                            Text(source.url, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = {
                            onUpdate(settings.copy(
                                customSources = settings.customSources.filter { it.id != source.id },
                                enabledSourceIds = settings.enabledSourceIds - source.id
                            ))
                        }) { Icon(Icons.Rounded.Close, stringResource(R.string.action_remove), modifier = Modifier.size(18.dp)) }
                    }
                }
            }
        }

        item {
            OutlinedButton(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.action_add_custom_news_source))
            }
        }

        item {
            SliderSetting(stringResource(R.string.news_max_articles), settings.maxArticles.toFloat(), 10f..100f, "${settings.maxArticles}", steps = 8) {
                onUpdate(settings.copy(maxArticles = it.toInt()))
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.dialog_add_news_source_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.dialog_news_source_name_label)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text(stringResource(R.string.dialog_news_source_url_label)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmedUrl = url.trim()
                    if (name.isNotBlank() && (trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://"))) {
                        val id = "custom_${System.currentTimeMillis()}"
                        val newSource = NewsSource(id, name.trim(), trimmedUrl, language = "he")
                        onUpdate(settings.copy(
                            customSources = settings.customSources + newSource,
                            enabledSourceIds = settings.enabledSourceIds + id
                        ))
                        showAddDialog = false
                    }
                }) { Text(stringResource(R.string.action_create)) }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}

@Composable
fun HiddenAppsSection(hidden: HiddenAppsSettings, onUpdate: (HiddenAppsSettings) -> Unit) {
    val context = LocalContext.current
    val apps = remember {
        val pm = context.packageManager
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply { addCategory(android.content.Intent.CATEGORY_LAUNCHER) }
        pm.queryIntentActivities(intent, 0).mapNotNull { ri ->
            val pkg = ri.activityInfo?.packageName ?: return@mapNotNull null
            if (pkg == context.packageName) return@mapNotNull null
            pkg to ri.loadLabel(pm).toString()
        }.sortedBy { it.second }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item { SectionLabel(stringResource(R.string.hidden_apps_count_label, hidden.hiddenPackages.size)) }
        item { Text(stringResource(R.string.hidden_apps_description), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }

        item { SectionLabel(stringResource(R.string.private_folder_count_label, hidden.privateFolderPackages.size)) }
        item { Text(stringResource(R.string.private_folder_description), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }

        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }

        items(apps) { (pkg, label) ->
            val isHidden = pkg in hidden.hiddenPackages
            val isPrivate = pkg in hidden.privateFolderPackages
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                    when {
                        isHidden -> Text(stringResource(R.string.label_hidden), fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        isPrivate -> Text(stringResource(R.string.label_private_folder), fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                }
                IconButton(onClick = {
                    val newHidden = hidden.hiddenPackages.toMutableSet()
                    if (isHidden) newHidden.remove(pkg) else { newHidden.add(pkg); }
                    onUpdate(hidden.copy(hiddenPackages = newHidden, privateFolderPackages = hidden.privateFolderPackages - pkg))
                }) {
                    Icon(
                        if (isHidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                        stringResource(R.string.action_hide),
                        tint = if (isHidden) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = {
                    val newPrivate = hidden.privateFolderPackages.toMutableSet()
                    if (isPrivate) newPrivate.remove(pkg) else { newPrivate.add(pkg); }
                    onUpdate(hidden.copy(privateFolderPackages = newPrivate, hiddenPackages = hidden.hiddenPackages - pkg))
                }) {
                    Icon(
                        Icons.Rounded.Lock,
                        stringResource(R.string.label_private_folder),
                        tint = if (isPrivate) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * v10: the wallpaper editor actually does something now. Images come from the
 * system Photo Picker (no storage permission) and are copied into app storage
 * (WallpaperStore); built-in gradients need no image at all. Each entry gets
 * a live thumbnail, the adjustments WallpaperLayer applies, and — depending on
 * the mode — the page, start hour or weekday it belongs to.
 */
@Composable
fun WallpaperSection(appearance: AppearanceSettings, pages: PagesSettings, onUpdate: (AppearanceSettings) -> Unit) {
    val wp = appearance.wallpaper
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    fun updateWp(w: WallpaperSettings) = onUpdate(appearance.copy(wallpaper = w))
    fun updateEntry(i: Int, e: WallpaperEntry) = updateWp(wp.copy(entries = wp.entries.toMutableList().also { it[i] = e }))
    var showBuiltIns by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    val importFailed = stringResource(R.string.wallpaper_import_failed)

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            importing = true
            val id = "wp_${System.currentTimeMillis()}"
            val path = WallpaperStore.import(context, uri, id)
            importing = false
            if (path != null) updateWp(wp.copy(entries = wp.entries + WallpaperEntry(id = id, uri = path)))
            else android.widget.Toast.makeText(context, importFailed, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionLabel(stringResource(R.string.section_wallpaper_mode)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                WallpaperMode.entries.forEach { mode ->
                    Card(
                        Modifier.fillMaxWidth().clickable { updateWp(wp.copy(mode = mode)) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (wp.mode == mode) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = wp.mode == mode, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(stringResource(mode.displayNameRes), color = MaterialTheme.colorScheme.onSurface)
                                val descRes = when (mode) {
                                    WallpaperMode.SINGLE -> R.string.wallpaper_desc_single
                                    WallpaperMode.PER_PAGE -> R.string.wallpaper_desc_per_page
                                    WallpaperMode.HOURLY -> R.string.wallpaper_desc_hourly
                                    WallpaperMode.DAILY -> R.string.wallpaper_desc_daily
                                    WallpaperMode.LIVE -> R.string.wallpaper_desc_live
                                }
                                Text(stringResource(descRes), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        if (wp.mode == WallpaperMode.LIVE) {
            item {
                Text(stringResource(R.string.wallpaper_live_hint), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Button(onClick = {
                    try {
                        context.startActivity(Intent(android.app.WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
                    } catch (_: Exception) {
                        try { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), null)) } catch (_: Exception) {}
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.wallpaper_live_choose)) }
            }
        } else {
            if (wp.entries.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.section_wallpaper_active)) }
                wp.entries.forEachIndexed { i, entry ->
                    item(key = entry.id.ifBlank { "wp$i" }) {
                        WallpaperEntryCard(
                            index = i, entry = entry, mode = wp.mode, pages = pages,
                            onChange = { updateEntry(i, it) },
                            onRemove = {
                                if (!entry.isBuiltIn) WallpaperStore.delete(entry.uri)
                                updateWp(wp.copy(entries = wp.entries.filterIndexed { idx, _ -> idx != i }))
                            }
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        enabled = !importing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.wallpaper_add_gallery))
                    }
                    OutlinedButton(onClick = { showBuiltIns = !showBuiltIns }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.wallpaper_add_builtin))
                    }
                }
            }
            if (importing) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (showBuiltIns) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BuiltInWallpaper.ALL.chunked(4).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { b ->
                                    Box(
                                        Modifier.weight(1f).height(96.dp).clip(RoundedCornerShape(12.dp))
                                            .background(androidx.compose.ui.graphics.Brush.linearGradient(b.colors.map { com.ailauncher.app.ui.theme.parseHexColor(it) }))
                                            .clickable {
                                                showBuiltIns = false
                                                updateWp(wp.copy(entries = wp.entries + WallpaperEntry(id = "wp_${System.currentTimeMillis()}", isBuiltIn = true, builtInId = b.id)))
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item { SwitchSetting(stringResource(R.string.wallpaper_dim_on_scroll), wp.dimOnScroll) { updateWp(wp.copy(dimOnScroll = it)) } }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WallpaperEntryCard(
    index: Int,
    entry: WallpaperEntry,
    mode: WallpaperMode,
    pages: PagesSettings,
    onChange: (WallpaperEntry) -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Box(Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.background)) {
                com.ailauncher.app.ui.wallpaper.WallpaperImage(entry, Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.wallpaper_entry_label, index + 1), fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)

            when (mode) {
                WallpaperMode.PER_PAGE -> {
                    Text(stringResource(R.string.wallpaper_for_page), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = entry.page.isBlank(), onClick = { onChange(entry.copy(page = "")) }, label = { Text(stringResource(R.string.wallpaper_any_page)) })
                        pages.enabledPages.forEach { page ->
                            FilterChip(selected = entry.page == page.id, onClick = { onChange(entry.copy(page = page.id)) }, label = { Text(page.localizedName(context)) })
                        }
                    }
                }
                WallpaperMode.HOURLY -> {
                    val hour = entry.hour.coerceIn(0, 23)
                    SliderSetting(stringResource(R.string.wallpaper_from_hour), hour.toFloat(), 0f..23f, "%02d:00".format(hour), steps = 22) {
                        onChange(entry.copy(hour = it.toInt()))
                    }
                }
                WallpaperMode.DAILY -> {
                    Text(stringResource(R.string.wallpaper_on_day), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val locale = LocalContext.current.resources.configuration.locales[0]
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // java.util.Calendar numbering: SUNDAY = 1 … SATURDAY = 7.
                        (1..7).forEach { calDay ->
                            val name = java.time.DayOfWeek.of(if (calDay == 1) 7 else calDay - 1)
                                .getDisplayName(java.time.format.TextStyle.SHORT, locale)
                            FilterChip(selected = entry.dayOfWeek == calDay, onClick = { onChange(entry.copy(dayOfWeek = calDay)) }, label = { Text(name) })
                        }
                    }
                }
                else -> {}
            }

            Spacer(Modifier.height(4.dp))
            SliderSetting(stringResource(R.string.wallpaper_brightness), entry.brightness, 0f..1f, "${(entry.brightness * 100).toInt()}%") { onChange(entry.copy(brightness = it)) }
            if (!entry.isBuiltIn) {
                SliderSetting(stringResource(R.string.wallpaper_contrast), entry.contrast, 0.5f..1.5f, "${(entry.contrast * 100).toInt()}%") { onChange(entry.copy(contrast = it)) }
                SliderSetting(stringResource(R.string.wallpaper_saturation), entry.saturation, 0f..2f, "${(entry.saturation * 100).toInt()}%") { onChange(entry.copy(saturation = it)) }
                SliderSetting(stringResource(R.string.wallpaper_warmth), entry.warmth, -1f..1f, "${(entry.warmth * 100).toInt()}") { onChange(entry.copy(warmth = it)) }
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                SliderSetting(stringResource(R.string.wallpaper_blur), entry.blur, 0f..25f, "${entry.blur.toInt()}") { onChange(entry.copy(blur = it)) }
            }
            SliderSetting(stringResource(R.string.wallpaper_vignette), entry.vignette, 0f..1f, "${(entry.vignette * 100).toInt()}%") { onChange(entry.copy(vignette = it)) }
            SliderSetting(stringResource(R.string.wallpaper_opacity), entry.opacity, 0.2f..1f, "${(entry.opacity * 100).toInt()}%") { onChange(entry.copy(opacity = it)) }
            TextButton(onClick = onRemove) { Text(stringResource(R.string.action_remove_wallpaper), color = MaterialTheme.colorScheme.error) }
        }
    }
}

/**
 * v10: links point at the live website (the old ones went to novelauncher.app,
 * a domain that doesn't exist), the real app icon replaces the placeholder
 * "N", and the opt-in update check lives here.
 */
@Composable
fun AboutSection(
    smartControl: com.ailauncher.app.domain.models.SmartControlSettings,
    onUpdateSmartControl: (com.ailauncher.app.domain.models.SmartControlSettings) -> Unit,
    updateChecker: com.ailauncher.app.data.update.UpdateChecker
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var updateStatus by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    fun open(url: String) {
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: Exception) {}
    }
    val appIcon = remember {
        androidx.core.content.res.ResourcesCompat.getDrawable(context.resources, R.mipmap.ic_launcher, context.theme)
            ?.let { runCatching { it.toBitmap(192, 192).asImageBitmap() }.getOrNull() }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
                if (appIcon != null) {
                    androidx.compose.foundation.Image(appIcon, null, Modifier.size(80.dp).clip(RoundedCornerShape(22.dp)))
                } else {
                    Box(Modifier.size(72.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                        Text("N", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("NoveLauncher", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                // v9: show the real build version instead of a hardcoded "1.0.0".
                Text(stringResource(R.string.about_version, com.ailauncher.app.BuildConfig.VERSION_NAME), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item {
            Text(
                stringResource(R.string.about_description),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )
        }

        item { HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant) }

        item { SectionLabel(stringResource(R.string.about_updates_section)) }
        item {
            SwitchSetting(stringResource(R.string.about_check_updates_auto), smartControl.checkForUpdates) {
                onUpdateSmartControl(smartControl.copy(checkForUpdates = it))
            }
        }
        item {
            Text(stringResource(R.string.about_check_updates_note), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            val upToDate = stringResource(R.string.about_up_to_date)
            OutlinedButton(onClick = {
                checking = true
                scope.launch {
                    val found = updateChecker.check(force = true)
                    checking = false
                    updateStatus = found?.let { context.getString(R.string.update_available, it.version) } ?: upToDate
                    found?.let { open(it.url) }
                }
            }, enabled = !checking, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.about_check_updates_now))
            }
        }
        updateStatus?.let { item { Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary) } }

        item { HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant) }

        item {
            AboutButton(Icons.Rounded.Language, stringResource(R.string.about_website)) { open(SITE) }
        }
        item {
            AboutButton(Icons.Rounded.Code, stringResource(R.string.about_source_code)) { open("https://github.com/DrummingBird1/NoveLauncher") }
        }
        item {
            val rateLabel = stringResource(R.string.about_rate)
            AboutButton(Icons.Rounded.Star, rateLabel) {
                try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.ailauncher.app"))) }
                catch (_: Exception) { open("https://play.google.com/store/apps/details?id=com.ailauncher.app") }
            }
        }
        item {
            AboutButton(Icons.Rounded.Favorite, stringResource(R.string.about_support)) { open("https://buymeacoffee.com/novelauncher") }
        }
        item {
            AboutButton(Icons.Rounded.VolunteerActivism, stringResource(R.string.about_patreon)) { open("https://www.patreon.com/cw/MrIdan") }
        }
        item {
            val contactLabel = stringResource(R.string.about_contact)
            val emailSubject = stringResource(R.string.about_contact_email_subject)
            AboutButton(Icons.Rounded.Email, contactLabel) {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:solvaris2@gmail.com")
                    putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                }
                try { context.startActivity(intent) } catch (_: Exception) {}
            }
        }
        item {
            AboutButton(Icons.Rounded.Policy, stringResource(R.string.about_privacy)) { open("$SITE/privacy.html") }
        }
        item {
            AboutButton(Icons.Rounded.Description, stringResource(R.string.about_terms)) { open("$SITE/terms.html") }
        }

        item {
            Text(
                stringResource(R.string.about_copyright),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            )
        }
    }
}

private const val SITE = "https://drummingbird1.github.io/NoveLauncher"

@Composable
fun AboutButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(label, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/**
 * v10: packs were listed but could never be selected or applied. Now one is
 * chosen here (AppearanceSettings.iconPackPackage) and InstalledAppsRepository
 * swaps in its icons; apps the pack doesn't cover keep their own icon.
 */
@Composable
fun IconPacksSection(manager: IconPackManager, appearance: AppearanceSettings, onUpdate: (AppearanceSettings) -> Unit) {
    val packs = remember { manager.getAvailableIconPacks() }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            IconPackRow(
                label = stringResource(R.string.icon_pack_system),
                icon = null,
                selected = appearance.iconPackPackage.isBlank()
            ) { onUpdate(appearance.copy(iconPackPackage = "")) }
        }
        if (packs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(stringResource(R.string.icon_packs_empty_title), color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.icon_packs_empty_hint),
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(packs, key = { it.packageName }) { pack ->
                IconPackRow(pack.label, pack.icon, selected = appearance.iconPackPackage == pack.packageName) {
                    manager.clearCache()
                    onUpdate(appearance.copy(iconPackPackage = pack.packageName))
                }
            }
        }
    }
}

@Composable
private fun IconPackRow(label: String, icon: android.graphics.drawable.Drawable?, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = null)
            Spacer(Modifier.width(8.dp))
            val bmp = remember(icon) { icon?.let { runCatching { it.toBitmap(96, 96).asImageBitmap() }.getOrNull() } }
            if (bmp != null) {
                androidx.compose.foundation.Image(bmp, null, Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)))
            } else {
                Icon(Icons.Rounded.AppShortcut, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Text(label, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
