package com.ailauncher.app.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ailauncher.app.R
import com.ailauncher.app.data.wallpaper.WallpaperStore
import com.ailauncher.app.domain.models.AppearanceSettings
import com.ailauncher.app.domain.models.BuiltInWallpaper
import com.ailauncher.app.domain.models.PagesSettings
import com.ailauncher.app.domain.models.WallpaperEntry
import com.ailauncher.app.domain.models.WallpaperMode
import com.ailauncher.app.domain.models.WallpaperSettings
import kotlinx.coroutines.launch

// v10: split out of MiscSettings.kt.

/**
 * v10: the wallpaper editor actually does something now. Images come from the
 * system Photo Picker (no storage permission) and are copied into app storage
 * (WallpaperStore); built-in gradients need no image at all. Each entry gets
 * a live thumbnail, the adjustments WallpaperLayer applies, and — depending on
 * the mode — the page, start hour or weekday it belongs to.
 */
@Composable
fun WallpaperSection(
    appearance: AppearanceSettings,
    pages: PagesSettings,
    onUpdate: (AppearanceSettings) -> Unit,
) {
    val wp = appearance.wallpaper
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun updateWp(w: WallpaperSettings) = onUpdate(appearance.copy(wallpaper = w))

    fun updateEntry(
        i: Int,
        e: WallpaperEntry,
    ) = updateWp(wp.copy(entries = wp.entries.toMutableList().also { it[i] = e }))
    var showBuiltIns by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    val importFailed = stringResource(R.string.wallpaper_import_failed)

    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                scope.launch {
                    importing = true
                    val id = "wp_${System.currentTimeMillis()}"
                    val path = WallpaperStore.import(context, uri, id)
                    importing = false
                    if (path != null) {
                        updateWp(wp.copy(entries = wp.entries + WallpaperEntry(id = id, uri = path)))
                    } else {
                        android.widget.Toast
                            .makeText(context, importFailed, android.widget.Toast.LENGTH_SHORT)
                            .show()
                    }
                }
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
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    if (wp.mode == mode) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                            ),
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = wp.mode == mode, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(stringResource(mode.displayNameRes), color = MaterialTheme.colorScheme.onSurface)
                                val descRes =
                                    when (mode) {
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
                        try {
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), null))
                        } catch (
                            _: Exception,
                        ) {
                        }
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.wallpaper_live_choose)) }
            }
        } else {
            if (wp.entries.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.section_wallpaper_active)) }
                wp.entries.forEachIndexed { i, entry ->
                    item(key = entry.id.ifBlank { "wp$i" }) {
                        WallpaperEntryCard(
                            index = i,
                            entry = entry,
                            mode = wp.mode,
                            pages = pages,
                            onChange = { updateEntry(i, it) },
                            onRemove = {
                                if (!entry.isBuiltIn) WallpaperStore.delete(entry.uri)
                                updateWp(wp.copy(entries = wp.entries.filterIndexed { idx, _ -> idx != i }))
                            },
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        enabled = !importing,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Add, null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.wallpaper_add_gallery))
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
                                        Modifier
                                            .weight(1f)
                                            .height(96.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                androidx.compose.ui.graphics.Brush.linearGradient(
                                                    b.colors.map {
                                                        com.ailauncher.app.ui.theme
                                                            .parseHexColor(it)
                                                    },
                                                ),
                                            ).clickable {
                                                showBuiltIns = false
                                                updateWp(
                                                    wp.copy(
                                                        entries =
                                                            wp.entries +
                                                                WallpaperEntry(
                                                                    id = "wp_${System.currentTimeMillis()}",
                                                                    isBuiltIn = true,
                                                                    builtInId = b.id,
                                                                ),
                                                    ),
                                                )
                                            },
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
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(12.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background),
            ) {
                com.ailauncher.app.ui.wallpaper
                    .WallpaperImage(entry, Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.wallpaper_entry_label, index + 1),
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            when (mode) {
                WallpaperMode.PER_PAGE -> {
                    Text(stringResource(R.string.wallpaper_for_page), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = entry.page.isBlank(), onClick = {
                            onChange(entry.copy(page = ""))
                        }, label = { Text(stringResource(R.string.wallpaper_any_page)) })
                        pages.enabledPages.forEach { page ->
                            FilterChip(selected = entry.page == page.id, onClick = {
                                onChange(entry.copy(page = page.id))
                            }, label = { Text(page.localizedName(context)) })
                        }
                    }
                }

                WallpaperMode.HOURLY -> {
                    val hour = entry.hour.coerceIn(0, 23)
                    SliderSetting(
                        stringResource(R.string.wallpaper_from_hour),
                        hour.toFloat(),
                        0f..23f,
                        "%02d:00".format(hour),
                        steps = 22,
                    ) {
                        onChange(entry.copy(hour = it.toInt()))
                    }
                }

                WallpaperMode.DAILY -> {
                    Text(stringResource(R.string.wallpaper_on_day), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val locale = LocalContext.current.resources.configuration.locales[0]
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // java.util.Calendar numbering: SUNDAY = 1 … SATURDAY = 7.
                        (1..7).forEach { calDay ->
                            val name =
                                java.time.DayOfWeek
                                    .of(if (calDay == 1) 7 else calDay - 1)
                                    .getDisplayName(java.time.format.TextStyle.SHORT, locale)
                            FilterChip(
                                selected = entry.dayOfWeek == calDay,
                                onClick = { onChange(entry.copy(dayOfWeek = calDay)) },
                                label = { Text(name) },
                            )
                        }
                    }
                }

                else -> {}
            }

            Spacer(Modifier.height(4.dp))
            SliderSetting(stringResource(R.string.wallpaper_brightness), entry.brightness, 0f..1f, "${(entry.brightness * 100).toInt()}%") {
                onChange(entry.copy(brightness = it))
            }
            if (!entry.isBuiltIn) {
                SliderSetting(
                    stringResource(R.string.wallpaper_contrast),
                    entry.contrast,
                    0.5f..1.5f,
                    "${(entry.contrast * 100).toInt()}%",
                ) {
                    onChange(entry.copy(contrast = it))
                }
                SliderSetting(
                    stringResource(R.string.wallpaper_saturation),
                    entry.saturation,
                    0f..2f,
                    "${(entry.saturation * 100).toInt()}%",
                ) {
                    onChange(entry.copy(saturation = it))
                }
                SliderSetting(stringResource(R.string.wallpaper_warmth), entry.warmth, -1f..1f, "${(entry.warmth * 100).toInt()}") {
                    onChange(entry.copy(warmth = it))
                }
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                SliderSetting(
                    stringResource(R.string.wallpaper_blur),
                    entry.blur,
                    0f..25f,
                    "${entry.blur.toInt()}",
                ) { onChange(entry.copy(blur = it)) }
            }
            SliderSetting(stringResource(R.string.wallpaper_vignette), entry.vignette, 0f..1f, "${(entry.vignette * 100).toInt()}%") {
                onChange(entry.copy(vignette = it))
            }
            SliderSetting(stringResource(R.string.wallpaper_opacity), entry.opacity, 0.2f..1f, "${(entry.opacity * 100).toInt()}%") {
                onChange(entry.copy(opacity = it))
            }
            TextButton(
                onClick = onRemove,
            ) { Text(stringResource(R.string.action_remove_wallpaper), color = MaterialTheme.colorScheme.error) }
        }
    }
}
