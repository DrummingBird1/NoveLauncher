package com.ailauncher.app.data.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import kotlin.math.max

/**
 * v10: wallpapers picked from the gallery are copied into app-private storage
 * (filesDir/wallpapers), downscaled to the screen. Keeping our own copy means
 * the wallpaper survives the original being deleted or moved, and no storage
 * permission is needed — the system Photo Picker grants one-off read access.
 *
 * Files are referenced by absolute path from WallpaperEntry.uri. A settings
 * backup restored on another device keeps the entry but not the image (the
 * file isn't part of the export); WallpaperLayer then just draws nothing.
 */
object WallpaperStore {
    private fun dir(context: Context) = File(context.filesDir, "wallpapers").apply { mkdirs() }

    /** Copies + downscales [source]; returns the stored file's path, or null on failure. */
    suspend fun import(
        context: Context,
        source: Uri,
        id: String,
    ): String? =
        withContext(Dispatchers.IO) {
            try {
                val metrics = context.resources.displayMetrics
                val target = max(metrics.widthPixels, metrics.heightPixels).coerceAtLeast(1080)
                val bitmap = decodeScaled(context, source, target) ?: return@withContext null
                val file = File(dir(context), "$id.jpg")
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                bitmap.recycle()
                file.absolutePath
            } catch (e: Exception) {
                Timber.w(e, "Wallpaper import failed")
                null
            }
        }

    /** Loads a stored wallpaper for drawing. Call off the main thread. */
    fun load(path: String): Bitmap? =
        try {
            if (File(path).isFile) BitmapFactory.decodeFile(path) else null
        } catch (e: Exception) {
            Timber.w(e, "Wallpaper load failed")
            null
        }

    fun delete(path: String) {
        if (path.isBlank()) return
        val file = File(path)
        // Only ever delete inside our own folder.
        if (file.parentFile?.name == "wallpapers") file.delete()
    }

    private fun decodeScaled(
        context: Context,
        uri: Uri,
        maxSide: Int,
    ): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val src = ImageDecoder.createSource(context.contentResolver, uri)
            return ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                val longest = max(info.size.width, info.size.height)
                if (longest > maxSide) {
                    val ratio = maxSide.toFloat() / longest
                    decoder.setTargetSize((info.size.width * ratio).toInt(), (info.size.height * ratio).toInt())
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }
}
