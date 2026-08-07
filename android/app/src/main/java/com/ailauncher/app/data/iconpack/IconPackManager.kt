package com.ailauncher.app.data.iconpack

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStreamReader

/**
 * v6 feature #4: Icon pack support (Nova/Lawnchair/ADW compatible).
 * Reads appfilter.xml from installed icon packs and maps component names to icon resources.
 */
data class IconPackInfo(val packageName: String, val label: String, val icon: Drawable?)

@javax.inject.Singleton
class IconPackManager @javax.inject.Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {

    private val loadedMappings = mutableMapOf<String, Map<String, String>>()

    /**
     * Find all installed launcher icon packs via common intent filters.
     */
    fun getAvailableIconPacks(): List<IconPackInfo> {
        val pm = context.packageManager
        val intents = listOf(
            "org.adw.launcher.THEMES",
            "com.gau.go.launcherex.theme",
            "com.anddoes.launcher.THEME",
            "com.novalauncher.THEME",
            "ch.deletescape.lawnchair.ICONPACK"
        )
        val found = mutableMapOf<String, IconPackInfo>()
        intents.forEach { action ->
            try {
                val intent = Intent(action)
                pm.queryIntentActivities(intent, PackageManager.GET_META_DATA).forEach { ri ->
                    val pkg = ri.activityInfo?.packageName ?: return@forEach
                    if (pkg !in found) {
                        val label = ri.loadLabel(pm).toString()
                        val icon = ri.loadIcon(pm)
                        found[pkg] = IconPackInfo(pkg, label, icon)
                    }
                }
            } catch (_: Exception) {}
        }
        return found.values.sortedBy { it.label }
    }

    /**
     * Load appfilter.xml from the icon pack and cache the component → drawable mapping.
     */
    fun loadIconPack(packageName: String): Map<String, String> {
        loadedMappings[packageName]?.let { return it }

        val mapping = mutableMapOf<String, String>()
        try {
            val pkgContext = context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY)
            val res = pkgContext.resources
            val xmlId = res.getIdentifier("appfilter", "xml", packageName)

            val parser = if (xmlId != 0) res.getXml(xmlId)
            else res.assets.open("appfilter.xml").let {
                XmlPullParserFactory.newInstance().newPullParser().apply {
                    setInput(InputStreamReader(it, "UTF-8"))
                }
            }

            var ev = parser.eventType
            while (ev != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component")
                    val drawable = parser.getAttributeValue(null, "drawable")
                    if (component != null && drawable != null) {
                        mapping[component] = drawable
                    }
                }
                ev = parser.next()
            }
        } catch (_: Exception) {}

        loadedMappings[packageName] = mapping
        return mapping
    }

    /**
     * Resolve a drawable for a given app from a loaded icon pack.
     * Returns null if the icon pack doesn't have a custom icon for this app.
     */
    fun getIconForApp(iconPackPkg: String, appComponent: String): Drawable? {
        val mapping = loadIconPack(iconPackPkg)
        val drawableName = mapping[appComponent] ?: return null
        return try {
            val pkgContext = context.createPackageContext(iconPackPkg, Context.CONTEXT_IGNORE_SECURITY)
            val resId = pkgContext.resources.getIdentifier(drawableName, "drawable", iconPackPkg)
            if (resId != 0) pkgContext.resources.getDrawable(resId, pkgContext.theme) else null
        } catch (_: Exception) { null }
    }

    fun clearCache() { loadedMappings.clear() }
}
