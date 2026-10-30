package com.starmist.walker.ui

import com.starmist.core.route.GeoPoint
import com.starmist.walker.data.ExploreState
import kotlin.math.atan2
import kotlin.math.cos

/** What the explorer sees of itself: where it is, where it is going, what it has explored, as the map and the dashboard show it. */
internal class ExploreView(
    val here: GeoPoint,
    val target: GeoPoint?,
    val targetName: String?,
    val targetKmLeft: Double,
    /** Townships explored by exploring (not counting those the route went through). */
    val visited: Set<Int>,
    val current: Int,
    val neighbors: Set<Int>,
    /** Degrees clockwise from north: the way to the target (or the last way walked). */
    val heading: Float,
    /** How far through the township the walker is in (0..1), and the kilometres walked in it. */
    val townFraction: Float,
    val townKm: Double,
    val followKey: Int,
)

/** Which township is next to which, found from outlines that touch (within about 200 m). */
internal fun TownAreas.neighborLists(): List<Set<Int>> {
    val tol = 0.002
    val cell = HashMap<Long, MutableList<Triple<Int, Double, Double>>>()
    fun key(la: Double, lo: Double) = (Math.floor(la / tol).toLong() shl 32) xor (Math.floor(lo / tol).toLong() and 0xffffffffL)
    towns.forEachIndexed { i, t ->
        for (r in t.rings) { var k = 0; while (k < r.size) { cell.getOrPut(key(r[k], r[k + 1])) { ArrayList() } += Triple(i, r[k], r[k + 1]); k += 2 } }
    }
    val out = List(towns.size) { HashSet<Int>() }
    for ((_, pts) in cell) {
        for ((i, la, lo) in pts) {
            for (dx in -1..1) for (dy in -1..1) {
                val other = cell[key(la + dx * tol, lo + dy * tol)] ?: continue
                for ((j, la2, lo2) in other) if (j != i && Math.abs(la - la2) <= tol && Math.abs(lo - lo2) <= tol) { out[i] += j; out[j] += i }
            }
        }
    }
    return out
}

/** The rules of free exploration: pure functions on [ExploreState]. */
internal object ExploreEngine {
    const val ROAD = 1.02

    fun anchor(a: TownArea) = GeoPoint(a.lat, a.lon)

    private fun townId(areas: TownAreas, p: GeoPoint): Int {
        val id = areas.find(p.lat, p.lng)
        if (id >= 0) return id
        return areas.townAt(p.lat, p.lng)?.let { areas.towns.indexOf(it) } ?: -1
    }

    /** Begins exploring from [at] (the township it lies in is counted as reached). */
    fun start(areas: TownAreas, at: GeoPoint, allTimeSteps: Long, heading: Float): ExploreState? {
        val id = townId(areas, at)
        if (id < 0) return null
        return ExploreState(true, id, -1, at.lat, at.lng, 0.0, allTimeSteps, setOf(id), 0.0, heading)
    }

    fun legKm(s: ExploreState, areas: TownAreas): Double {
        if (s.target !in areas.towns.indices) return 0.0
        return GeoPoint(s.startLat, s.startLng).distanceKm(anchor(areas.towns[s.target])) * ROAD
    }

    fun position(s: ExploreState, areas: TownAreas): GeoPoint {
        val a = GeoPoint(s.startLat, s.startLng)
        if (s.target !in areas.towns.indices) return a
        val b = anchor(areas.towns[s.target])
        val len = legKm(s, areas)
        val u = if (len <= 1e-9) 1.0 else (s.progressKm / len).coerceIn(0.0, 1.0)
        return GeoPoint(a.lat + (b.lat - a.lat) * u, a.lng + (b.lng - a.lng) * u)
    }

    fun bearing(from: GeoPoint, to: GeoPoint): Float {
        val east = (to.lng - from.lng) * cos(Math.toRadians(from.lat))
        val north = to.lat - from.lat
        if (east == 0.0 && north == 0.0) return 0f
        return ((Math.toDegrees(atan2(east, north)) + 360.0) % 360.0).toFloat()
    }

    private fun turn(a: Float, b: Float): Float { var d = Math.abs(a - b) % 360f; if (d > 180f) d = 360f - d; return d }

