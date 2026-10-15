package com.ailauncher.app.domain.ranking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackScoringTest {

    @Test
    fun `no feedback leaves the score unchanged`() {
        assertEquals(0.6f, FeedbackScoring.adjust(0.6f, taps = 0f, impressions = 0f), 0.0001f)
    }

    @Test
    fun `a single interaction barely moves anything`() {
        val ignoredOnce = FeedbackScoring.adjust(0.6f, taps = 0f, impressions = 1f)
        assertTrue(ignoredOnce < 0.6f && ignoredOnce > 0.55f)
    }

    @Test
    fun `consistently ignored suggestions sink, consistently tapped ones rise`() {
        val ignored = FeedbackScoring.adjust(0.6f, taps = 0f, impressions = 40f)
        val tapped = FeedbackScoring.adjust(0.6f, taps = 30f, impressions = 40f)
        assertTrue(ignored < 0.55f)
        assertTrue(tapped > 0.7f)
    }

    @Test
    fun `the adjustment is bounded`() {
        // Lift is clamped to [-60%, +140%] at 25% strength → score × [0.85, 1.35].
        assertEquals(0.6f * 0.85f, FeedbackScoring.adjust(0.6f, taps = 0f, impressions = 10_000f), 0.001f)
        assertEquals(0.6f * 1.35f, FeedbackScoring.adjust(0.6f, taps = 10_000f, impressions = 10_000f), 0.001f)
        assertEquals(1f, FeedbackScoring.adjust(0.95f, taps = 10_000f, impressions = 10_000f), 0.0001f)
    }

    @Test
    fun `counts halve every half-life`() {
        val halfLifeMs = (FeedbackScoring.HALF_LIFE_DAYS * 86_400_000).toLong()
        assertEquals(5f, FeedbackScoring.decay(10f, halfLifeMs), 0.001f)
        assertEquals(2.5f, FeedbackScoring.decay(10f, 2 * halfLifeMs), 0.001f)
        assertEquals(10f, FeedbackScoring.decay(10f, 0), 0.0001f)
    }
}
