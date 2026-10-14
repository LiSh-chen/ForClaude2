package com.starmist.core.route

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** How close the walker must be to see a small sight. */
const val POI_RANGE_KM = 12.0

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

/** What a small roadside sight looks like on the map. */
enum class PoiKind { HARBOR, TEMPLE, MUSEUM, SPRING, WATERFALL, PARK, THEATER, FORT, LIGHTHOUSE }

/** A small sight drawn on the map only while the walker is near; it gives no card. */
data class Poi(val id: String, val name: String, val at: GeoPoint, val kind: PoiKind)

/** A place to stop and restock in a long stretch between cards: a railway station with its opening year. */
data class Supply(val id: String, val name: String, val opened: String)

/**
 * One vertex of the route; [landmark] is set where a card is collected. When [transit] is set (for
 * example "搭火車"), the way from the previous stop to this one cannot be walked: it is crossed by
 * that transport at no cost in steps.
 */
data class Stop(
    val name: String,
    val at: GeoPoint,
    val landmark: Landmark? = null,
    val transit: String? = null,
    /** A shaping point that only bends the line; it is never announced or collected. */
    val via: Boolean = false,
    val supply: Supply? = null,
)

/**
 * A walkable real-world route. Distances are along the straight lines between stops, stretched by
 * [roadFactor] because real roads wind; they are approximate, which the app says ("about"). Legs
 * marked as transit add no distance.
 */
data class RouteDef(
    val id: String,
    val region: String,
    val name: String,
    val description: String,
    val stops: List<Stop>,
    val roadFactor: Double = 1.2,
    /** Small sights near the way; shown on the map when the walker is within [POI_RANGE_KM]. */
    val pois: List<Poi> = emptyList(),
    /** Routes that are the same way walked in the other direction share a family. */
    val family: String = id,
    val direction: String = "",
) {
    init {
        require(stops.size >= 2) { "a route needs at least two stops" }
    }

    /** Distance from the start to each stop, in km. */
    val cumulativeKm: List<Double> = buildList {
        var total = 0.0
        add(0.0)
        for (i in 1 until stops.size) {
            if (stops[i].transit == null) total += stops[i - 1].at.distanceKm(stops[i].at) * roadFactor
            add(total)
        }
    }

    val totalKm: Double get() = cumulativeKm.last()

    /** Cards in route order, each with the distance at which it is collected. */
    val cards: List<Pair<Landmark, Double>> =
        stops.mapIndexedNotNull { i, stop -> stop.landmark?.let { it to cumulativeKm[i] } }

    /** Supply stops in route order, each with the distance at which it is reached. */
    val supplies: List<Pair<Supply, Double>> =
        stops.mapIndexedNotNull { i, stop -> stop.supply?.let { it to cumulativeKm[i] } }

    /** Supply stops reached after walking [walkedKm]. */
    fun suppliesReached(walkedKm: Double): List<Supply> = supplies.filter { it.second <= walkedKm + 1e-9 }.map { it.first }

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
                // A transit leg has no length: the walker is at its end as soon as the start is reached.
                if (span == 0.0 && stops[i].transit != null && d >= cumulativeKm[i]) return stops[i].at
                val u = if (span == 0.0) 0.0 else (d - cumulativeKm[i - 1]) / span
                val a = stops[i - 1].at
                val b = stops[i].at
                return GeoPoint(a.lat + (b.lat - a.lat) * u, a.lng + (b.lng - a.lng) * u)
            }
        }
        return stops.last().at
    }

    /** Small sights within [POI_RANGE_KM] of [at], nearest first. */
    fun poisNear(at: GeoPoint, rangeKm: Double = POI_RANGE_KM): List<Pair<Poi, Double>> =
        pois.map { it to it.at.distanceKm(at) }.filter { it.second <= rangeKm }.sortedBy { it.second }

    /**
     * The same loop walked the other way: it starts at the first stop, visits the middle stops in
     * reverse and ends at the last stop. A train or ferry leg stays a train or ferry leg, so the
     * flag moves to the stop that is now reached by that leg.
     */
    fun counterClockwise(newId: String, newName: String, newDescription: String, directionName: String): RouteDef {
        val n = stops.size - 1
        val first = stops.first()
        val last = stops.last()
        // Forward stop j (1..n-1) is now entered from forward stop j + 1, so it inherits that leg's transport.
        val reversed = (n - 1 downTo 1).map { j ->
            stops[j].copy(transit = if (j + 1 <= n - 1) stops[j + 1].transit else null)
        }
        return copy(
            id = newId,
            name = newName,
            description = newDescription,
            stops = listOf(first.copy(transit = null)) + reversed + listOf(last.copy(transit = null)),
            direction = directionName,
        )
    }

    /** The transport used to reach stop [index], if that leg is not walked. */
    fun transitInto(index: Int): String? = stops.getOrNull(index)?.transit

    /** The stop most recently passed, for "now near ..." wording. */
    fun lastPassed(walkedKm: Double): Stop =
        stops.take(cumulativeKm.indexOfLast { it <= walkedKm + 1e-9 }.coerceAtLeast(0) + 1).last { !it.via }
}
