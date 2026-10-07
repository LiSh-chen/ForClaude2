package com.starmist.core.steps

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object DaySplitter {
    /** A span longer than this is not spread out; everything lands on the last day. */
    private const val MAX_SPREAD_DAYS = 400L

    /**
     * Distributes [steps] taken between [fromMillis] and [toMillis] over the calendar days the span
     * touches, proportionally to the time spent in each day. The parts always add up to [steps]
     * (largest-remainder rounding). Zero-length or reversed spans go entirely to the end day.
     */
    fun split(steps: Long, fromMillis: Long, toMillis: Long, zone: ZoneId): Map<LocalDate, Long> {
        require(steps >= 0) { "steps must not be negative" }
        val endDay = Instant.ofEpochMilli(toMillis).atZone(zone).toLocalDate()
        if (steps == 0L) return emptyMap()
        if (toMillis <= fromMillis) return mapOf(endDay to steps)

        val startDay = Instant.ofEpochMilli(fromMillis).atZone(zone).toLocalDate()
        if (startDay == endDay) return mapOf(endDay to steps)
        if (endDay.toEpochDay() - startDay.toEpochDay() > MAX_SPREAD_DAYS) return mapOf(endDay to steps)

        // Overlap of the span with each day, in millis. Using zone-aware day starts handles DST days.
        val days = ArrayList<LocalDate>()
        val overlaps = ArrayList<Long>()
        var day = startDay
        while (!day.isAfter(endDay)) {
            val dayStart = day.atStartOfDay(zone).toInstant().toEpochMilli()
            val dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val overlap = minOf(dayEnd, toMillis) - maxOf(dayStart, fromMillis)
            if (overlap > 0) {
                days += day
                overlaps += overlap
            }
            day = day.plusDays(1)
        }
        if (days.isEmpty()) return mapOf(endDay to steps)

        val total = overlaps.sum().toDouble()
        val shares = overlaps.map { steps * (it / total) }
        val floors = shares.map { it.toLong() }.toMutableList()
        var remaining = steps - floors.sum()
        // Hand the leftover steps to the days with the largest fractional parts.
        val order = shares.indices.sortedByDescending { shares[it] - floors[it] }
        var i = 0
        while (remaining > 0) {
            floors[order[i % order.size]] += 1
            remaining--
            i++
        }
        val result = LinkedHashMap<LocalDate, Long>()
        days.forEachIndexed { index, d -> if (floors[index] > 0) result[d] = floors[index] }
        return result
    }
}
