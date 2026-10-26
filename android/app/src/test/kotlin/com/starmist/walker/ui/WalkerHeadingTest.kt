package com.starmist.walker.ui

import com.starmist.core.route.TaiwanLoop
import org.junit.Assert.assertEquals
import org.junit.Test

/** The traveller must face the way the route runs on the screen, whichever way round the island is walked. */
class WalkerHeadingTest {
    private val clockwise = TaiwanLoop.route
    private val counter = TaiwanLoop.counterClockwise

    private fun kmOf(route: com.starmist.core.route.RouteDef, id: String) = route.cards.first { it.first.id == id }.second

    @Test
    fun clockwiseFacesEastTowardsTheNortheastCapeAndWestAlongTheSouthTip() {
        val fulong = kmOf(clockwise, "fulong"); val sandiao = kmOf(clockwise, "sandiaojiao")
        assertEquals(1f, walkerHeading(clockwise, (fulong + sandiao) / 2), 0f)          // Fulong to Sandiaojiao: east
        val eluanbi = kmOf(clockwise, "eluanbi"); val maobitou = kmOf(clockwise, "maobitou")
        assertEquals(-1f, walkerHeading(clockwise, (eluanbi + maobitou) / 2), 0f)       // Eluanbi to Maobitou: west
    }

    @Test
    fun counterClockwiseFacesTheOtherWayAtTheSamePlaces() {
        // The same two stretches of coast, walked backwards: Sandiaojiao comes first, then Fulong, so the traveller faces west.
        val fulong = kmOf(counter, "fulong"); val sandiao = kmOf(counter, "sandiaojiao")
        assertEquals(-1f, walkerHeading(counter, (fulong + sandiao) / 2), 0f)
        val eluanbi = kmOf(counter, "eluanbi"); val maobitou = kmOf(counter, "maobitou")
        assertEquals(1f, walkerHeading(counter, (eluanbi + maobitou) / 2), 0f)
    }

    @Test
    fun everyKilometreFacesTheWayTheRouteMovesOnScreen() {
        for (route in listOf(clockwise, counter)) {
            var km = 0.0
            while (km < route.totalKm - 1.0) {
                val dx = route.positionAt(km + 1.0).unit().x - route.positionAt(km).unit().x
                val h = walkerHeading(route, km)
                if (dx > 0.0004f) assertEquals(1f, h, 0f) else if (dx < -0.0004f) assertEquals(-1f, h, 0f) else assertEquals(0f, h, 0f)
                km += 0.5
            }
        }
    }
}
