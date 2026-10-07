package com.starmist.core.steps

import java.time.LocalDate
import java.time.ZoneId

/** What must survive between two readings. */
data class LedgerState(
    val lastSnapshot: CounterSnapshot? = null,
    /** Fractional remainder from applying the correction factor. */
    val carry: Double = 0.0,
)

data class LedgerUpdate(
    val state: LedgerState,
    /** Uncorrected steps to add per day. */
    val rawByDay: Map<LocalDate, Long>,
    /** Corrected steps to add per day. */
    val scaledByDay: Map<LocalDate, Long>,
    val rebooted: Boolean,
    val isBaseline: Boolean,
)

/**
 * Turns a new counter reading into per-day step increments. Pure: it only maps (state, reading) to
 * (state, increments), so the Android layer just persists what comes back.
 */
class StepLedger(private val zone: ZoneId) {

    fun process(state: LedgerState, reading: CounterSnapshot, multiplier: Double): LedgerUpdate {
        val delta = StepDeltaCalculator.compute(state.lastSnapshot, reading)
        val rawByDay = DaySplitter.split(delta.steps, delta.fromMillis, delta.toMillis, zone)

        var carry = state.carry
        val scaledByDay = LinkedHashMap<LocalDate, Long>()
        // Oldest day first so the carry flows forward in time.
        for ((day, raw) in rawByDay.toSortedMap()) {
            val scaled = StepScaler.scale(raw, multiplier, carry)
            carry = scaled.carry
            if (scaled.steps > 0) scaledByDay[day] = scaled.steps
        }

        return LedgerUpdate(
            state = LedgerState(lastSnapshot = reading, carry = carry),
            rawByDay = rawByDay,
            scaledByDay = scaledByDay,
            rebooted = delta.rebooted,
            isBaseline = delta.isBaseline,
        )
    }
}
