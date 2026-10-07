package com.starmist.core.steps

import kotlin.math.abs

/** Result of comparing two counter readings. */
data class StepDelta(
    /** Steps taken between the readings (never negative). */
    val steps: Long,
    /** True when the counter was reset by a reboot between the readings. */
    val rebooted: Boolean,
    /** Wall-clock start of the interval the steps belong to. */
    val fromMillis: Long,
    /** Wall-clock end of the interval the steps belong to. */
    val toMillis: Long,
    /** True when [steps] is 0 only because there was no earlier reading to compare against. */
    val isBaseline: Boolean = false,
)

object StepDeltaCalculator {
    /**
     * Boot time is derived from two clocks and drifts by a few seconds (NTP, suspend), so only a
     * difference larger than this is treated as a reboot.
     */
    const val BOOT_TOLERANCE_MS = 2 * 60 * 1000L

    fun compute(previous: CounterSnapshot?, current: CounterSnapshot): StepDelta {
        if (previous == null) {
            // Steps since boot are not "today's" steps; the first reading is only a baseline.
            return StepDelta(0, false, current.takenAtMillis, current.takenAtMillis, isBaseline = true)
        }

        val bootChanged = abs(current.bootTimeMillis - previous.bootTimeMillis) > BOOT_TOLERANCE_MS
        val counterWentBack = current.counter < previous.counter
        val rebooted = bootChanged || counterWentBack

        return if (rebooted) {
            // The counter restarted from 0 at boot, so everything on it belongs to the time since
            // boot (but never before the previous reading, to avoid double counting on clock jumps).
            val from = maxOf(previous.takenAtMillis, current.bootTimeMillis).coerceAtMost(current.takenAtMillis)
            StepDelta(current.counter, true, from, current.takenAtMillis)
        } else {
            StepDelta(
                steps = current.counter - previous.counter,
                rebooted = false,
                fromMillis = minOf(previous.takenAtMillis, current.takenAtMillis),
                toMillis = current.takenAtMillis,
            )
        }
    }
}
