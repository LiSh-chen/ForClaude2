package com.starmist.core.route

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RouteTest {
    init { TestPack.install() }

    private val route = Routes.byId("taiwan_loop")!!

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
        // Only the train and ferry legs may jump: the walker is carried across them.
        val hops = route.stops.indices.filter { route.stops[it].transit != null && !route.stops[it].via }.map { route.cumulativeKm[it] }
        var last = route.positionAt(0.0)
        var km = 0.0
        while (km < route.totalKm) {
            val before = km
            km += 5.0
            val p = route.positionAt(km)
            val crossesHop = hops.any { it > before - 1e-9 && it <= km + 1e-9 }
            assertTrue(crossesHop || p.distanceKm(last) < 6.0, "jump at $km")
            last = p
        }
    }

    @Test
    fun `train and ferry legs add no distance`() {
        val hualien = route.stops.indexOfFirst { it.name == "花蓮" }
        assertEquals(route.cumulativeKm[hualien - 1], route.cumulativeKm[hualien], 1e-9)
        assertEquals("搭火車", route.transitInto(hualien))
        val tamsui = route.stops.indexOfFirst { it.name == "淡水" }
        assertEquals(route.cumulativeKm[tamsui - 1], route.cumulativeKm[tamsui], 1e-9)
    }

    @Test
    fun `cards and supply stops together leave no stretch longer than forty-three kilometres`() {
        val points = (route.cards.map { it.second } + route.supplies.map { it.second }).sorted()
        points.zipWithNext().forEach { (a, b) -> assertTrue(b - a <= 43.0, "gap ${"%.0f".format(b - a)} km after ${"%.0f".format(a)}") }
        assertEquals(route.supplies.size, route.supplies.map { it.first.id }.toSet().size)
    }

    @Test
    fun `the counter-clockwise loop has the same cards, stamps and two crossings`() {
        val ccw = Routes.byId("taiwan_loop_ccw")!!
        assertEquals(route.cards.map { it.first.id }.toSet(), ccw.cards.map { it.first.id }.toSet())
        assertEquals(route.supplies.map { it.first.id }.toSet(), ccw.supplies.map { it.first.id }.toSet())
        assertEquals("taipei101", ccw.cards.first().first.id)
        assertEquals("finish", ccw.cards.last().first.id)
        assertEquals(2, ccw.stops.count { it.transit != null && !it.via })
        assertEquals(route.totalKm, ccw.totalKm, 40.0)
        assertEquals(2, Routes.families.single().size)
        // The train leg is now Hualien -> Suao.
        val suao = ccw.stops.indexOfFirst { it.landmark?.id == "suao" }
        assertEquals("搭火車", ccw.stops[suao].transit)
        assertEquals("花蓮", ccw.stops.take(suao).last { !it.via }.name)
    }

    @Test
    fun `no stretch of the counter-clockwise loop is longer than forty-three kilometres either`() {
        val ccw = Routes.byId("taiwan_loop_ccw")!!
        val points = (ccw.cards.map { it.second } + ccw.supplies.map { it.second }).sorted()
        points.zipWithNext().forEach { (a, b) -> assertTrue(b - a <= 43.0, "gap ${"%.0f".format(b - a)} km after ${"%.0f".format(a)}") }
    }

    @Test
    fun `small sights show only near the walker`() {
        val near = route.poisNear(GeoPoint(25.16, 121.76))
        assertEquals("heping", near.first().first.id)
        assertTrue(route.poisNear(GeoPoint(23.0, 121.1)).none { it.first.id == "heping" })
        assertTrue(route.pois.all { p -> p.id.isNotBlank() && p.name.isNotBlank() })
    }

    /** Writes the card list for review into the build folder; not an assertion. */
    @Test
    fun `write review table`() {
        val out = File("build/review").apply { mkdirs() }
        val rows = route.cards.mapIndexed { i, (card, km) ->
            "| ${i + 1} | ${card.name} | ${card.blurb} | ${"%.0f".format(km)} |"
        }
        val all = (route.cards.map { it.first.name to it.second } + route.supplies.map { "🚉" + it.first.name to it.second }).sortedBy { it.second }
        File(out, "taiwan_gaps_all.txt").writeText(all.zipWithNext { a, b -> "${a.first}→${b.first} ${"%.0f".format(b.second - a.second)} km" }.joinToString("\n"))
        val json = route.stops.mapIndexed { i, st ->
            val kind = when {
                st.landmark != null -> "card"
                st.supply != null -> "supply"
                st.transit != null -> "transit"
                else -> "via"
            }
            val label = st.landmark?.blurb ?: st.supply?.opened ?: st.transit ?: ""
            """{"name":"${st.name}","lat":${st.at.lat},"lng":${st.at.lng},"kind":"$kind","km":${"%.1f".format(route.cumulativeKm[i])},"text":"$label"}"""
        }
        File(out, "taiwan_route.json").writeText("[" + json.joinToString(",\n") + "]")
        val gaps = route.cards.zipWithNext { a, b -> "${a.first.name}→${b.first.name} ${"%.0f".format(b.second - a.second)} km" }
        File(out, "taiwan_gaps.txt").writeText(gaps.joinToString("\n"))
        File(out, "taiwan_loop.md").writeText(
            "| # | 景點 | 十字特色 | 累計公里 |\n|---|---|---|---|\n" + rows.joinToString("\n") +
                "\n\n全長約 ${"%.0f".format(route.totalKm)} 公里\n",
        )
    }
}
