package com.ailauncher.app.data

import android.graphics.drawable.Drawable
import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * v9: process-lifetime memory cache for app launcher icons.
 *
 * Why: HomeScreen/AppsScreen/PersonalZoneScreen all called
 *      `icon.toBitmap(128, 128).asImageBitmap()` inside `remember(packageName)`.
 *      `remember` is scoped to a Composition, so navigating between pages, scrolling
 *      a row out of the LazyGrid viewport, or rotating the device discards the
 *      bitmap and forces a fresh decode + Bitmap allocation on the next show.
 *      On a 200-app drawer at 128×128 ARGB_8888 that's ~12 MB of redundant work
 *      *per page swipe*.
 *
 * Sizing: 8 MB cap is comfortable on every supported device (minSdk 26 → ~3 GB RAM
 *         floor). Eviction is LRU, so the visible drawer + dock + recommended row
 *         stay resident; rarely-seen apps drop out.
 *
 * Keying: the package name alone isn't enough — a user-installed icon pack swap
 *         changes the rendered icon while the package name is unchanged. Callers
 *         should pass the IconPackManager-resolved Drawable identity as the loader,
 *         and invoke [invalidateAll] after icon-pack changes.
 */
@Singleton
class IconCache @Inject constructor() {

    // Size in *bytes*; bitmap.allocationByteCount feeds sizeOf below.
    private val cache = object : LruCache<String, ImageBitmap>(MAX_BYTES) {
        override fun sizeOf(key: String, value: ImageBitmap): Int {
            // ImageBitmap.asAndroidBitmap is the cheap path; allocationByteCount
            // is the actual RAM footprint (height × rowBytes).
            return runCatching { value.asAndroidBitmap().allocationByteCount }.getOrDefault(1)
        }
    }

    /**
     * Returns the cached ImageBitmap for [key], or computes it via [loader] and
     * stores it. [loader] runs synchronously — pass a cheap drawable→bitmap
     * conversion. If [loader] returns null the cache is not populated.
     */
    fun getOrLoad(key: String, loader: () -> Drawable?): ImageBitmap? {
        cache.get(key)?.let { return it }
        val drawable = loader() ?: return null
        val bitmap = runCatching { drawable.toBitmap(ICON_PX, ICON_PX).asImageBitmap() }
            .getOrNull() ?: return null
        cache.put(key, bitmap)
        return bitmap
    }

    /** Drop everything. Call after an icon-pack swap or theme change that affects icons. */
    fun invalidateAll() { cache.evictAll() }

    /** Drop a specific package's icon — e.g. after an app update. */
    fun invalidate(key: String) { cache.remove(key) }

    companion object {
        private const val MAX_BYTES = 8 * 1024 * 1024  // 8 MB
        private const val ICON_PX = 128
    }
}
