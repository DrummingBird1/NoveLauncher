package com.ailauncher.app.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import com.ailauncher.app.R
import com.ailauncher.app.domain.models.NewsSettings
import com.ailauncher.app.domain.models.NewsSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.URL

data class NewsItem(val title: String, val description: String, val link: String, val pubDate: String, val source: String)

/**
 * v8: process-lifetime cache of RSS results. TTL prevents hammering 12 feeds
 * every time the user swipes onto the news page. Cleared with the process.
 */
private object NewsCache {
    private const val TTL_MS = 10 * 60 * 1000L
    private var cachedAt: Long = 0L
    private var cachedKey: String = ""
    private var cached: List<NewsItem> = emptyList()

    fun get(key: String): List<NewsItem>? {
        val now = System.currentTimeMillis()
        return if (key == cachedKey && now - cachedAt < TTL_MS) cached else null
    }

    fun put(key: String, items: List<NewsItem>) {
        cachedKey = key
        cachedAt = System.currentTimeMillis()
        cached = items
    }

    fun invalidate() { cachedAt = 0L }
}

@Composable
fun NewsScreen(newsSettings: NewsSettings = NewsSettings()) {
    val context = LocalContext.current

    // v5: Toggle between RSS feed and embedded app
    var showEmbeddedApp by remember { mutableStateOf(newsSettings.useEmbeddedApp) }

    if (showEmbeddedApp) {
        // Embedded news app mode
        EmbeddedNewsApp(
            packageName = newsSettings.embeddedAppPackage,
            onSwitchToFeed = { showEmbeddedApp = false }
        )
    } else {
        // RSS feed mode
        RssFeedView(
            newsSettings = newsSettings,
            onSwitchToApp = { showEmbeddedApp = true }
        )
    }
}

@Composable
private fun EmbeddedNewsApp(packageName: String, onSwitchToFeed: () -> Unit) {
    val context = LocalContext.current

    // Known news app packages with their web fallback URLs
    val newsApps = mapOf(
        "com.google.android.googlequicksearchbox" to "https://news.google.com/?hl=iw",
        "com.google.android.apps.magazines" to "https://news.google.com/?hl=iw",
        "com.ynet" to "https://www.ynet.co.il",
        "com.walla.android" to "https://www.walla.co.il",
        "com.bbc.mondo" to "https://www.bbc.com/news",
        "com.cnn.mobile.android.phone" to "https://www.cnn.com",
        "com.yahoo.mobile.client.android.yahoo" to "https://news.yahoo.com",
    )

    val isInstalled = remember {
        try { context.packageManager.getApplicationInfo(packageName, 0); true } catch (_: PackageManager.NameNotFoundException) { false }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        // Top bar with switch option
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.page_news), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
            Row {
                // Switch to RSS mode
                IconButton(onClick = onSwitchToFeed) {
                    Icon(Icons.Rounded.RssFeed, stringResource(R.string.news_rss_mode), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // Open external app — go through launchApp so AppLockManager gets a say.
                if (isInstalled) {
                    IconButton(onClick = { launchApp(context, packageName) }) {
                        Icon(Icons.Rounded.OpenInNew, stringResource(R.string.news_open_in_app), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Show WebView with news site
        val webUrl = newsApps[packageName] ?: "https://news.google.com/?hl=iw"
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    webViewClient = WebViewClient()
                    loadUrl(webUrl)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun RssFeedView(newsSettings: NewsSettings, onSwitchToApp: () -> Unit) {
    val context = LocalContext.current
    var news by remember { mutableStateOf<List<NewsItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun load(forceRefresh: Boolean = false) {
        scope.launch {
            isLoading = true; error = null
            try {
                val sources = NewsSource.ALL_SOURCES.filter { it.id in newsSettings.enabledSourceIds }
                val cacheKey = sources.joinToString(",") { it.id } + "|${newsSettings.maxArticles}"
                if (forceRefresh) NewsCache.invalidate()
                val cached = NewsCache.get(cacheKey)
                news = cached ?: fetchRssFeeds(sources, newsSettings.maxArticles).also {
                    NewsCache.put(cacheKey, it)
                }
            } catch (e: Exception) { error = e.message }
            isLoading = false
        }
    }

    LaunchedEffect(newsSettings) { load() }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Newspaper, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.page_news), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
            }
            Row {
                // v5: Switch to embedded app
                IconButton(onClick = onSwitchToApp) {
                    Icon(Icons.Rounded.PhoneAndroid, stringResource(R.string.news_app_mode), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { load(forceRefresh = true) }) {
                    Icon(Icons.Rounded.Refresh, stringResource(R.string.action_refresh), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.news_error_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { load(forceRefresh = true) }) { Text(stringResource(R.string.action_retry)) }
                }
            }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(news) { item ->
                    Card(Modifier.fillMaxWidth().clickable {
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.link))) } catch (_: Exception) {}
                    }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(item.source, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            Text(item.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (item.description.isNotBlank()) { Spacer(Modifier.height(4.dp)); Text(item.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis) }
                            Spacer(Modifier.height(6.dp))
                            Text(item.pubDate, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

private suspend fun fetchRssFeeds(sources: List<NewsSource>, maxArticles: Int): List<NewsItem> = withContext(Dispatchers.IO) {
    val all = mutableListOf<NewsItem>()
    for (source in sources) {
        try {
            val conn = URL(source.url).openConnection(); conn.connectTimeout = 5000; conn.readTimeout = 5000
            val stream = conn.getInputStream()
            val parser = XmlPullParserFactory.newInstance().newPullParser(); parser.setInput(stream, "UTF-8")
            var ev = parser.eventType; var inItem = false; var title = ""; var desc = ""; var link = ""; var pubDate = ""; var tag = ""
            while (ev != XmlPullParser.END_DOCUMENT) {
                when (ev) {
                    XmlPullParser.START_TAG -> { tag = parser.name ?: ""; if (tag == "item" || tag == "entry") { inItem = true; title = ""; desc = ""; link = ""; pubDate = "" } }
                    XmlPullParser.TEXT -> if (inItem) { val t = parser.text?.trim() ?: ""; when (tag) { "title" -> title = t; "description","summary","content" -> desc = t.replace(Regex("<[^>]*>"), "").replace("&nbsp;"," ").trim(); "link" -> link = t; "pubDate","published","updated" -> pubDate = t } }
                    XmlPullParser.END_TAG -> { if ((parser.name == "item" || parser.name == "entry") && inItem) { inItem = false; if (title.isNotBlank()) all.add(NewsItem(title, desc, link, pubDate, source.name)) }; tag = "" }
                }
                ev = parser.next()
            }
            stream.close()
        } catch (_: Exception) {}
    }
    all.take(maxArticles)
}
