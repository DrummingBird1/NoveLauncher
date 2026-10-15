package com.ailauncher.app.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ailauncher.app.R
import com.ailauncher.app.domain.search.QuickAnswers
import com.ailauncher.app.domain.search.SearchMatcher
import com.ailauncher.app.ui.LauncherViewModel
import com.ailauncher.app.ui.LocalIconCache
import com.ailauncher.app.ui.launchApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * v6 feature #9: Unified search — apps, contacts, settings, web.
 *
 * v10: apps come from the launcher's own ranked list (LauncherViewModel.searchApps)
 * instead of a fresh PackageManager query, which means:
 * - hidden and private-folder apps no longer leak into search results;
 * - work-profile and (unlocked) Private Space apps are included;
 * - matching is SearchMatcher's: Hebrew ↔ English spellings, typos, initials.
 * Plus an instant-answer card (calculator, unit conversion) from QuickAnswers.
 */
data class SearchResult(
    val type: ResultType,
    val title: String,
    val subtitle: String = "",
    val icon: ImageVector,
    val bitmap: ImageBitmap? = null,
    val action: () -> Unit
)

enum class ResultType(@StringRes val displayNameRes: Int) {
    APP(R.string.search_type_app),
    CONTACT(R.string.search_type_contact),
    SETTING(R.string.search_type_setting),
    WEB(R.string.search_type_web)
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
    SettingsPage.GESTURES to R.string.settings_gestures,
    SettingsPage.JEWISH_CALENDAR to R.string.settings_jewish_calendar,
    SettingsPage.ABOUT to R.string.settings_about,
    SettingsPage.STATISTICS to R.string.settings_statistics,
    SettingsPage.ICON_PACKS to R.string.settings_icon_packs
)

@Composable
fun GlobalSearchScreen(viewModel: LauncherViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val iconCache = LocalIconCache.current
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var answer by remember { mutableStateOf<QuickAnswers.Answer?>(null) }
    var hasLocalMatches by remember { mutableStateOf(true) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    // v10: system Back closes the overlay (it used to do nothing).
    androidx.activity.compose.BackHandler(onBack = onDismiss)

    LaunchedEffect(query) {
        if (query.isBlank()) {
            results = emptyList()
            answer = null
            return@LaunchedEffect
        }
        delay(120) // let fast typing settle before querying contacts
        answer = withContext(Dispatchers.Default) { QuickAnswers.answer(query) }

        val apps = viewModel.searchApps(query, limit = 6).map { ranked ->
            val app = ranked.app
            SearchResult(
                ResultType.APP, app.label, "", Icons.Rounded.Apps,
                bitmap = app.icon?.let { d -> iconCache.getOrLoad(app.key) { d } }
            ) { launchApp(context, app) } // routed through the app-lock check
        }

        val contacts = withContext(Dispatchers.IO) { searchContacts(context, query) }

        val settingsMatches = SEARCHABLE_SETTINGS_PAGES.mapNotNull { (page, labelRes) ->
            val label = context.getString(labelRes)
            if (SearchMatcher.score(query, label) >= 58) {
                SearchResult(ResultType.SETTING, label, "", Icons.Rounded.Settings) {
                    context.startActivity(Intent(context, SettingsActivity::class.java).apply {
                        putExtra(SettingsActivity.EXTRA_SHORTCUT_PAGE, page.name)
                    })
                }
            } else null
        }

        val local = apps + contacts + settingsMatches
        hasLocalMatches = local.isNotEmpty() || answer != null

        // Web search fallback — always offered, even when nothing local matched.
        results = local + SearchResult(ResultType.WEB, context.getString(R.string.search_google_for, query), "", Icons.Rounded.Search) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")))
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.97f)).clickable(onClick = onDismiss)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_close)) }
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
                answer?.let { a ->
                    item(key = "answer") {
                        Card(
                            Modifier.fillMaxWidth().clickable { copyToClipboard(context, a.copyValue) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Calculate, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Text(a.display, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                Icon(Icons.Rounded.ContentCopy, stringResource(R.string.search_copy_result), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
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
                results.groupBy { it.type }.toSortedMap(compareBy { it.ordinal }).forEach { (type, items) ->
                    item {
                        Text(stringResource(type.displayNameRes), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
                    }
                    items(items) { result ->
                        Card(Modifier.fillMaxWidth().clickable { result.action(); onDismiss() }, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (result.bitmap != null) {
                                    Image(result.bitmap, null, Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)))
                                } else {
                                    Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                        Icon(result.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    }
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

/** Contacts by name (only if READ_CONTACTS is granted — silently skipped otherwise). */
private fun searchContacts(context: Context, query: String): List<SearchResult> {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
        return emptyList()
    }
    return try {
        val resolver: ContentResolver = context.contentResolver
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val out = mutableListOf<SearchResult>()
        resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, selection, arrayOf("%$query%"), null)?.use { cursor ->
            while (cursor.moveToNext() && out.size < 5) {
                val name = cursor.getString(0) ?: continue
                val number = cursor.getString(1) ?: continue
                out.add(SearchResult(ResultType.CONTACT, name, number, Icons.Rounded.Person) {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
                })
            }
        }
        out
    } catch (_: Exception) {
        emptyList()
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("NoveLauncher", text))
    // Android 13+ shows its own clipboard confirmation.
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, context.getString(R.string.search_copied, text), Toast.LENGTH_SHORT).show()
    }
}
