package com.starmist.core.stats

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

enum class Period { WEEK, MONTH, YEAR }

/** One bar in a chart. [start] is the first day the bar covers. */
data class Bucket(val start: LocalDate, val steps: Long)

data class PeriodSummary(
    val buckets: List<Bucket>,
    val totalSteps: Long,
    /** Average per day over the days that have already happened in the period. */
    val averagePerDay: Long,
    val bestDay: Pair<LocalDate, Long>?,
    val goalDays: Int,
    val rangeStart: LocalDate,
    val rangeEnd: LocalDate,
)

object Aggregator {

    fun rangeOf(anchor: LocalDate, period: Period): Pair<LocalDate, LocalDate> = when (period) {
        Period.WEEK -> {
            val start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            start to start.plusDays(6)
        }
        Period.MONTH -> {
            val ym = YearMonth.from(anchor)
            ym.atDay(1) to ym.atEndOfMonth()
        }
        Period.YEAR -> LocalDate.of(anchor.year, 1, 1) to LocalDate.of(anchor.year, 12, 31)
    }

    /** Anchor date of the neighbouring period; [direction] is -1 (earlier) or +1 (later). */
    fun shift(anchor: LocalDate, period: Period, direction: Int): LocalDate = when (period) {
        Period.WEEK -> anchor.plusWeeks(direction.toLong())
        Period.MONTH -> anchor.plusMonths(direction.toLong())
        Period.YEAR -> anchor.plusYears(direction.toLong())
    }

    /**
     * @param daily total steps per day (days without data may be missing)
     * @param today the current date; days after it are not counted in the average
     */
    fun summarize(
        anchor: LocalDate,
        period: Period,
        daily: Map<LocalDate, Long>,
        goal: Int,
        today: LocalDate,
    ): PeriodSummary {
        val (start, end) = rangeOf(anchor, period)

        val buckets = when (period) {
            Period.WEEK, Period.MONTH -> generateSequence(start) { it.plusDays(1) }
                .takeWhile { !it.isAfter(end) }
                .map { Bucket(it, daily[it] ?: 0L) }
                .toList()
            Period.YEAR -> (1..12).map { month ->
                val first = LocalDate.of(start.year, month, 1)
                val last = YearMonth.of(start.year, month).atEndOfMonth()
                val sum = daily.filterKeys { !it.isBefore(first) && !it.isAfter(last) }.values.sum()
                Bucket(first, sum)
            }
        }

        val inRange = daily.filterKeys { !it.isBefore(start) && !it.isAfter(end) }
        val total = inRange.values.sum()

        val lastCounted = if (today.isBefore(end)) today else end
        val elapsedDays = if (lastCounted.isBefore(start)) 0L else lastCounted.toEpochDay() - start.toEpochDay() + 1
        val average = if (elapsedDays > 0) total / elapsedDays else 0L

        val best = inRange.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.let { it.key to it.value }
        val goalDays = if (goal > 0) inRange.count { it.value >= goal } else 0

        return PeriodSummary(buckets, total, average, best, goalDays, start, end)
    }
}
