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
    /**
     * True when the reading is lower than the previous one without a reboot. The system can replay an
     * old sensor value when a listener registers; such a reading carries no information and must not
     * replace the stored one.
     */
    val isStale: Boolean = false,
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

        // Only the boot time decides whether the counter restarted. A lower value on the same boot is
        // an out-of-date reading, not a reboot (see StepDelta.isStale).
        val rebooted = abs(current.bootTimeMillis - previous.bootTimeMillis) > BOOT_TOLERANCE_MS
        if (!rebooted && current.counter < previous.counter) {
            return StepDelta(0, false, previous.takenAtMillis, current.takenAtMillis, isStale = true)
        }

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
