package com.starmist.core.steps

import kotlin.math.floor

/** Applies the user's correction factor without losing fractions between readings. */
object StepScaler {
    const val MIN_MULTIPLIER = 0.70
    const val MAX_MULTIPLIER = 1.50

    data class Scaled(val steps: Long, val carry: Double)

    fun clampMultiplier(value: Double): Double =
        if (value.isNaN()) 1.0 else value.coerceIn(MIN_MULTIPLIER, MAX_MULTIPLIER)

    /**
     * @param carry fractional remainder left over from the previous call, in [0, 1)
     * @return whole scaled steps and the new remainder
     */
    fun scale(rawSteps: Long, multiplier: Double, carry: Double): Scaled {
        val exact = rawSteps * clampMultiplier(multiplier) + carry
        val whole = floor(exact)
        return Scaled(whole.toLong(), exact - whole)
    }
}
