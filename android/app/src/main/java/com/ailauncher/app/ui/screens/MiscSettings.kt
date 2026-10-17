package com.ailauncher.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ailauncher.app.R
import com.ailauncher.app.domain.models.HiddenAppsSettings
import com.ailauncher.app.domain.models.NewsSettings
import com.ailauncher.app.domain.models.NewsSource

/*
 * v9: extracted from SettingsActivity.kt. Two small SettingsPages that share a
 * file: HIDDEN_APPS and NEWS_SOURCES. v10: WALLPAPER, ABOUT and ICON_PACKS
 * moved to WallpaperSettings.kt, AboutSettings.kt and IconPackSettings.kt.
 */

@Composable
fun NewsSourcesSection(
    settings: NewsSettings,
    onUpdate: (NewsSettings) -> Unit,
) {
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
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            if (enabled) {
                                MaterialTheme.colorScheme.primary.copy(
                                    alpha = 0.1f,
                                )
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                    ),
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
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                if (enabled) {
                                    MaterialTheme.colorScheme.primary.copy(
                                        alpha = 0.1f,
                                    )
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                        ),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = enabled, onCheckedChange = null)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(source.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                            Text(
                                source.url,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(onClick = {
                            onUpdate(
                                settings.copy(
                                    customSources = settings.customSources.filter { it.id != source.id },
                                    enabledSourceIds = settings.enabledSourceIds - source.id,
                                ),
                            )
                        }) { Icon(Icons.Rounded.Close, stringResource(R.string.action_remove), modifier = Modifier.size(18.dp)) }
                    }
                }
            }
        }

        item {
            OutlinedButton(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_add_custom_news_source))
            }
        }

        item {
            SliderSetting(
                stringResource(R.string.news_max_articles),
                settings.maxArticles.toFloat(),
                10f..100f,
                "${settings.maxArticles}",
                steps = 8,
            ) {
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
                    OutlinedTextField(value = name, onValueChange = {
                        name = it
                    }, label = {
                        Text(
                            stringResource(R.string.dialog_news_source_name_label),
                        )
                    }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = url, onValueChange = {
                        url = it
                    }, label = {
                        Text(
                            stringResource(R.string.dialog_news_source_url_label),
                        )
                    }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmedUrl = url.trim()
                    if (name.isNotBlank() && (trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://"))) {
                        val id = "custom_${System.currentTimeMillis()}"
                        val newSource = NewsSource(id, name.trim(), trimmedUrl, language = "he")
                        onUpdate(
                            settings.copy(
                                customSources = settings.customSources + newSource,
                                enabledSourceIds = settings.enabledSourceIds + id,
                            ),
                        )
                        showAddDialog = false
                    }
                }) { Text(stringResource(R.string.action_create)) }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
fun HiddenAppsSection(
    hidden: HiddenAppsSettings,
    onUpdate: (HiddenAppsSettings) -> Unit,
) {
    val context = LocalContext.current
    val apps =
        remember {
            val pm = context.packageManager
            val intent =
                android.content
                    .Intent(
                        android.content.Intent.ACTION_MAIN,
                    ).apply { addCategory(android.content.Intent.CATEGORY_LAUNCHER) }
            pm
                .queryIntentActivities(intent, 0)
                .mapNotNull { ri ->
                    val pkg = ri.activityInfo?.packageName ?: return@mapNotNull null
                    if (pkg == context.packageName) return@mapNotNull null
                    pkg to ri.loadLabel(pm).toString()
                }.sortedBy { it.second }
        }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item { SectionLabel(stringResource(R.string.hidden_apps_count_label, hidden.hiddenPackages.size)) }
        item {
            Text(
                stringResource(R.string.hidden_apps_description),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item { SectionLabel(stringResource(R.string.private_folder_count_label, hidden.privateFolderPackages.size)) }
        item {
            Text(
                stringResource(R.string.private_folder_description),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }

        items(apps) { (pkg, label) ->
            val isHidden = pkg in hidden.hiddenPackages
            val isPrivate = pkg in hidden.privateFolderPackages
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                    when {
                        isHidden -> {
                            Text(stringResource(R.string.label_hidden), fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }

                        isPrivate -> {
                            Text(
                                stringResource(R.string.label_private_folder),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
                IconButton(onClick = {
                    val newHidden = hidden.hiddenPackages.toMutableSet()
                    if (isHidden) {
                        newHidden.remove(pkg)
                    } else {
                        newHidden.add(pkg)
                    }
                    onUpdate(hidden.copy(hiddenPackages = newHidden, privateFolderPackages = hidden.privateFolderPackages - pkg))
                }) {
                    Icon(
                        if (isHidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                        stringResource(R.string.action_hide),
                        tint = if (isHidden) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = {
                    val newPrivate = hidden.privateFolderPackages.toMutableSet()
                    if (isPrivate) {
                        newPrivate.remove(pkg)
                    } else {
                        newPrivate.add(pkg)
                    }
                    onUpdate(hidden.copy(privateFolderPackages = newPrivate, hiddenPackages = hidden.hiddenPackages - pkg))
                }) {
                    Icon(
                        Icons.Rounded.Lock,
                        stringResource(R.string.label_private_folder),
                        tint = if (isPrivate) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
