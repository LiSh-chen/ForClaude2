package com.starmist.walker.ui

import com.starmist.core.route.Poi
import com.starmist.core.route.RouteDef
import java.util.concurrent.ConcurrentHashMap

/**
 * When a small sight's stamp is earned: once the walker is past the middle of the walk through the township the sight lies in (half
 * of the stretch of route inside it). A sight whose township the route never goes through is never stamped (null).
 */
internal object PoiStamps {
    private val cache = ConcurrentHashMap<String, List<Pair<Poi, Double?>>>()

    /** Every small sight of [route] with the kilometre of its stamp, in the order they are earned (those never earned last). */
    fun of(route: RouteDef, areas: TownAreas): List<Pair<Poi, Double?>> = cache.getOrPut(route.id) { compute(route, areas) }

    private fun townOf(poi: Poi, areas: TownAreas): Int {
        val id = areas.find(poi.at.lat, poi.at.lng)
        if (id >= 0) return id
        return areas.townAt(poi.at.lat, poi.at.lng)?.let { areas.towns.indexOf(it) } ?: -1
    }

    private fun compute(route: RouteDef, areas: TownAreas): List<Pair<Poi, Double?>> {
        val step = 0.25
        val total = route.totalKm
        val along = IntArray((total / step).toInt() + 2) { k -> route.positionAt((k * step).coerceAtMost(total)).let { areas.find(it.lat, it.lng) } }
        return route.pois.map { poi ->
            val own = townOf(poi, areas)
            val entry = if (own < 0) -1 else along.indexOfFirst { it == own }
            if (entry < 0) poi to null else {
                var exit = entry
                while (exit + 1 < along.size && along[exit + 1] == own) exit++
                poi to (entry + (exit - entry) / 2.0) * step
            }
        }.sortedBy { it.second ?: Double.MAX_VALUE }
    }

    /** The sights whose stamp is earned after walking [walkedKm]. */
    fun earned(stamps: List<Pair<Poi, Double?>>, walkedKm: Double): List<Poi> =
        stamps.filter { (_, km) -> km != null && km <= walkedKm + 1e-9 }.map { it.first }
}
