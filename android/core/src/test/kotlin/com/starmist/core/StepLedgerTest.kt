package com.starmist.core

import com.starmist.core.steps.CounterSnapshot
import com.starmist.core.steps.DaySplitter
import com.starmist.core.steps.LedgerState
import com.starmist.core.steps.StepDeltaCalculator
import com.starmist.core.steps.StepLedger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StepLedgerTest {
    private val zone = ZoneId.of("Asia/Taipei")

    private fun millis(y: Int, m: Int, d: Int, h: Int, min: Int = 0): Long =
        LocalDateTime.of(y, m, d, h, min).atZone(zone).toInstant().toEpochMilli()

    private val boot = millis(2026, 10, 1, 6)

    private fun snap(counter: Long, at: Long, bootAt: Long = boot) = CounterSnapshot(counter, at, bootAt)

    // ---- delta ---------------------------------------------------------------------------------

    @Test
    fun `first reading is only a baseline`() {
        val delta = StepDeltaCalculator.compute(null, snap(12_345, millis(2026, 10, 7, 9)))
        assertEquals(0, delta.steps)
        assertTrue(delta.isBaseline)
    }

    @Test
    fun `normal increase is the difference`() {
        val prev = snap(1_000, millis(2026, 10, 7, 9))
        val cur = snap(1_450, millis(2026, 10, 7, 10))
        val delta = StepDeltaCalculator.compute(prev, cur)
        assertEquals(450, delta.steps)
        assertFalse(delta.rebooted)
    }

    @Test
    fun `counter going backwards means reboot and counts everything since boot`() {
        val prev = snap(9_000, millis(2026, 10, 7, 9))
        val newBoot = millis(2026, 10, 7, 9, 30)
        val cur = snap(300, millis(2026, 10, 7, 10), newBoot)
        val delta = StepDeltaCalculator.compute(prev, cur)
        assertEquals(300, delta.steps)
        assertTrue(delta.rebooted)
        assertEquals(newBoot, delta.fromMillis)
    }

    @Test
    fun `reboot is detected by boot time even if the counter grew past the old value`() {
        val prev = snap(500, millis(2026, 10, 7, 6))
        val newBoot = millis(2026, 10, 7, 6, 30)
        // Walked a lot after rebooting, so 2_000 > 500; a naive difference would give 1_500.
        val cur = snap(2_000, millis(2026, 10, 7, 12), newBoot)
        val delta = StepDeltaCalculator.compute(prev, cur)
        assertEquals(2_000, delta.steps)
        assertTrue(delta.rebooted)
    }

    @Test
    fun `small boot time drift is not a reboot`() {
        val prev = snap(1_000, millis(2026, 10, 7, 9))
        val cur = snap(1_100, millis(2026, 10, 7, 9, 15), boot + 5_000)
        val delta = StepDeltaCalculator.compute(prev, cur)
        assertEquals(100, delta.steps)
        assertFalse(delta.rebooted)
    }

    // ---- day splitting -------------------------------------------------------------------------

    @Test
    fun `same day goes to that day`() {
        val parts = DaySplitter.split(500, millis(2026, 10, 7, 9), millis(2026, 10, 7, 11), zone)
        assertEquals(mapOf(LocalDate.of(2026, 10, 7) to 500L), parts)
    }

    @Test
    fun `span across midnight is split by time and keeps the total`() {
        // 22:00 -> 02:00 is 2 h on each side.
        val parts = DaySplitter.split(1_001, millis(2026, 10, 7, 22), millis(2026, 10, 8, 2), zone)
        assertEquals(1_001L, parts.values.sum())
        assertEquals(2, parts.size)
        val day1 = parts.getValue(LocalDate.of(2026, 10, 7))
        val day2 = parts.getValue(LocalDate.of(2026, 10, 8))
        assertTrue(kotlin.math.abs(day1 - day2) <= 1)
    }

    @Test
    fun `multi day gap is spread and keeps the total`() {
        val parts = DaySplitter.split(10_000, millis(2026, 10, 5, 12), millis(2026, 10, 8, 12), zone)
        assertEquals(10_000L, parts.values.sum())
        assertEquals(4, parts.size)
        // The middle days get a full day's worth, the edge days half.
        assertTrue(parts.getValue(LocalDate.of(2026, 10, 6)) > parts.getValue(LocalDate.of(2026, 10, 5)))
    }

    @Test
    fun `reversed span goes to the end day`() {
        val parts = DaySplitter.split(40, millis(2026, 10, 8, 1), millis(2026, 10, 7, 23), zone)
        assertEquals(mapOf(LocalDate.of(2026, 10, 7) to 40L), parts)
    }

    @Test
    fun `zero steps produce nothing`() {
        assertTrue(DaySplitter.split(0, millis(2026, 10, 7, 1), millis(2026, 10, 8, 1), zone).isEmpty())
    }

    // ---- ledger --------------------------------------------------------------------------------

    @Test
    fun `ledger credits a normal interval to today`() {
        val ledger = StepLedger(zone)
        var state = LedgerState()
        state = ledger.process(state, snap(1_000, millis(2026, 10, 7, 8)), 1.0).state
        val update = ledger.process(state, snap(1_800, millis(2026, 10, 7, 9)), 1.0)
        assertEquals(mapOf(LocalDate.of(2026, 10, 7) to 800L), update.rawByDay)
        assertEquals(mapOf(LocalDate.of(2026, 10, 7) to 800L), update.scaledByDay)
    }

    @Test
    fun `ledger survives a reboot and a midnight in one go`() {
        val ledger = StepLedger(zone)
        var state = LedgerState()
        state = ledger.process(state, snap(8_000, millis(2026, 10, 7, 23)), 1.0).state
        // Phone restarted at 23:30, back to counting from 0; read at 01:00 with 200 steps on it.
        val newBoot = millis(2026, 10, 7, 23, 30)
        val update = ledger.process(state, snap(200, millis(2026, 10, 8, 1), newBoot), 1.0)
        assertTrue(update.rebooted)
        assertEquals(200L, update.rawByDay.values.sum())
        assertEquals(setOf(LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 8)), update.rawByDay.keys)
    }

    @Test
    fun `multiplier is applied without losing fractions over many small readings`() {
        val ledger = StepLedger(zone)
        var state = ledger.process(LedgerState(), snap(0, millis(2026, 10, 7, 8)), 1.04).state
        var total = 0L
        for (i in 1..1_000) {
            val update = ledger.process(state, snap(i.toLong(), millis(2026, 10, 7, 8, 0) + i * 1_000L), 1.04)
            state = update.state
            total += update.scaledByDay.values.sum()
        }
        // 1000 raw steps * 1.04 = 1040, never off by more than the one step still held in the carry.
        assertTrue(total in 1_039..1_040, "total was $total")
    }

    @Test
    fun `multiplier of one is the identity`() {
        val ledger = StepLedger(zone)
        var state = ledger.process(LedgerState(), snap(100, millis(2026, 10, 7, 8)), 1.0).state
        val update = ledger.process(state, snap(137, millis(2026, 10, 7, 9)), 1.0)
        assertEquals(37L, update.scaledByDay.values.sum())
        assertEquals(0.0, update.state.carry)
    }

    @Test
    fun `first reading yields no steps but stores the baseline`() {
        val reading = snap(5_000, millis(2026, 10, 7, 8))
        val update = StepLedger(zone).process(LedgerState(), reading, 1.0)
        assertTrue(update.rawByDay.isEmpty())
        assertEquals(reading, update.state.lastSnapshot)
        assertTrue(update.isBaseline)
    }
}
