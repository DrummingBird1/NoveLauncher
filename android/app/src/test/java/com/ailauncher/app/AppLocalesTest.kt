package com.ailauncher.app

import android.app.Activity
import android.app.Application
import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** An Activity wired the way LauncherActivity and SettingsActivity are. */
class LocaleTestActivity : Activity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocales.attach(this, newBase))
    }
}

/**
 * Below Android 13 the language picker goes through AppLocales' own path (Pitfall #12):
 * the stored tag is applied in attachBaseContext, and an Activity created before a
 * newer pick recreates itself on resume.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32], application = Application::class)
class AppLocalesTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After fun followSystemAgain() = AppLocales.apply(context, "")

    @Test fun `a picked language reaches a wrapped context`() {
        AppLocales.apply(context, "fr")

        val wrapped = AppLocales.wrap(context)

        assertEquals(
            "fr",
            wrapped.resources.configuration.locales[0]
                .language,
        )
        assertEquals("Utiliser ma position", wrapped.getString(R.string.location_allow))
        assertEquals("fr", AppLocales.locale(context).language)
    }

    @Test fun `a right-to-left language flips the layout direction`() {
        AppLocales.apply(context, "ar")

        assertEquals(
            View.LAYOUT_DIRECTION_RTL,
            AppLocales
                .wrap(context)
                .resources.configuration.layoutDirection,
        )
    }

    @Test fun `following the system leaves the context alone`() {
        AppLocales.apply(context, "")

        assertSame(context, AppLocales.wrap(context))
    }

    @Test fun `an activity is created in the picked language`() {
        AppLocales.apply(context, "fr")

        val activity = Robolectric.buildActivity(LocaleTestActivity::class.java).setup().get()

        assertEquals("Utiliser ma position", activity.getString(R.string.location_allow))
        assertFalse(AppLocales.onResume(activity))
    }

    @Test fun `an activity created before a new pick is recreated`() {
        AppLocales.apply(context, "fr")
        val activity = Robolectric.buildActivity(LocaleTestActivity::class.java).setup().get()

        AppLocales.apply(context, "de")

        assertTrue(AppLocales.onResume(activity))
    }

    @Test fun `a right-to-left pick sets the window's layout direction`() {
        AppLocales.apply(context, "he")

        val activity = Robolectric.buildActivity(LocaleTestActivity::class.java).setup().get()
        AppLocales.onResume(activity)

        assertEquals(View.LAYOUT_DIRECTION_RTL, activity.window.decorView.layoutDirection)
    }

    @Test fun `following the system again resets the window's layout direction`() {
        AppLocales.apply(context, "he")
        val activity = Robolectric.buildActivity(LocaleTestActivity::class.java).setup().get()
        AppLocales.onResume(activity)

        AppLocales.apply(context, "")
        val next = Robolectric.buildActivity(LocaleTestActivity::class.java).setup().get()
        // A recreated Activity can inherit the old window, direction and all.
        next.window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        AppLocales.onResume(next)

        assertEquals(View.LAYOUT_DIRECTION_LTR, next.window.decorView.layoutDirection)
    }
}

/** On 13+ the framework LocaleManager does the work; AppLocales stays out of the way. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class AppLocalesModernTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun `contexts are never wrapped and activities never recreated by hand`() {
        val activity = Robolectric.buildActivity(LocaleTestActivity::class.java).setup().get()

        assertSame(context, AppLocales.wrap(context))
        assertFalse(AppLocales.onResume(activity))
    }
}
