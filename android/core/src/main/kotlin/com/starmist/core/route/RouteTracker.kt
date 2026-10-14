package com.starmist.core.route

/**
 * Where a walker is on a real-world route. Steps are turned into kilometres once, at the stride in
 * force at the time, so changing the stride later never moves cards that were already collected.
 */
data class RouteState(
    val routeId: String,
    /** All-time step total that has already been turned into distance. */
    val consumedSteps: Long,
    val walkedKm: Double = 0.0,
)

sealed interface RouteEvent {
    data class CardCollected(val landmark: Landmark, val atKm: Double) : RouteEvent
    data class StampCollected(val supply: Supply, val atKm: Double) : RouteEvent
    /** The walker went over a leg crossed by train or ferry. */
    data class Transit(val by: String, val into: String) : RouteEvent
    data object Finished : RouteEvent
}

data class RouteAdvance(val state: RouteState, val events: List<RouteEvent>, val addedKm: Double)

object RouteTracker {
    fun start(route: RouteDef, allTimeSteps: Long) = RouteState(route.id, consumedSteps = allTimeSteps)

    /**
     * Adds the steps counted since the last call as distance and reports every card, stamp and
     * train or ferry crossing passed on the way, in route order.
     */
    fun advance(route: RouteDef, state: RouteState, allTimeSteps: Long, strideM: Double): RouteAdvance {
        // If the total shrank (a manual correction), do not wait for it to grow back.
        val base = minOf(state.consumedSteps, allTimeSteps)
        val steps = (allTimeSteps - base).coerceAtLeast(0)
        val before = state.walkedKm
        val after = (before + steps * strideM / 1000.0).coerceAtMost(route.totalKm)
        val events = ArrayList<RouteEvent>()
        for ((i, stop) in route.stops.withIndex()) {
            val at = route.cumulativeKm[i]
            // The first stop sits at kilometre 0: it is collected with the first steps, once.
            val firstStop = at == 0.0 && before == 0.0 && steps > 0
            if (!((at > before + 1e-9 || firstStop) && at <= after + 1e-9)) continue
            if (!stop.via) stop.transit?.let { events += RouteEvent.Transit(it, stop.name) }
            stop.landmark?.let { events += RouteEvent.CardCollected(it, at) }
            stop.supply?.let { events += RouteEvent.StampCollected(it, at) }
        }
        if (before < route.totalKm - 1e-9 && after >= route.totalKm - 1e-9) events += RouteEvent.Finished
        return RouteAdvance(RouteState(state.routeId, allTimeSteps, after), events, after - before)
    }
}

/** Text form for storing in preferences. Unknown or damaged input decodes to null. */
object RouteStateCodec {
    fun encode(state: RouteState): String = "v1|${state.routeId}|${state.consumedSteps}|${state.walkedKm}"

    fun decode(text: String?): RouteState? {
        if (text.isNullOrBlank()) return null
        val parts = text.split("|")
        if (parts.size != 4 || parts[0] != "v1") return null
        return RouteState(
            routeId = parts[1],
            consumedSteps = parts[2].toLongOrNull() ?: return null,
            walkedKm = parts[3].toDoubleOrNull()?.takeIf { it >= 0 } ?: return null,
        )
    }
}

/** The routes the app offers, by region. */
object Routes {
    /** Every route offered; the two directions of one loop share a [RouteDef.family]. */
    val all: List<RouteDef> = listOf(TaiwanLoop.route, TaiwanLoop.counterClockwise)

    /** Routes grouped by family, in the order they are offered. */
    val families: List<List<RouteDef>> get() = all.groupBy { it.family }.values.toList()

    fun byId(id: String): RouteDef? = all.firstOrNull { it.id == id }
}
