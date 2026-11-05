package com.starmist.core.route

import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.random.Random

/** A township's key ("county|name") and a point inside it. */
data class TownPoint(val key: String, val lat: Double, val lon: Double)

/** Chooses which off-route townships a postcard can come from. */
object PostcardPicker {
    private fun km(a: TownPoint, b: TownPoint): Double {
        val dy = (a.lat - b.lat) * 111.0
        val dx = (a.lon - b.lon) * 111.0 * cos((a.lat + b.lat) / 2 * 3.141592653589793 / 180)
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * Up to [n] townships the player may pick from: not on any route, not yet unlocked, and among the nearest to what the
     * player has already walked through or unlocked. The same [seed] gives the same offer, so it does not change between visits.
     */
    fun offered(all: List<TownPoint>, onRoute: Set<String>, unlocked: Set<String>, visited: Set<String>, seed: Int, n: Int = 3): List<String> {
        val pool = all.filter { it.key !in onRoute && it.key !in unlocked }
        if (pool.isEmpty()) return emptyList()
        val anchors = all.filter { it.key in visited || it.key in unlocked }
        val ranked = if (anchors.isEmpty()) pool.sortedBy { it.key } else pool.sortedBy { p -> anchors.minOf { km(p, it) } }
        return ranked.take(maxOf(n * 4, n)).shuffled(Random(seed)).take(n).map { it.key }
    }
}
