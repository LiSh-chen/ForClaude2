package com.starmist.walker.ui

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The daily statistics draw: what each card says, which are available, and that a day's pick is steady. */
class StatDrawTest {
    private val today = LocalDate.of(2026, 10, 28)             // a Wednesday

    /** Three months of walking: 6,000 steps a day, 9,000 on Saturdays, a rest on the 20th of each month. */
    private val history: Map<LocalDate, Long> = (0L..100L).associate { back ->
        val d = today.minusDays(back)
        d to when { d.dayOfMonth == 20 -> 0L; d.dayOfWeek.value == 6 -> 9000L; else -> 6000L }
    }

    private val towns = listOf(
        TownVisit("甲鎮", "測試縣", 6.0, 40.0, 100, true),
        TownVisit("乙鄉", "測試縣", 1.5, 90.0, 900, true),
        TownVisit("丙區", "另一市", 3.0, 10.0, 0, false),
    )

    private val full = StatInputs(
        today = today, history = history, goal = 8000, strideM = 0.7, routeTotalKm = 1100.0, walkedKm = 275.0,
        nextCard = "某某卡" to 12.3, towns = towns, townsTotal = 109, tagsFound = 5, cardsOwned = 9, cardsTotal = 45,
    )

    @Test
    fun withAFullHistoryEveryKindHasACard() {
        for (k in StatKind.values()) {
            val c = buildStat(k, full, dayStatSeed(today))
            assertNotNull("$k has no card", c)
            assertTrue("$k headline", c!!.headline.isNotBlank())
            assertTrue("$k line", c.line.isNotBlank())
        }
        assertTrue(StatKind.values().size >= 15)
    }

    @Test
    fun withNothingWalkedOnlyTheMeasureAndTheFortuneAreLeft() {
        val empty = StatInputs(today, emptyMap(), 8000, 0.7)
        assertEquals(listOf(StatKind.RULER, StatKind.FORTUNE), availableStats(empty, 1L))
    }

    @Test
    fun theRulersDoTheirArithmetic() {
        val steps = history.values.sum()
        val km = steps * 0.7 / 1000.0
        for (r in rulersFor(1100.0)) {
            val c = buildStat(StatKind.RULER, full, 3L, r)!!
            val n = c.headline.substringBefore(' ').replace(",", "").toDouble()
            val half = when { km / r.km < 1 -> 0.0051; km / r.km < 10 -> 0.051; else -> 0.51 }     // the headline is rounded: two, one or no decimals
            assertTrue("${c.headline} for $km km with ${r.id}", kotlin.math.abs(km / r.km - n) <= half)
            assertTrue(c.headline.endsWith(r.unit + r.what))
            assertTrue(c.line.contains(steps.withCommas()))
        }
    }

    @Test
    fun theRulerListHasFamiliarLengthsAndTheRouteOnlyWithARoute() {
        assertTrue(rulersFor(0.0).size >= 10)
        assertTrue(rulersFor(0.0).none { it.id == "route" })
        assertEquals(1100.0, rulersFor(1100.0).first { it.id == "route" }.km, 0.0)
        assertEquals(0.4, rulersFor(0.0).first { it.id == "track" }.km, 0.0)
        assertEquals(42.195, rulersFor(0.0).first { it.id == "full" }.km, 0.0)
        assertEquals(rulersFor(0.0).map { it.id }.size, rulersFor(0.0).map { it.id }.toSet().size)
    }

    @Test
    fun todaysRulerIsSteadyAndNotTheDayBeforesAndAnEveryDayChartUnitIsShort() {
        val rulers = rulersFor(1100.0)
        assertEquals(drawRuler(today, rulers)?.id, drawRuler(today, rulers)?.id)
        for (back in 0..60) {
            val d = today.minusDays(back.toLong())
            assertNotEquals("$d repeats the day before", drawRuler(d, rulers)?.id, drawRuler(d.minusDays(1), rulers)?.id)
        }
        assertNull(drawRuler(today, emptyList()))
        assertEquals("0.27", niceCount(0.2712)); assertEquals("3.4", niceCount(3.44)); assertEquals("56", niceCount(55.6)); assertEquals("1,235", niceCount(1234.6))
    }

