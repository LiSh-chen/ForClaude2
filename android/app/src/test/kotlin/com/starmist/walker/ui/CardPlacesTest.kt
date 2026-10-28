package com.starmist.walker.ui

import com.starmist.core.route.Routes
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Each card shows its county and township, and the collection groups the cards by county, so every card must land in one. */
class CardPlacesTest {
    private val areas = File("src/main/res/raw/town_areas.bin").inputStream().use { TownAreas.parse(it) }

    @Test
    fun everyCardOnEveryRouteHasATownship() {
        assertTrue("townships read", areas.towns.size > 300)
        val ids = Routes.all.flatMap { r -> r.cards.map { it.first.id } }.toSet()
        assertEquals(45, ids.size)
        for (id in ids) assertNotNull("card $id has no township", areas.cardPlaces[id])
    }

    @Test
    fun aCardInTheMiddleOfATownIsFoundInThatTown() {
        val taipei101 = areas.cardPlaces["taipei101"]
        assertNotNull(taipei101)
        assertEquals("信義區", taipei101!!.name)
    }

    @Test
    fun aPointInTheSeaGetsTheNearestTownship() {
        val t = areas.townAt(25.45, 121.60)           // out at sea north of Shimen
        assertNotNull(t)
        assertTrue(t!!.county.contains("新北"))
    }

    @Test
    fun countiesAreGroupedInRouteOrder() {
        val route = Routes.all.first()
        val counties = route.cards.map { areas.cardPlaces[it.first.id]!!.county }
        val groups = counties.distinct()
        assertTrue("several counties", groups.size in 8..25)
        assertEquals(groups, route.cards.groupBy { areas.cardPlaces[it.first.id]!!.county }.keys.toList())
    }
}
