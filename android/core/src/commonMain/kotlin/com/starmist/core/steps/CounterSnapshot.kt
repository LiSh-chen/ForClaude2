package com.starmist.core.steps

/**
 * One reading of the hardware step counter.
 *
 * @param counter steps accumulated since the device last booted
 * @param takenAtMillis wall-clock time of the reading (epoch millis)
 * @param bootTimeMillis wall-clock time the device booted, i.e. `now - elapsedRealtime`
 */
data class CounterSnapshot(
    val counter: Long,
    val takenAtMillis: Long,
    val bootTimeMillis: Long,
)
