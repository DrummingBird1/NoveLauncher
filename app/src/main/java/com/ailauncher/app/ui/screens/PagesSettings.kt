package com.ailauncher.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Tab
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ailauncher.app.R
import com.ailauncher.app.domain.models.LauncherPage
import com.ailauncher.app.domain.models.PagesSettings

/**
 * v9: extracted from SettingsActivity.kt. The PAGES SettingsPage — enabled
 * pages, drag-order, disabled built-ins, and the "create new page" dialog.
 */
@Composable
fun PagesSection(pages: PagesSettings, onUpdate: (PagesSettings) -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionLabel(stringResource(R.string.settings_pages)) }

        items(pages.enabledPages) { page ->
            val isDefault = page.id == pages.defaultPageId
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (page.iconName) {
                            "home" -> Icons.Rounded.Home; "apps" -> Icons.Rounded.Apps
                            "newspaper" -> Icons.Rounded.Newspaper; else -> Icons.Rounded.Tab
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(page.localizedName(LocalContext.current), fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        if (isDefault) Text(stringResource(R.string.page_default), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        if (!page.isBuiltIn) Text(stringResource(R.string.page_custom), fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                    if (!isDefault) {
                        TextButton(onClick = { onUpdate(pages.copy(defaultPageId = page.id)) }) {
                            Text(stringResource(R.string.page_default), fontSize = 11.sp)
                        }
                    }
                    if (page.isRemovable) {
                        IconButton(onClick = {
                            val newPages = pages.enabledPages.filter { it.id != page.id }
                            val newCustom = pages.customPages.filter { it.id != page.id }
                            onUpdate(pages.copy(enabledPages = newPages, customPages = newCustom))
                        }) { Icon(Icons.Rounded.Close, stringResource(R.string.action_remove), modifier = Modifier.size(18.dp)) }
                    }
                }
            }
        }

        item { SectionLabel(stringResource(R.string.section_order_drag)) }
        pages.enabledPages.forEachIndexed { index, page ->
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${index + 1}.", Modifier.width(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(page.localizedName(LocalContext.current), Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                    if (index > 0) IconButton(onClick = {
                        val list = pages.enabledPages.toMutableList()
                        list[index] = list[index - 1].also { list[index - 1] = list[index] }
                        onUpdate(pages.copy(enabledPages = list))
                    }) { Icon(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.action_move_up)) }
                    if (index < pages.enabledPages.lastIndex) IconButton(onClick = {
                        val list = pages.enabledPages.toMutableList()
                        list[index] = list[index + 1].also { list[index + 1] = list[index] }
                        onUpdate(pages.copy(enabledPages = list))
                    }) { Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.action_move_down)) }
                }
            }
        }

        val disabled = LauncherPage.BUILT_IN.filter { bp -> pages.enabledPages.none { it.id == bp.id } }
        if (disabled.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.section_disabled_pages)) }
            disabled.forEach { page ->
                item {
                    Card(Modifier.fillMaxWidth().clickable {
                        onUpdate(pages.copy(enabledPages = pages.enabledPages + page))
                    }, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AddCircleOutline, null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(R.string.action_enable_page, page.localizedName(LocalContext.current)), color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        item {
            OutlinedButton(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.action_create_new_page))
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.dialog_new_page_title)) },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.dialog_new_page_name_label)) }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        val id = "custom_${System.currentTimeMillis()}"
                        val newPage = LauncherPage(id, name, "tab", isBuiltIn = false, isRemovable = true)
                        onUpdate(pages.copy(
                            enabledPages = pages.enabledPages + newPage,
                            customPages = pages.customPages + newPage
                        ))
                        showAddDialog = false
                    }
                }) { Text(stringResource(R.string.action_create)) }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}
