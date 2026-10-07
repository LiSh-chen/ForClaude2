package com.starmist.core.sensor

import kotlin.math.sqrt

enum class FilterLevel { LOOSE, STANDARD, STRICT }

/**
 * Fallback step detector for devices without a hardware step counter. Works on raw accelerometer
 * samples; the caller feeds them in and reads back newly confirmed steps.
 *
 * To reject shaking and one-off bumps, a step only counts once [Config.minConsecutive] peaks in a
 * row arrived at a plausible walking rhythm. The buffered steps are then credited at once.
 */
class AccelStepDetector(private val config: Config = Config.of(FilterLevel.STANDARD)) {

    data class Config(
        /** Minimum smoothed acceleration peak (m/s²) above the gravity baseline. */
        val peakThreshold: Double,
        val minConsecutive: Int,
        /** Peaks closer than this are vibration, not steps (≈ 3.7 steps/s). */
        val minIntervalMs: Long = 270,
        /** A longer gap ends the walking sequence (≈ 0.5 steps/s). */
        val maxIntervalMs: Long = 2000,
    ) {
        companion object {
            fun of(
                level: FilterLevel,
                threshold: Double? = null,
                minConsecutive: Int? = null,
            ): Config {
                val base = when (level) {
                    FilterLevel.LOOSE -> Config(1.0, 4)
                    FilterLevel.STANDARD -> Config(1.5, 6)
                    FilterLevel.STRICT -> Config(2.2, 8)
                }
                return base.copy(
                    peakThreshold = threshold ?: base.peakThreshold,
                    minConsecutive = (minConsecutive ?: base.minConsecutive).coerceAtLeast(1),
                )
            }
        }
    }

    var totalSteps: Long = 0
        private set

    private var baseline = 0.0
    private var smoothed = 0.0
    private var initialized = false

    private var prev2 = 0.0
    private var prev1 = 0.0
    private var prev1Time = 0L
    private var samplesSeen = 0

    private var lastPeakTime: Long? = null
    private var sequence = 0

    /** @return steps newly confirmed by this sample (usually 0) */
    fun onSample(timestampMs: Long, x: Double, y: Double, z: Double): Int {
        val magnitude = sqrt(x * x + y * y + z * z)
        if (!initialized) {
            baseline = magnitude
            smoothed = 0.0
            initialized = true
        }
        // Slowly tracks gravity (and any constant offset), leaving only the motion.
        baseline += BASELINE_ALPHA * (magnitude - baseline)
        smoothed += SMOOTHING_ALPHA * ((magnitude - baseline) - smoothed)

        val current = smoothed
        var confirmed = 0
        // prev1 is a peak when it is higher than its left neighbour and not lower than its right one.
        if (samplesSeen >= 2 && prev1 > prev2 && prev1 >= current && prev1 > config.peakThreshold) {
            confirmed = registerPeak(prev1Time)
        }
        prev2 = prev1
        prev1 = current
        prev1Time = timestampMs
        samplesSeen++

        totalSteps += confirmed
        return confirmed
    }

    fun reset() {
        totalSteps = 0
        initialized = false
        samplesSeen = 0
        lastPeakTime = null
        sequence = 0
    }

    private fun registerPeak(time: Long): Int {
        val last = lastPeakTime
        if (last == null) {
            sequence = 1
        } else {
            val gap = time - last
            sequence = when {
                gap < config.minIntervalMs -> 0 // vibration: invalidate the sequence
                gap > config.maxIntervalMs -> 1 // pause: a new sequence starts with this peak
                else -> sequence + 1
            }
        }
        lastPeakTime = time

        return when {
            sequence == config.minConsecutive -> config.minConsecutive
            sequence > config.minConsecutive -> 1
            else -> 0
        }
    }

    private companion object {
        const val BASELINE_ALPHA = 0.02
        const val SMOOTHING_ALPHA = 0.3
    }
}
