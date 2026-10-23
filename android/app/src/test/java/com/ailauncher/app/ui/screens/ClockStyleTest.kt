package com.ailauncher.app.ui.screens

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ailauncher.app.domain.models.ClockSettings
import com.ailauncher.app.domain.models.ClockStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClockStyleTest {
    @Test fun `digital follows the 24-hour and seconds switches`() {
        assertEquals("HH:mm", clockTimePattern(ClockSettings(use24Hour = true)))
        assertEquals("HH:mm:ss", clockTimePattern(ClockSettings(showSeconds = true)))
        assertEquals("hh:mm:ss a", clockTimePattern(ClockSettings(use24Hour = false, showSeconds = true)))
    }

    @Test fun `minimal never shows seconds or AM-PM`() {
        val minimal = ClockSettings(clockStyle = ClockStyle.MINIMAL, showSeconds = true)
        assertEquals("HH:mm", clockTimePattern(minimal))
        assertEquals("h:mm", clockTimePattern(minimal.copy(use24Hour = false)))
    }

    @Test fun `minimal is smaller and light, digital is unchanged`() {
        val digital = ClockSettings(fontSize = 60)
        assertEquals(60.sp, clockTimeFontSize(digital))
        assertNull(clockTimeFontWeight(digital))
        val minimal = digital.copy(clockStyle = ClockStyle.MINIMAL)
        assertEquals(48.sp, clockTimeFontSize(minimal))
        assertEquals(FontWeight.Light, clockTimeFontWeight(minimal))
    }
}
