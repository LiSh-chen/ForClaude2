package com.starmist.core

import com.starmist.core.diagnostics.HealthInput
import com.starmist.core.diagnostics.Issue
import com.starmist.core.diagnostics.SnapshotHealth
import com.starmist.core.stats.Aggregator
import com.starmist.core.stats.Period
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AggregatorAndHealthTest {
    private val wednesday = LocalDate.of(2026, 10, 7)

    @Test
    fun `week runs monday to sunday`() {
        val (start, end) = Aggregator.rangeOf(wednesday, Period.WEEK)
        assertEquals(LocalDate.of(2026, 10, 5), start)
        assertEquals(LocalDate.of(2026, 10, 11), end)
    }

    @Test
    fun `week summary counts only elapsed days in the average`() {
        val daily = mapOf(
            LocalDate.of(2026, 10, 5) to 9_000L,
            LocalDate.of(2026, 10, 6) to 6_000L,
            wednesday to 3_000L,
        )
        val s = Aggregator.summarize(wednesday, Period.WEEK, daily, goal = 8_000, today = wednesday)
        assertEquals(7, s.buckets.size)
        assertEquals(18_000, s.totalSteps)
        assertEquals(6_000, s.averagePerDay) // 3 elapsed days, not 7
        assertEquals(LocalDate.of(2026, 10, 5) to 9_000L, s.bestDay)
        assertEquals(1, s.goalDays)
    }

    @Test
    fun `past period averages over all its days`() {
        val daily = (1..31).associate { LocalDate.of(2026, 7, it) to 1_000L }
        val s = Aggregator.summarize(LocalDate.of(2026, 7, 15), Period.MONTH, daily, goal = 0, today = wednesday)
        assertEquals(31, s.buckets.size)
        assertEquals(31_000, s.totalSteps)
        assertEquals(1_000, s.averagePerDay)
        assertEquals(0, s.goalDays)
    }

    @Test
    fun `year has twelve monthly buckets`() {
        val daily = mapOf(
            LocalDate.of(2026, 1, 3) to 100L,
            LocalDate.of(2026, 1, 20) to 200L,
            LocalDate.of(2026, 3, 1) to 500L,
            LocalDate.of(2025, 12, 31) to 999L, // other year, must be ignored
        )
        val s = Aggregator.summarize(wednesday, Period.YEAR, daily, goal = 0, today = wednesday)
        assertEquals(12, s.buckets.size)
        assertEquals(300, s.buckets[0].steps)
        assertEquals(0, s.buckets[1].steps)
        assertEquals(500, s.buckets[2].steps)
        assertEquals(800, s.totalSteps)
    }

    @Test
    fun `empty period has no best day and no crash`() {
        val s = Aggregator.summarize(wednesday, Period.WEEK, emptyMap(), goal = 8_000, today = wednesday)
        assertEquals(0, s.totalSteps)
        assertEquals(null, s.bestDay)
    }

    @Test
    fun `future period has a zero average`() {
        val s = Aggregator.summarize(LocalDate.of(2027, 1, 1), Period.MONTH, emptyMap(), 8_000, wednesday)
        assertEquals(0, s.averagePerDay)
    }

    @Test
    fun `shifting moves by the period length`() {
        assertEquals(LocalDate.of(2026, 9, 30), Aggregator.shift(wednesday, Period.WEEK, -1))
        assertEquals(LocalDate.of(2026, 9, 7), Aggregator.shift(wednesday, Period.MONTH, -1))
        assertEquals(LocalDate.of(2027, 10, 7), Aggregator.shift(wednesday, Period.YEAR, 1))
    }

    // ---- health --------------------------------------------------------------------------------

    private val now = 1_000_000_000_000L
    private fun input(
        permission: Boolean = true,
        counter: Boolean = true,
        last: Long? = now - 60_000,
        battery: Boolean = false,
    ) = HealthInput(permission, counter, last, now, battery)

    @Test
    fun `healthy setup has no issues`() {
        assertTrue(SnapshotHealth.evaluate(input()).isEmpty())
    }

    @Test
    fun `missing permission is reported first`() {
        assertEquals(listOf(Issue.PERMISSION_MISSING), SnapshotHealth.evaluate(input(permission = false)))
    }

    @Test
    fun `stale snapshot and battery restriction are both reported`() {
        val issues = SnapshotHealth.evaluate(input(last = now - 7 * 60 * 60 * 1000L, battery = true))
        assertEquals(listOf(Issue.STALE_SNAPSHOT, Issue.BATTERY_RESTRICTED), issues)
    }

    @Test
    fun `stable mode expected but service not running is reported`() {
        val running = HealthInput(true, true, now - 60_000, now, false, serviceExpected = true, serviceRunning = true)
        val stopped = running.copy(serviceRunning = false)
        assertTrue(SnapshotHealth.evaluate(running).isEmpty())
        assertEquals(listOf(Issue.SERVICE_NOT_RUNNING), SnapshotHealth.evaluate(stopped))
    }

    @Test
    fun `service that is not expected is never reported`() {
        val off = HealthInput(true, true, now - 60_000, now, false, serviceExpected = false, serviceRunning = false)
        assertTrue(SnapshotHealth.evaluate(off).isEmpty())
    }

    @Test
    fun `never read is reported`() {
        assertEquals(listOf(Issue.NEVER_READ), SnapshotHealth.evaluate(input(last = null)))
    }

    @Test
    fun `no hardware counter is reported`() {
        assertEquals(listOf(Issue.NO_HARDWARE_COUNTER), SnapshotHealth.evaluate(input(counter = false)))
    }
}