    /** Walking on after a township was reached with nothing picked: a township not explored yet that lies closest to the way the walker was going. */
    fun autoTarget(s: ExploreState, areas: TownAreas, neighbors: List<Set<Int>>, explored: Set<Int>): Int? {
        val options = neighbors.getOrNull(s.current)?.toList().orEmpty().ifEmpty { return null }
        val here = anchor(areas.towns[s.current])
        val fresh = options.filter { it !in explored }
        val pool = fresh.ifEmpty { options }
        return pool.minByOrNull { turn(bearing(here, anchor(areas.towns[it])), s.heading) }
    }

    /** Heads for township [id] from where the walker is now. */
    fun go(s: ExploreState, areas: TownAreas, id: Int): ExploreState {
        if (id !in areas.towns.indices || id == s.target) return s
        val here = position(s, areas)
        return s.copy(target = id, startLat = here.lat, startLng = here.lng, progressKm = 0.0, heading = bearing(here, anchor(areas.towns[id])))
    }

    /** Everything the map and the dashboard show of the state. */
    fun view(s: ExploreState, areas: TownAreas, neighbors: Set<Int>): ExploreView {
        val here = position(s, areas)
        val target = s.target.takeIf { it in areas.towns.indices }?.let { anchor(areas.towns[it]) }
        val (fraction, km) = townProgress(s, areas)
        return ExploreView(
            here, target, target?.let { areas.towns[s.target].name }, (legKm(s, areas) - s.progressKm).coerceAtLeast(0.0),
            s.visited, s.current, neighbors, if (target != null) bearing(here, target) else s.heading, fraction, km,
            (s.totalKm * 20).toInt(),
        )
    }

    class Advance(val state: ExploreState, val reached: List<Int>)

    /** Turns the steps counted since last time into distance, on to the next township each time one is reached. [explored] are all townships explored so far (route and exploring). */
    fun advance(s: ExploreState, areas: TownAreas, neighbors: List<Set<Int>>, explored: Set<Int>, allTimeSteps: Long, strideM: Double): Advance {
        val base = minOf(s.consumedSteps, allTimeSteps)
        var km = (allTimeSteps - base).coerceAtLeast(0) * strideM / 1000.0
        var st = s.copy(consumedSteps = allTimeSteps)
        val reached = ArrayList<Int>()
        var seen = explored
        var guard = 0
        while (km > 1e-9 && guard++ < 400) {
            if (st.target < 0) {
                val next = autoTarget(st, areas, neighbors, seen) ?: break
                st = go(st, areas, next)
            }
            val left = legKm(st, areas) - st.progressKm
            if (km >= left) {
                km -= left
                val arrived = st.target
                val at = anchor(areas.towns[arrived])
                st = st.copy(current = arrived, target = -1, startLat = at.lat, startLng = at.lng, progressKm = 0.0, visited = st.visited + arrived, totalKm = st.totalKm + left)
                seen = seen + arrived
                reached += arrived
            } else {
                st = st.copy(progressKm = st.progressKm + km, totalKm = st.totalKm + km)
                km = 0.0
            }
        }
        return Advance(st, reached)
    }

    /** How far through the township the walker is in (0..1) and the kilometres walked in it, along the leg being walked. */
    fun townProgress(s: ExploreState, areas: TownAreas): Pair<Float, Double> {
        if (s.target !in areas.towns.indices) return 0f to 0.0
        val len = legKm(s, areas)
        if (len <= 1e-9) return 0f to 0.0
        val step = 0.25
        val n = (len / step).toInt() + 1
        val a = GeoPoint(s.startLat, s.startLng)
        val b = anchor(areas.towns[s.target])
        val ids = IntArray(n + 1) { k -> val u = (k * step / len).coerceIn(0.0, 1.0); townId(areas, GeoPoint(a.lat + (b.lat - a.lat) * u, a.lng + (b.lng - a.lng) * u)) }
        val at = (s.progressKm / step).toInt().coerceIn(0, n)
        var lo = at; while (lo > 0 && ids[lo - 1] == ids[at]) lo--
        var hi = at; while (hi < n && ids[hi + 1] == ids[at]) hi++
        val run = (hi - lo).coerceAtLeast(1)
        return ((at - lo).toFloat() / run).coerceIn(0f, 1f) to (s.progressKm - lo * step).coerceAtLeast(0.0)
    }
}
