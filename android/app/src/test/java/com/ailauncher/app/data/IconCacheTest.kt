package com.ailauncher.app.data

import android.app.Application
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Exercises the disk-persistence round trip added in v9.3 — see IconCache's
 * class kdoc for why the write is async while the read (getOrLoad) stays
 * synchronous. ioScope is swapped for an UnconfinedTestDispatcher-backed scope
 * so the fire-and-forget disk write completes before the test's next
 * assertion instead of racing a real background thread.
 *
 * `application = Application::class` — same reasoning as SettingsRepositoryTest:
 * avoids booting the real @HiltAndroidApp AILauncherApp under Robolectric.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class IconCacheTest {

    private fun newCache(): IconCache {
        val context = ApplicationProvider.getApplicationContext<Application>()
        return IconCache(context).apply { ioScope = CoroutineScope(UnconfinedTestDispatcher()) }
    }

    private fun testDrawable() = BitmapDrawable(
        ApplicationProvider.getApplicationContext<Application>().resources,
        Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
    )

    @Test
    fun `preloadFromDisk restores an icon written by a prior instance's getOrLoad`() = runTest {
        newCache().getOrLoad("com.example.app") { testDrawable() }

        // Simulate a process restart: a fresh instance, same on-disk cache dir.
        val restarted = newCache()
        restarted.preloadFromDisk(listOf("com.example.app"))

        var loaderCalled = false
        val result = restarted.getOrLoad("com.example.app") { loaderCalled = true; testDrawable() }

        assertNotNull(result)
        assertFalse("preload should have warmed the memory cache; loader must not run", loaderCalled)
    }

    @Test
    fun `preloadFromDisk is a no-op for a key nothing was ever written for`() = runTest {
        val cache = newCache()
        cache.preloadFromDisk(listOf("com.never.written"))

        var loaderCalled = false
        cache.getOrLoad("com.never.written") { loaderCalled = true; testDrawable() }

        assertTrue("loader should still run — disk had nothing to warm from", loaderCalled)
    }

    @Test
    fun `invalidate deletes the disk file so a later preload finds nothing`() = runTest {
        val first = newCache()
        first.getOrLoad("com.example.app") { testDrawable() }
        first.invalidate("com.example.app")

        val restarted = newCache()
        restarted.preloadFromDisk(listOf("com.example.app"))

        var loaderCalled = false
        restarted.getOrLoad("com.example.app") { loaderCalled = true; testDrawable() }

        assertTrue("invalidate should have removed the disk file too", loaderCalled)
    }
}
