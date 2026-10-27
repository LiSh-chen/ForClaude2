package com.starmist.walker.ui

import com.starmist.core.route.GeoPoint
import com.starmist.core.route.RouteDef
import com.starmist.core.route.Stop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** The dashboard's "steps to the next region" depends on where the route crosses a township border. */
class NextBorderTest {
    private fun square(name: String, lon0: Double, lon1: Double) = TownArea(
        name, "測試縣", 100.0, 10, 20, 5, 23.0, (lon0 + lon1) / 2,
        listOf(doubleArrayOf(22.9, lon0, 22.9, lon1, 23.1, lon1, 23.1, lon0)),
    )

    private val areas = TownAreas(listOf(square("甲", 120.0, 120.1), square("乙", 120.1, 120.2)))
    private val route = RouteDef(
        "t", "測試", "測試路線", "兩個鄉鎮",
        listOf(Stop("起", GeoPoint(23.0, 120.02)), Stop("終", GeoPoint(23.0, 120.18))), roadFactor = 1.0,
    )

    @Test
    fun atTheStartTheBorderIsAboutEightKilometresAway() {
        val b = areas.nextBorder(route, 0.0)!!
        assertEquals(0.08 * 111.32 * Math.cos(Math.toRadians(23.0)), b.kmLeft, 0.35)
        assertEquals(0.0, b.fraction, 0.01)
    }

    @Test
    fun halfWayThroughTheFirstTownshipItIsHalfWay() {
        val half = route.totalKm / 4.0             // the border is half way along the route's first half
        val b = areas.nextBorder(route, half)!!
        assertEquals(0.5, b.fraction, 0.06)
        assertEquals(half, b.kmLeft, 0.4)
    }

    @Test
    fun inTheLastTownshipThereIsNoBorderAhead() {
        assertNull(areas.nextBorder(route, route.totalKm * 0.8))
        assertNotNull(areas.nextBorder(route, route.totalKm * 0.2))
    }
}
