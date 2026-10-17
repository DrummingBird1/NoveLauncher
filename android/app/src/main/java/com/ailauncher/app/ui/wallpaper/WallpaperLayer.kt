package com.ailauncher.app.ui.wallpaper

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.ailauncher.app.data.wallpaper.WallpaperStore
import com.ailauncher.app.domain.models.BuiltInWallpaper
import com.ailauncher.app.domain.models.WallpaperEntry
import com.ailauncher.app.domain.models.WallpaperSettings
import com.ailauncher.app.domain.wallpaper.WallpaperRules
import com.ailauncher.app.ui.LocalReduceMotion
import com.ailauncher.app.ui.theme.parseHexColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * v10: draws the launcher's own wallpaper behind every page — a gallery image
 * or a built-in gradient, chosen per [WallpaperRules.select] and adjusted with
 * the entry's brightness/contrast/saturation/warmth matrix, blur (Android 12+,
 * where the GPU blur exists), opacity and vignette. [dimmed] darkens it while
 * a page other than Home is showing ("Dim on scroll").
 *
 * Draws nothing when there's no entry or in LIVE mode — LauncherRoot then shows
 * the theme background or lets the system wallpaper through.
 */
@Composable
fun WallpaperLayer(
    settings: WallpaperSettings,
    pageId: String,
    dimmed: Boolean,
) {
    // HOURLY/DAILY change on their own; re-select once a minute.
    var now by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(settings.mode) {
        while (true) {
            delay(60_000)
            now = Calendar.getInstance()
        }
    }
    val entry =
        remember(settings, pageId, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.DAY_OF_YEAR)) {
            WallpaperRules.select(
                settings,
                pageId,
                hour = now.get(Calendar.HOUR_OF_DAY),
                dayOfWeek = now.get(Calendar.DAY_OF_WEEK),
                dayOfYear = now.get(Calendar.DAY_OF_YEAR),
            )
        } ?: return

    val reduceMotion = LocalReduceMotion.current
    val dim by animateFloatAsState(if (dimmed && settings.dimOnScroll) 0.35f else 0f, label = "wallpaperDim")

    Box(Modifier.fillMaxSize()) {
        WallpaperImage(entry, Modifier.fillMaxSize().alpha(entry.opacity.coerceIn(0f, 1f)))
        if (entry.vignette > 0f) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        0.55f to Color.Transparent,
                        1f to Color.Black.copy(alpha = entry.vignette.coerceIn(0f, 1f)),
                    ),
                ),
            )
        }
        if (dim > 0f) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (reduceMotion) 0.35f else dim)))
    }
}

/** One wallpaper entry, adjusted. Also used for the thumbnails in Settings → Wallpapers. */
@Composable
fun WallpaperImage(
    entry: WallpaperEntry,
    modifier: Modifier,
) {
    val filter =
        remember(entry.brightness, entry.contrast, entry.saturation, entry.warmth) {
            ColorFilter.colorMatrix(ColorMatrix(WallpaperRules.colorMatrix(entry)))
        }
    val blurred = if (entry.blur > 0f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) modifier.blur(entry.blur.dp) else modifier

    if (entry.isBuiltIn) {
        val colors = BuiltInWallpaper.ALL.firstOrNull { it.id == entry.builtInId }?.colors ?: return
        val brush = Brush.linearGradient(colors.map { parseHexColor(it) })
        // A gradient has no pixels to filter; brightness still applies as a black overlay.
        Box(blurred.background(brush)) {
            val darken = 1f - entry.brightness.coerceIn(0f, 1f)
            if (darken > 0f) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = darken)))
        }
        return
    }

    val bitmap by produceState<ImageBitmap?>(null, entry.uri) {
        value = withContext(Dispatchers.IO) { WallpaperStore.load(entry.uri)?.asImageBitmap() }
    }
    bitmap?.let {
        Image(bitmap = it, contentDescription = null, contentScale = ContentScale.Crop, colorFilter = filter, modifier = blurred)
    }
}
