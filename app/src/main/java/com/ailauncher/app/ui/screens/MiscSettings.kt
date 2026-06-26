package com.ailauncher.app.ui.screens

import android.content.Intent
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
        item {
            SliderSetting(stringResource(R.string.news_max_articles), settings.maxArticles.toFloat(), 10f..100f, "${settings.maxArticles}", steps = 8) {
                onUpdate(settings.copy(maxArticles = it.toInt()))
            }
        }
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

@Composable
fun WallpaperSection(appearance: AppearanceSettings, onUpdate: (AppearanceSettings) -> Unit) {
    val wp = appearance.wallpaper
    fun updateWp(w: WallpaperSettings) = onUpdate(appearance.copy(wallpaper = w))

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

        if (wp.entries.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.section_wallpaper_active)) }
            wp.entries.forEachIndexed { i, entry ->
                item {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(stringResource(R.string.wallpaper_entry_label, i + 1), fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(8.dp))
                            SliderSetting(stringResource(R.string.wallpaper_brightness), entry.brightness, 0f..1f, "${(entry.brightness * 100).toInt()}%") {
                                val newEntries = wp.entries.toMutableList(); newEntries[i] = entry.copy(brightness = it); updateWp(wp.copy(entries = newEntries))
                            }
                            SliderSetting(stringResource(R.string.wallpaper_blur), entry.blur, 0f..25f, "${entry.blur.toInt()}") {
                                val newEntries = wp.entries.toMutableList(); newEntries[i] = entry.copy(blur = it); updateWp(wp.copy(entries = newEntries))
                            }
                            SliderSetting(stringResource(R.string.wallpaper_saturation), entry.saturation, 0f..2f, "${(entry.saturation * 100).toInt()}%") {
                                val newEntries = wp.entries.toMutableList(); newEntries[i] = entry.copy(saturation = it); updateWp(wp.copy(entries = newEntries))
                            }
                            TextButton(onClick = {
                                updateWp(wp.copy(entries = wp.entries.filterIndexed { idx, _ -> idx != i }))
                            }) { Text(stringResource(R.string.action_remove_wallpaper), color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }

        item {
            OutlinedButton(onClick = {
                val newEntry = WallpaperEntry(id = "wp_${System.currentTimeMillis()}")
                updateWp(wp.copy(entries = wp.entries + newEntry))
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.action_add_wallpaper))
            }
        }

        item { SwitchSetting(stringResource(R.string.wallpaper_dim_on_scroll), wp.dimOnScroll) { updateWp(wp.copy(dimOnScroll = it)) } }
    }
}

@Composable
fun AboutSection() {
    val context = LocalContext.current
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
                Box(Modifier.size(72.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                    Text("N", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Bold)
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

        item {
            val rateLabel = stringResource(R.string.about_rate)
            AboutButton(Icons.Rounded.Star, rateLabel) {
                try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.ailauncher.app"))) }
                catch (_: Exception) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.ailauncher.app"))) }
            }
        }
        item {
            val supportLabel = stringResource(R.string.about_support)
            AboutButton(Icons.Rounded.Favorite, supportLabel) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/novelauncher")))
            }
        }
        item {
            val patreonLabel = stringResource(R.string.about_patreon)
            AboutButton(Icons.Rounded.VolunteerActivism, patreonLabel) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.patreon.com/cw/MrIdan")))
            }
        }
        item {
            val contactLabel = stringResource(R.string.about_contact)
            val emailSubject = stringResource(R.string.about_contact_email_subject)
            AboutButton(Icons.Rounded.Email, contactLabel) {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:support@novelauncher.app")
                    putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                }
                try { context.startActivity(intent) } catch (_: Exception) {}
            }
        }
        item {
            val privacyLabel = stringResource(R.string.about_privacy)
            AboutButton(Icons.Rounded.Policy, privacyLabel) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://novelauncher.app/privacy")))
            }
        }
        item {
            val termsLabel = stringResource(R.string.about_terms)
            AboutButton(Icons.Rounded.Description, termsLabel) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://novelauncher.app/terms")))
            }
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

@Composable
fun IconPacksSection(manager: IconPackManager) {
    val packs = remember { manager.getAvailableIconPacks() }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            items(packs) { pack ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AppShortcut, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(pack.label, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}
