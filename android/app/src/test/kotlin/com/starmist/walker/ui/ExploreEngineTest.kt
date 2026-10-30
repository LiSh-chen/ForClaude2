package com.starmist.walker.ui

import com.starmist.core.route.GeoPoint
import com.starmist.walker.data.ExploreCodec
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Free exploration: which townships touch, and how the walker goes from one to the next. */
class ExploreEngineTest {
    private fun square(name: String, lon0: Double, lon1: Double) = TownArea(
        name, "測試縣", 100.0, 10, 20, 5, 23.0, (lon0 + lon1) / 2,
        listOf(doubleArrayOf(22.9, lon0, 22.9, lon1, 23.1, lon1, 23.1, lon0)),
    )

    // 甲 | 乙 | 丙 side by side, 0.1 degrees (about 10 km) wide each
    private val areas = TownAreas(listOf(square("甲", 120.0, 120.1), square("乙", 120.1, 120.2), square("丙", 120.2, 120.3)))
    private val nb = areas.neighborLists()

    @Test
    fun touchingTownshipsAreNeighbours() {
        assertEquals(setOf(1), nb[0])
        assertEquals(setOf(0, 2), nb[1])
        assertEquals(setOf(1), nb[2])
    }

    @Test
    fun startingCountsTheTownshipAsReached() {
        val s = ExploreEngine.start(areas, GeoPoint(23.0, 120.05), 1000, 90f)!!
        assertEquals(0, s.current)
        assertEquals(setOf(0), s.visited)
        assertNull(ExploreEngine.start(areas, GeoPoint(10.0, 100.0), 0, 0f))
    }

    @Test
    fun stepsWalkTowardsThePickedTownshipAndArrive() {
        val s0 = ExploreEngine.start(areas, GeoPoint(23.0, 120.05), 0, 90f)!!
        val s1 = ExploreEngine.go(s0, areas, 1)
        val leg = ExploreEngine.legKm(s1, areas)
        assertTrue(leg > 5.0 && leg < 12.0)
        // half the way: still on the way, nothing reached
        val half = ExploreEngine.advance(s1, areas, nb, s1.visited, (leg / 2 * 1000 / 0.7).toLong(), 0.7)
        assertTrue(half.reached.isEmpty())
        assertEquals(leg / 2, half.state.progressKm, 0.05)
        // more than the whole leg: arrived in 乙 and, with nothing picked, set out for 丙 (the way it was going)
        val done = ExploreEngine.advance(s1, areas, nb, s1.visited, (leg * 1.2 * 1000 / 0.7).toLong(), 0.7)
        assertEquals(listOf(1), done.reached)
        assertTrue(1 in done.state.visited)
        assertEquals(2, done.state.target)
    }

    @Test
    fun withNothingPickedItKeepsTheOriginalDirection() {
        val s0 = ExploreEngine.start(areas, GeoPoint(23.0, 120.15), 0, 90f)!!    // in 乙, heading east
        assertEquals(2, ExploreEngine.autoTarget(s0, areas, nb, s0.visited))
        val west = s0.copy(heading = 270f)
        assertEquals(0, ExploreEngine.autoTarget(west, areas, nb, west.visited))
    }

    @Test
    fun theSameStepsAreNeverCountedTwice() {
        val s0 = ExploreEngine.go(ExploreEngine.start(areas, GeoPoint(23.0, 120.05), 500, 90f)!!, areas, 1)
        val a = ExploreEngine.advance(s0, areas, nb, s0.visited, 1500, 0.7)
        val b = ExploreEngine.advance(a.state, areas, nb, a.state.visited, 1500, 0.7)
        assertEquals(a.state.progressKm, b.state.progressKm, 1e-9)
    }

    @Test
    fun progressThroughTheTownshipGrowsAlongTheLeg() {
        val s0 = ExploreEngine.go(ExploreEngine.start(areas, GeoPoint(23.0, 120.05), 0, 90f)!!, areas, 1)
        val early = ExploreEngine.townProgress(s0.copy(progressKm = 1.0), areas).first
        val later = ExploreEngine.townProgress(s0.copy(progressKm = 3.5), areas).first
        assertTrue(later > early)
    }

    @Test
    fun stateSurvivesBeingSavedAsText() {
        val s = ExploreEngine.go(ExploreEngine.start(areas, GeoPoint(23.0, 120.05), 42, 90f)!!, areas, 1)
        assertEquals(s, ExploreCodec.decode(ExploreCodec.encode(s)))
        assertNull(ExploreCodec.decode("garbage"))
    }

    @Test
    fun realTownshipsHaveNeighbours() {
        val real = File("src/main/res/raw/town_areas.bin").inputStream().use { TownAreas.parse(it) }
        val lists = real.neighborLists()
        assertNotNull(lists)
        val lonely = lists.count { it.isEmpty() }
        assertTrue("$lonely townships without a neighbour", lonely <= 12)   // offshore islands only
        for (i in lists.indices) for (j in lists[i]) assertTrue(i in lists[j])
        assertFalse(lists.any { i -> false })
    }
}
