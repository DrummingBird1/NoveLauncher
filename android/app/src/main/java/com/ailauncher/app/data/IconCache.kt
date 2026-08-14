package com.ailauncher.app.data

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
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
 * Sizing: v9 hardcoded an 8 MB cap. v9.1 scales it to 1/8th of
 *         ActivityManager.getMemoryClass() instead — a low-RAM device (memoryClass
 *         ~ 96-128 MB) gets a smaller cache that won't pressure the rest of the app,
 *         while a high-RAM device (memoryClass 256-512+ MB) gets more headroom for
 *         a large app drawer. Clamped to [4 MB, 32 MB] so neither extreme is silly.
 *         Eviction is LRU, so the visible drawer + dock + recommended row stay
 *         resident; rarely-seen apps drop out.
 *
 * Keying: the package name alone isn't enough — a user-installed icon pack swap
 *         changes the rendered icon while the package name is unchanged. Callers
 *         should pass the IconPackManager-resolved Drawable identity as the loader,
 *         and invoke [invalidateAll] after icon-pack changes.
 *
 * v9.3: added a disk layer under context.cacheDir (OS-reclaimable, not user
 *       data) purely to warm the in-memory cache across process restarts —
 *       [getOrLoad] itself stays 100% synchronous and never touches disk, since
 *       it's called directly from Composable bodies (HomeScreen/AppsScreen)
 *       where a disk read would block the main thread (exactly what
 *       AILauncherApp's debug-only StrictMode.detectDiskReads() exists to
 *       catch). Instead: (1) a successful decode fires an async, best-effort
 *       write to disk via [ioScope], off the hot path entirely; (2) callers
 *       that want cold-start benefit call [preloadFromDisk] from a coroutine
 *       *before* the first composition that needs those icons — see
 *       LauncherViewModel.refresh().
 */
@Singleton
class IconCache @Inject constructor(@ApplicationContext context: Context) {

    private val maxBytes: Int = run {
        val memoryClassMb = (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)
            ?.memoryClass ?: DEFAULT_MEMORY_CLASS_MB
        ((memoryClassMb * 1024 * 1024) / MEMORY_CLASS_FRACTION).coerceIn(MIN_BYTES, MAX_BYTES_CAP)
    }

    // Size in *bytes*; bitmap.allocationByteCount feeds sizeOf below.
    private val cache = object : LruCache<String, ImageBitmap>(maxBytes) {
        override fun sizeOf(key: String, value: ImageBitmap): Int {
            // ImageBitmap.asAndroidBitmap is the cheap path; allocationByteCount
            // is the actual RAM footprint (height × rowBytes).
            return runCatching { value.asAndroidBitmap().allocationByteCount }.getOrDefault(1)
        }
    }

    private val diskCacheDir = File(context.cacheDir, "icon_cache")

    // internal + var so a Robolectric test can swap in a scope backed by a
    // TestDispatcher and deterministically await the async disk write instead
    // of racing a real background thread.
    internal var ioScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
        persistToDiskAsync(key, bitmap)
        return bitmap
    }

    /**
     * Warms the in-memory cache from disk for any of [keys] not already
     * resident. Must be called from a coroutine (suspend), never from a
     * Composable body — see class kdoc. A miss or decode failure for a given
     * key is silently skipped; [getOrLoad] falls back to its normal loader
     * path on the next real render regardless.
     */
    suspend fun preloadFromDisk(keys: List<String>) = withContext(Dispatchers.IO) {
        if (!diskCacheDir.isDirectory) return@withContext
        for (key in keys) {
            if (cache.get(key) != null) continue
            val file = diskFile(key)
            if (!file.isFile) continue
            val bitmap = runCatching { BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() }
                .getOrNull() ?: continue
            cache.put(key, bitmap)
        }
    }

    private fun persistToDiskAsync(key: String, bitmap: ImageBitmap) {
        ioScope.launch {
            runCatching {
                if (!diskCacheDir.isDirectory) diskCacheDir.mkdirs()
                FileOutputStream(diskFile(key)).use { out ->
                    bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }.onFailure { Timber.w(it, "Icon disk-cache write failed for %s", key) }
        }
    }

    // packageName is already filesystem-safe (Java package identifier syntax),
    // but a user-supplied icon-pack key theoretically could carry a "/" —
    // hashCode() sidesteps that instead of trying to sanitize every input.
    private fun diskFile(key: String): File = File(diskCacheDir, "${key.hashCode()}.png")

    /** Drop everything. Call after an icon-pack swap or theme change that affects icons. */
    fun invalidateAll() {
        cache.evictAll()
        ioScope.launch { runCatching { diskCacheDir.listFiles()?.forEach { it.delete() } } }
    }

    /** Drop a specific package's icon — e.g. after an app update. */
    fun invalidate(key: String) {
        cache.remove(key)
        ioScope.launch { runCatching { diskFile(key).delete() } }
    }

    companion object {
        private const val MEMORY_CLASS_FRACTION = 8
        private const val DEFAULT_MEMORY_CLASS_MB = 64  // conservative fallback if ActivityManager is unavailable
        private const val MIN_BYTES = 4 * 1024 * 1024
        private const val MAX_BYTES_CAP = 32 * 1024 * 1024
        private const val ICON_PX = 128
    }
}
