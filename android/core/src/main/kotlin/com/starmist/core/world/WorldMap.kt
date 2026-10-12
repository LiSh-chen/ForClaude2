package com.starmist.core.world

import kotlin.math.hypot

/** A point on the map; both axes run 0.0 to 1.0 (x to the right, y downwards). */
data class Pt(val x: Double, val y: Double) {
    fun distanceTo(other: Pt): Double = hypot(x - other.x, y - other.y)
}

/**
 * A place on the map. [available] is false for regions that are drawn but have no content yet, so
 * the map can say "not open yet" instead of pretending they are reachable.
 */
data class MapNode(val id: String, val name: String, val at: Pt, val available: Boolean)

/**
 * The whole world on one sheet, plus the fog that hides the parts nobody has walked to.
 *
 * The route visits the nodes in order. Region k is walked from an entry point, through its node, to
 * the next entry point, so the traveller moves along a smooth curve and always passes its region.
 */
object WorldMap {
    // Positions follow the painted map (android/assets/generated/world_map_base.png): where each place
    // is drawn, as a share of the picture's width and height.
    val nodes: List<MapNode> = listOf(
        MapNode("whispering_forest", "低語森林", Pt(0.25, 0.10), available = true),
        MapNode("moonlit_coast", "月光海岸", Pt(0.66, 0.23), available = true),
        MapNode("coral_isles", "珊瑚群島", Pt(0.82, 0.44), available = false),
        MapNode("misty_swamp", "霧中沼澤", Pt(0.28, 0.48), available = false),
        MapNode("star_dunes", "星砂沙丘", Pt(0.33, 0.65), available = false),
        MapNode("sky_ruins", "浮空遺跡", Pt(0.67, 0.66), available = false),
        MapNode("world_tree", "世界之樹", Pt(0.50, 0.86), available = false),
    )

    /**
     * The way through each region as a line of waypoints laid along the painted paths. Every region
     * starts where the previous one ended; the trip to the coral isles crosses the water by boat and
     * lands on the east shore of the mainland.
     */
    private val routes: List<List<Pt>> = listOf(
        listOf(Pt(0.13, 0.12), Pt(0.19, 0.13), Pt(0.25, 0.15), Pt(0.31, 0.16), Pt(0.36, 0.17), Pt(0.40, 0.16)),
        listOf(Pt(0.40, 0.16), Pt(0.47, 0.18), Pt(0.53, 0.20), Pt(0.60, 0.21), Pt(0.66, 0.23), Pt(0.70, 0.26), Pt(0.72, 0.30)),
        listOf(Pt(0.72, 0.30), Pt(0.78, 0.34), Pt(0.82, 0.39), Pt(0.83, 0.44), Pt(0.79, 0.50), Pt(0.70, 0.51), Pt(0.61, 0.50)),
        listOf(Pt(0.61, 0.50), Pt(0.52, 0.51), Pt(0.44, 0.52), Pt(0.36, 0.51), Pt(0.29, 0.49), Pt(0.26, 0.53), Pt(0.29, 0.58)),
        listOf(Pt(0.29, 0.58), Pt(0.31, 0.62), Pt(0.34, 0.66), Pt(0.38, 0.69), Pt(0.44, 0.68), Pt(0.50, 0.66)),
        listOf(Pt(0.50, 0.66), Pt(0.57, 0.66), Pt(0.64, 0.65), Pt(0.70, 0.66), Pt(0.72, 0.70), Pt(0.68, 0.74)),
        listOf(Pt(0.68, 0.74), Pt(0.62, 0.79), Pt(0.56, 0.82), Pt(0.50, 0.86)),
    )

    /** Where the journey begins: the jetty just outside the first region. */
    val start: Pt = routes.first().first()

    /** How far around a walked spot the fog is lifted. */
    const val REVEAL_RADIUS = 0.18

    fun node(id: String): MapNode? = nodes.firstOrNull { it.id == id }

    /** Position along region [regionIndex] after walking [fraction] (0..1) of it, by distance along its line. */
    fun pointAt(regionIndex: Int, fraction: Double): Pt {
        val line = routes[regionIndex.coerceIn(0, routes.lastIndex)]
        val lengths = line.zipWithNext { a, b -> a.distanceTo(b) }
        var remaining = fraction.coerceIn(0.0, 1.0) * lengths.sum()
        for (i in lengths.indices) {
            if (remaining <= lengths[i] || i == lengths.lastIndex) {
                val u = if (lengths[i] == 0.0) 0.0 else (remaining / lengths[i]).coerceIn(0.0, 1.0)
                val a = line[i]
                val b = line[i + 1]
                return Pt(a.x + (b.x - a.x) * u, a.y + (b.y - a.y) * u)
            }
            remaining -= lengths[i]
        }
        return line.last()
    }

    /**
     * Spots the traveller has been to: every finished region and the walked part of the current one.
     * Fog is lifted around these.
     */
    fun walkedPoints(regionIndex: Int, fraction: Double, finished: Boolean, perRegion: Int = 12): List<Pt> {
        val points = ArrayList<Pt>()
        val current = regionIndex.coerceIn(0, nodes.lastIndex)
        for (k in 0 until current) {
            for (i in 0..perRegion) points += pointAt(k, i.toDouble() / perRegion)
        }
        val upTo = if (finished) 1.0 else fraction.coerceIn(0.0, 1.0)
        val steps = (perRegion * upTo).toInt()
        for (i in 0..steps) points += pointAt(current, i.toDouble() / perRegion)
        points += pointAt(current, upTo)
        return points
    }

    fun isRevealed(spot: Pt, walked: List<Pt>, radius: Double = REVEAL_RADIUS): Boolean =
        walked.any { it.distanceTo(spot) <= radius }
}
