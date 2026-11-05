package com.starmist.core.route

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PostcardTest {
    private val all = (0 until 30).map { TownPoint("c|t$it", 23.0 + it * 0.05, 120.0) }

    @Test fun offersOnlyOffRouteLockedTowns() {
        val onRoute = (0 until 10).map { "c|t$it" }.toSet()
        val got = PostcardPicker.offered(all, onRoute, setOf("c|t10"), setOf("c|t9"), seed = 1)
        assertEquals(3, got.size)
        assertTrue(got.none { it in onRoute || it == "c|t10" })
    }

    @Test fun offersTheNearestAndIsStableForASeed() {
        val onRoute = (0 until 10).map { "c|t$it" }.toSet()
        val a = PostcardPicker.offered(all, onRoute, emptySet(), setOf("c|t9"), seed = 7)
        val b = PostcardPicker.offered(all, onRoute, emptySet(), setOf("c|t9"), seed = 7)
        assertEquals(a, b)
        // the 12 nearest to t9 among the rest are t10..t21
        assertTrue(a.all { it.removePrefix("c|t").toInt() in 10..21 })
    }

    @Test fun nothingLeftGivesNothing() {
        assertTrue(PostcardPicker.offered(all, all.map { it.key }.toSet(), emptySet(), emptySet(), 0).isEmpty())
        assertFalse(PostcardPicker.offered(all, emptySet(), emptySet(), emptySet(), 0).isEmpty())
    }
}
