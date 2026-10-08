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
    val nodes: List<MapNode> = listOf(
        MapNode("whispering_forest", "低語森林", Pt(0.26, 0.12), available = true),
        MapNode("moonlit_coast", "月光海岸", Pt(0.72, 0.24), available = true),
        MapNode("coral_isles", "珊瑚群島", Pt(0.80, 0.46), available = false),
        MapNode("misty_swamp", "霧中沼澤", Pt(0.28, 0.46), available = false),
        MapNode("star_dunes", "星砂沙丘", Pt(0.24, 0.68), available = false),
        MapNode("sky_ruins", "浮空遺跡", Pt(0.70, 0.72), available = false),
        MapNode("world_tree", "世界之樹", Pt(0.50, 0.92), available = false),
    )

    /** Where the journey begins: just outside the first region. */
    val start = Pt(0.10, 0.05)

    /** How far around a walked spot the fog is lifted. */
    const val REVEAL_RADIUS = 0.18

    fun node(id: String): MapNode? = nodes.firstOrNull { it.id == id }

    /** The point where region [index] begins (and the previous one ends). */
    private fun entry(index: Int): Pt = when {
        index <= 0 -> start
        index >= nodes.size -> nodes.last().at
        else -> {
            val a = nodes[index - 1].at
            val b = nodes[index].at
            Pt((a.x + b.x) / 2, (a.y + b.y) / 2)
        }
    }

    /** Position along region [regionIndex] after walking [fraction] (0..1) of it. */
    fun pointAt(regionIndex: Int, fraction: Double): Pt {
        val k = regionIndex.coerceIn(0, nodes.lastIndex)
        val t = fraction.coerceIn(0.0, 1.0)
        val from = entry(k)
        val via = nodes[k].at
        val to = entry(k + 1)
        val u = 1 - t
        // Quadratic Bezier that bends through the neighbourhood of the region's node.
        return Pt(
            u * u * from.x + 2 * u * t * via.x + t * t * to.x,
            u * u * from.y + 2 * u * t * via.y + t * t * to.y,
        )
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
