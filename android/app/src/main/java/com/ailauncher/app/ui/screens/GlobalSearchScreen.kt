package com.ailauncher.app.ui.screens

import android.Manifest
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.StringRes
import com.ailauncher.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * v6 feature #9: Unified search — apps, contacts, web.
 */
data class SearchResult(
    val type: ResultType,
    val title: String,
    val subtitle: String = "",
    val icon: ImageVector,
    val action: () -> Unit
)

enum class ResultType(@StringRes val displayNameRes: Int) {
    APP(R.string.search_type_app),
    CONTACT(R.string.search_type_contact),
    WEB(R.string.search_type_web),
    SETTING(R.string.search_type_setting)
}

private val SEARCHABLE_SETTINGS_PAGES = listOf(
    SettingsPage.APPEARANCE to R.string.settings_appearance,
    SettingsPage.THEMES to R.string.settings_themes,
    SettingsPage.FONTS to R.string.settings_fonts,
    SettingsPage.ICON_SHAPES to R.string.settings_icon_shapes,
    SettingsPage.CLOCK to R.string.settings_clock,
    SettingsPage.PAGES to R.string.settings_pages,
    SettingsPage.BACKUP to R.string.settings_backup,
    SettingsPage.SECURITY to R.string.settings_security,
    SettingsPage.NEWS_SOURCES to R.string.settings_news_sources,
    SettingsPage.HIDDEN_APPS to R.string.settings_hidden_apps,
    SettingsPage.WALLPAPER to R.string.settings_wallpapers,
    SettingsPage.ABOUT to R.string.settings_about,
    SettingsPage.STATISTICS to R.string.settings_statistics,
    SettingsPage.ICON_PACKS to R.string.settings_icon_packs
)

@Composable
fun GlobalSearchScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var hasLocalMatches by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    LaunchedEffect(query) {
        if (query.isBlank()) { results = emptyList(); return@LaunchedEffect }
        scope.launch {
            val combined = mutableListOf<SearchResult>()

            // Apps
            val apps = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
                pm.queryIntentActivities(intent, 0)
                    .mapNotNull { ri ->
                        val label = ri.loadLabel(pm).toString()
                        val pkg = ri.activityInfo?.packageName ?: return@mapNotNull null
                        if (label.contains(query, ignoreCase = true)) {
                            SearchResult(ResultType.APP, label, pkg, Icons.Rounded.Apps) {
                                // Route through launchApp so AppLockManager checks fire.
                                launchApp(context, pkg)
                            }
                        } else null
                    }
                    .take(5)
            }
            combined.addAll(apps)

            // Contacts (if permission granted — silently skip otherwise)
            val contacts = withContext(Dispatchers.IO) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
                    != PackageManager.PERMISSION_GRANTED) return@withContext emptyList<SearchResult>()
                try {
                    val resolver: ContentResolver = context.contentResolver
                    val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                    val projection = arrayOf(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    )
                    val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
                    val results = mutableListOf<SearchResult>()
                    resolver.query(uri, projection, selection, arrayOf("%$query%"), null)?.use { cursor ->
                        while (cursor.moveToNext() && results.size < 5) {
                            val name = cursor.getString(0) ?: continue
                            val number = cursor.getString(1) ?: continue
                            results.add(SearchResult(ResultType.CONTACT, name, number, Icons.Rounded.Person) {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
                            })
                        }
                    }
                    results
                } catch (_: Exception) { emptyList() }
            }
            combined.addAll(contacts)

            // Settings pages
            val settingsMatches = SEARCHABLE_SETTINGS_PAGES.mapNotNull { (page, labelRes) ->
                val label = context.getString(labelRes)
                if (label.contains(query, ignoreCase = true)) {
                    SearchResult(ResultType.SETTING, label, "", Icons.Rounded.Settings) {
                        context.startActivity(Intent(context, SettingsActivity::class.java).apply {
                            putExtra(SettingsActivity.EXTRA_SHORTCUT_PAGE, page.name)
                        })
                    }
                } else null
            }
            combined.addAll(settingsMatches)

            hasLocalMatches = combined.isNotEmpty()

            // Web search fallback — always offered, even when nothing local matched.
            combined.add(SearchResult(ResultType.WEB, context.getString(R.string.search_google_for, query), "", Icons.Rounded.Search) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")))
            })

            results = combined
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f)).clickable(onClick = onDismiss)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.ArrowBack, stringResource(R.string.action_close)) }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 20.dp, vertical = 16.dp)) {
                    if (query.isEmpty()) Text(stringResource(R.string.search_all_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                    BasicTextField(
                        value = query, onValueChange = { query = it },
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (query.isNotBlank() && !hasLocalMatches) {
                    item {
                        Text(
                            stringResource(R.string.search_no_local_matches),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
                val grouped = results.groupBy { it.type }
                grouped.forEach { (type, items) ->
                    item {
                        Text(stringResource(type.displayNameRes), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
                    }
                    items(items) { result ->
                        Card(Modifier.fillMaxWidth().clickable { result.action(); onDismiss() }, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                    Icon(result.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(result.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                                    if (result.subtitle.isNotBlank()) Text(result.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
