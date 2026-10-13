package com.starmist.core.route

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoPoint(val lat: Double, val lng: Double) {
    /** Great-circle distance in kilometres. */
    fun distanceKm(other: GeoPoint): Double {
        val r = 6371.0088
        val p1 = Math.toRadians(lat)
        val p2 = Math.toRadians(other.lat)
        val dp = p2 - p1
        val dl = Math.toRadians(other.lng - lng)
        val a = sin(dp / 2) * sin(dp / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
        return 2 * r * asin(sqrt(a))
    }
}

/** A collectable place on a route. [blurb] is exactly ten characters about what makes it special. */
data class Landmark(val id: String, val name: String, val blurb: String)

/** One vertex of the route; [landmark] is set where a card is collected. */
data class Stop(val name: String, val at: GeoPoint, val landmark: Landmark? = null)

/**
 * A walkable real-world route. Distances are along the straight lines between stops, stretched by
 * [roadFactor] because real roads wind; they are approximate, which the app says ("about").
 */
data class RouteDef(
    val id: String,
    val region: String,
    val name: String,
    val description: String,
    val stops: List<Stop>,
    val roadFactor: Double = 1.2,
) {
    init {
        require(stops.size >= 2) { "a route needs at least two stops" }
    }

    /** Distance from the start to each stop, in km. */
    val cumulativeKm: List<Double> = buildList {
        var total = 0.0
        add(0.0)
        for (i in 1 until stops.size) {
            total += stops[i - 1].at.distanceKm(stops[i].at) * roadFactor
            add(total)
        }
    }

    val totalKm: Double get() = cumulativeKm.last()

    /** Cards in route order, each with the distance at which it is collected. */
    val cards: List<Pair<Landmark, Double>> =
        stops.mapIndexedNotNull { i, stop -> stop.landmark?.let { it to cumulativeKm[i] } }

    /** Cards collected after walking [walkedKm]. */
    fun collected(walkedKm: Double): List<Landmark> = cards.filter { it.second <= walkedKm + 1e-9 }.map { it.first }

    /** The next card still to be reached and how far away it is, or null at the end. */
    fun next(walkedKm: Double): Pair<Landmark, Double>? =
        cards.firstOrNull { it.second > walkedKm + 1e-9 }?.let { it.first to it.second - walkedKm }

    /** Where on the earth the walker is after [walkedKm] (clamped to the route). */
    fun positionAt(walkedKm: Double): GeoPoint {
        val d = walkedKm.coerceIn(0.0, totalKm)
        for (i in 1 until stops.size) {
            if (d <= cumulativeKm[i]) {
                val span = cumulativeKm[i] - cumulativeKm[i - 1]
                val u = if (span == 0.0) 0.0 else (d - cumulativeKm[i - 1]) / span
                val a = stops[i - 1].at
                val b = stops[i].at
                return GeoPoint(a.lat + (b.lat - a.lat) * u, a.lng + (b.lng - a.lng) * u)
            }
        }
        return stops.last().at
    }

    /** The stop most recently passed, for "now near ..." wording. */
    fun lastPassed(walkedKm: Double): Stop =
        stops[cumulativeKm.indexOfLast { it <= walkedKm + 1e-9 }.coerceAtLeast(0)]
}
