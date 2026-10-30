package com.starmist.walker.ui

import com.starmist.core.route.Poi
import com.starmist.core.route.RouteDef
import java.util.concurrent.ConcurrentHashMap

/**
 * When a small sight's stamp is earned: once the walker has gone into the township the sight lies in, and has reached the point of the
 * route nearest to it, whichever comes later. A sight whose township the route never goes through is never stamped (null).
 */
internal object PoiStamps {
    private val cache = ConcurrentHashMap<String, List<Pair<Poi, Double?>>>()

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
        return route.poiStamps.map { (poi, nearest) ->
            val own = townOf(poi, areas)
            val entry = if (own < 0) -1 else along.indexOfFirst { it == own }
            poi to if (entry < 0) null else maxOf(entry * step, nearest)
        }
    }

    /** The sights whose stamp is earned after walking [walkedKm]. */
    fun earned(stamps: List<Pair<Poi, Double?>>, walkedKm: Double): List<Poi> =
        stamps.filter { (_, km) -> km != null && km <= walkedKm + 1e-9 }.map { it.first }
}
