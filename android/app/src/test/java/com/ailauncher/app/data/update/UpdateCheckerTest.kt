package com.ailauncher.app.data.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    @Test
    fun `numeric comparison, not string comparison`() {
        assertTrue(UpdateChecker.isNewer("10.0.0", "9.3.0"))
        assertTrue(UpdateChecker.isNewer("9.10.0", "9.9.9"))
        assertTrue(UpdateChecker.isNewer("10.0.1", "10.0.0"))
        assertFalse(UpdateChecker.isNewer("9.3.0", "10.0.0"))
    }

    @Test
    fun `equal versions are not an update`() {
        assertFalse(UpdateChecker.isNewer("10.0.0", "10.0.0"))
        assertFalse(UpdateChecker.isNewer("10.0", "10.0.0"))
    }

    @Test
    fun `pre-release suffixes are ignored`() {
        assertTrue(UpdateChecker.isNewer("10.1.0-beta", "10.0.0"))
        assertFalse(UpdateChecker.isNewer("10.0.0-rc1", "10.0.0"))
    }
}
