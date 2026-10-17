package com.ailauncher.app.domain.ranking

import kotlin.math.pow

/**
 * v10: turns "shown in Recommended now" (impressions) vs. "tapped there" (taps)
 * into a ranking adjustment. Pure math — unit-tested on the JVM.
 *
 * - Tap-through rate is smoothed with a Beta prior worth [PRIOR_IMPRESSIONS]
 *   impressions at [PRIOR_CTR], so a single tap or a single ignore barely moves
 *   anything; it takes a pattern.
 * - Both counts decay with a [HALF_LIFE_DAYS] half-life, so the ranking follows
 *   habits as they change instead of being anchored to last year's.
 * - The resulting lift is clamped, so feedback can reorder suggestions but never
 *   override what the usage signals say outright: a consistently ignored app
 *   drops to ~85% of its score, a consistently tapped one rises up to ~135%.
 */
object FeedbackScoring {
    const val PRIOR_CTR = 0.25f
    const val PRIOR_IMPRESSIONS = 4f
    const val HALF_LIFE_DAYS = 14.0
    private const val MIN_LIFT = -0.6f
    private const val MAX_LIFT = 1.4f
    private const val STRENGTH = 0.25f

    fun decay(
        value: Float,
        elapsedMillis: Long,
    ): Float {
        if (elapsedMillis <= 0) return value
        val days = elapsedMillis / 86_400_000.0
        return (value * 0.5.pow(days / HALF_LIFE_DAYS)).toFloat()
    }

    fun smoothedCtr(
        taps: Float,
        impressions: Float,
    ): Float = (taps + PRIOR_CTR * PRIOR_IMPRESSIONS) / (impressions + PRIOR_IMPRESSIONS)

    /** Multiplies [score] (0..1) by the feedback lift; result stays in 0..1. */
    fun adjust(
        score: Float,
        taps: Float,
        impressions: Float,
    ): Float {
        val lift = ((smoothedCtr(taps, impressions) - PRIOR_CTR) / PRIOR_CTR).coerceIn(MIN_LIFT, MAX_LIFT)
        return (score * (1f + STRENGTH * lift)).coerceIn(0f, 1f)
    }
}
