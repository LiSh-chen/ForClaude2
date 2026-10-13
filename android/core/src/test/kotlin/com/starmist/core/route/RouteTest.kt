package com.starmist.core.route

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RouteTest {
    private val route = TaiwanLoop.route

    @Test
    fun `every card blurb is exactly ten characters`() {
        route.cards.forEach { (card, _) ->
            assertEquals(10, card.blurb.length, "${card.name}: ${card.blurb}")
        }
    }

    @Test
    fun `card ids are unique and cards come in route order`() {
        val ids = route.cards.map { it.first.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(route.cards.zipWithNext().all { (a, b) -> a.second < b.second })
    }

    @Test
    fun `the loop is about a thousand kilometres`() {
        assertTrue(route.totalKm in 900.0..1300.0, "total ${route.totalKm}")
    }

    @Test
    fun `the first card is collected at the start and all of them at the end`() {
        assertEquals("taipei101", route.collected(0.0).single().id)
        assertEquals(route.cards.size, route.collected(route.totalKm).size)
        assertEquals(null, route.next(route.totalKm))
    }

    @Test
    fun `position starts and ends at the first and last stop and never jumps`() {
        assertEquals(route.stops.first().at, route.positionAt(0.0))
        assertEquals(route.stops.last().at, route.positionAt(route.totalKm + 50))
        var last = route.positionAt(0.0)
        var km = 0.0
        while (km < route.totalKm) {
            km += 5.0
            val p = route.positionAt(km)
            assertTrue(p.distanceKm(last) < 6.0, "jump at $km")
            last = p
        }
    }

    /** Writes the card list for review into the build folder; not an assertion. */
    @Test
    fun `write review table`() {
        val out = File("build/review").apply { mkdirs() }
        val rows = route.cards.mapIndexed { i, (card, km) ->
            "| ${i + 1} | ${card.name} | ${card.blurb} | ${"%.0f".format(km)} |"
        }
        File(out, "taiwan_loop.md").writeText(
            "| # | 景點 | 十字特色 | 累計公里 |\n|---|---|---|---|\n" + rows.joinToString("\n") +
                "\n\n全長約 ${"%.0f".format(route.totalKm)} 公里\n",
        )
    }
}
