package com.starmist.core.route

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RouteTrackerTest {
    private val route = TaiwanLoop.route  // texts are not needed here
    private val stride = 0.7

    @Test
    fun `the first card is collected as soon as walking begins`() {
        val start = RouteTracker.start(route, 1_000)
        val none = RouteTracker.advance(route, start, 1_000, stride)
        assertTrue(none.events.isEmpty(), "no steps, nothing yet")
        val first = RouteTracker.advance(route, start, 1_010, stride)
        val cards = first.events.filterIsInstance<RouteEvent.CardCollected>()
        assertEquals("taipei101", cards.single().landmark.id)
    }

    @Test
    fun `steps become kilometres at the stride and cards unlock in order`() {
        var state = RouteTracker.start(route, 0)
        val seen = ArrayList<String>()
        var steps = 0L
        while (state.walkedKm < 160.0) {
            steps += 1_000
            val result = RouteTracker.advance(route, state, steps, stride)
            state = result.state
            result.events.forEach { if (it is RouteEvent.CardCollected) seen += it.landmark.id }
        }
        assertEquals(listOf("taipei101", "keelung", "jiufen", "fulong", "sandiaojiao", "toucheng", "ncta", "suao", "qixingtan", "pinegarden"), seen)
        assertEquals(steps * stride / 1000.0, state.walkedKm, 1e-6)
    }

    @Test
    fun `a big jump reports everything passed once and the train hop`() {
        val state = RouteTracker.start(route, 0)
        val result = RouteTracker.advance(route, state, 300_000, stride) // 210 km
        val transit = result.events.filterIsInstance<RouteEvent.Transit>()
        assertEquals("搭火車", transit.single().by)
        val ids = result.events.filterIsInstance<RouteEvent.CardCollected>().map { it.landmark.id }
        assertTrue("suao" in ids && "qixingtan" in ids && "lintienshan" in ids)
        // A second call with no new steps adds nothing.
        val again = RouteTracker.advance(route, result.state, 300_000, stride)
        assertTrue(again.events.isEmpty())
    }

    @Test
    fun `stamps carry the station name and opening year`() {
        val result = RouteTracker.advance(route, RouteTracker.start(route, 0), 400_000, stride)
        val stamps = result.events.filterIsInstance<RouteEvent.StampCollected>()
        assertEquals("鳳林站", stamps.first().supply.name)
        assertEquals("1912", stamps.first().supply.opened)
    }

    @Test
    fun `the route ends once and does not go past the end`() {
        val result = RouteTracker.advance(route, RouteTracker.start(route, 0), 5_000_000, stride)
        assertEquals(route.totalKm, result.state.walkedKm, 1e-9)
        assertEquals(1, result.events.count { it is RouteEvent.Finished })
        val again = RouteTracker.advance(route, result.state, 6_000_000, stride)
        assertTrue(again.events.isEmpty())
    }

    @Test
    fun `a shrinking total does not stall later walking`() {
        val state = RouteState(route.id, consumedSteps = 10_000, walkedKm = 5.0)
        val corrected = RouteTracker.advance(route, state, 8_000, stride)
        assertEquals(5.0, corrected.state.walkedKm, 1e-9)
        val later = RouteTracker.advance(route, corrected.state, 9_000, stride)
        assertTrue(later.state.walkedKm > 5.0)
    }

    @Test
    fun `state text round trips and rejects damage`() {
        val s = RouteState("taiwan_loop", 12_345, 87.65)
        assertEquals(s, RouteStateCodec.decode(RouteStateCodec.encode(s)))
        assertEquals(null, RouteStateCodec.decode("garbage"))
        assertEquals(null, RouteStateCodec.decode("v1|a|x|1.0"))
        assertNotNull(Routes.byId("taiwan_loop"))
    }

    @Test
    fun `the outline covers the island`() {
        val lons = TaiwanOutline.lonLat.map { it.lng }
        val lats = TaiwanOutline.lonLat.map { it.lat }
        assertTrue(lons.min() < 120.1 && lons.max() > 121.9 && lats.min() < 22.0 && lats.max() > 25.2)
    }
}