    @Test
    fun theHeatMapHasADayPerCellAndKnowsTheGoalDays() {
        val c = buildStat(StatKind.HEATMAP, full, 1L)!!
        val v = c.visual as StatVisual.Heat
        assertEquals(31, v.steps.size)
        assertEquals(27, v.todayIndex)
        assertEquals(6000L, v.steps[0])                            // 1 October 2026 is a Thursday
        assertEquals(0L, v.steps[19])                              // the rest day, the 20th
        assertEquals(0L, v.steps[30])                              // not yet
    }

    @Test
    fun streaksCountBackFromTodayOrYesterday() {
        val h = (0L..5L).associate { today.minusDays(it) to 9000L } + (today.minusDays(6) to 100L) + (today.minusDays(7) to 9000L)
        assertEquals(6 to 6, streaks(h, 8000, today))
        val notYet = h + (today to 10L)                            // today not done: the run ends yesterday
        assertEquals(5 to 5, streaks(notYet, 8000, today))
        assertEquals(0 to 0, streaks(h, 0, today))
    }

    @Test
    fun theWeekAgainstLastWeekComparesTheSameSpan() {
        val c = buildStat(StatKind.WEEK_VS_LAST, full, 1L)!!
        val bars = (c.visual as StatVisual.Bars).rows
        assertEquals(2, bars.size)
        // Monday to Wednesday this week is 3 x 6,000; the same days last week include the rest day, the 20th
        assertEquals(18000.0, bars[0].second, 0.1)
        assertEquals(12000.0, bars[1].second, 0.1)
    }

    @Test
    fun townCardsPickTheRightTownship() {
        assertEquals("甲鎮", buildStat(StatKind.TOWN_LONGEST, full, 1L)!!.headline)
        assertEquals("乙鄉", buildStat(StatKind.TOWN_SHORTEST, full, 1L)!!.headline)    // the one still being walked is not counted
        assertEquals("乙鄉", buildStat(StatKind.TOWN_BIGGEST, full, 1L)!!.headline)
        assertEquals("乙鄉", buildStat(StatKind.TOWN_HIGHEST, full, 1L)!!.headline)
        assertEquals("3 / 109", buildStat(StatKind.FOOTPRINT, full, 1L)!!.headline)
    }

    @Test
    fun cardsNeedingARouteAreLeftOutWithoutOne() {
        val noRoute = full.copy(routeTotalKm = 0.0, walkedKm = 0.0, towns = emptyList(), cardsTotal = 0)
        assertNull(buildStat(StatKind.ROUTE_PROGRESS, noRoute, 1L))
        assertNull(buildStat(StatKind.CARDS, noRoute, 1L))
        assertNull(buildStat(StatKind.TOWN_LONGEST, noRoute, 1L))
    }

    @Test
    fun todaysPickIsSteadyAndChangesFromDayToDay() {
        val kinds = StatKind.values().toList()
        assertEquals(drawStat(today, kinds), drawStat(today, kinds))
        for (back in 0..60) {
            val d = today.minusDays(back.toLong())
            assertNotEquals("$d repeats the day before", drawStat(d, kinds), drawStat(d.minusDays(1), kinds))   // the same list of cards both days
        }
        assertNull(drawStat(today, emptyList()))
        assertEquals(StatKind.FORTUNE, drawStat(today, listOf(StatKind.FORTUNE)))
    }

    @Test
    fun theFortuneIsTheSameAllDay() {
        val a = buildStat(StatKind.FORTUNE, full, dayStatSeed(today))!!
        val b = buildStat(StatKind.FORTUNE, full, dayStatSeed(today))!!
        assertEquals(a.line, b.line); assertEquals(a.note, b.note)
    }
}
