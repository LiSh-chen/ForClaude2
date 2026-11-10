package com.starmist.core.route

/** The figures known about a township the route does not go through. */
data class TownProfile(val key: String, val county: String, val name: String, val highM: Int, val medianM: Int, val areaKm2: Double, val villages: Int)

/**
 * Which place a traveler writes from, and what they say. Only the township's own figures are used as facts; the rest is the
 * traveler's voice (feelings and habits), so nothing unchecked is claimed about a place.
 */
object PostcardTowns {
    /** How much each traveler likes a place (lower is more): their taste, from the figures we have. */
    private fun score(travelerId: String, p: TownProfile): Double = when (travelerId) {
        "ahbo" -> -p.highM.toDouble()
        "xiaohe" -> p.medianM.toDouble()
        "xipo" -> p.medianM + p.areaKm2 / 10.0
        "atie" -> kotlin.math.abs(p.medianM - 150).toDouble()
        "momo" -> -p.villages.toDouble()
        else -> 0.0
    }

    /** One of the places [travelerId] likes best among [candidates] (townships not yet written from); [seed] varies the pick. */
    fun choose(travelerId: String, candidates: List<TownProfile>, seed: Int, preferred: Set<String> = emptySet()): String? {
        if (candidates.isEmpty()) return null
        // a place the traveler has something researched to say about comes first
        val pool = candidates.filter { it.key in preferred }.ifEmpty { candidates }
        val best = pool.sortedWith(compareBy({ score(travelerId, it) }, { it.key })).take(6)
        return best[((seed % best.size) + best.size) % best.size].key
    }

    /** The postcard text: what the traveler says (researched, in their own words) and their name. */
    fun lines(t: Traveler, fact: String): List<String> = listOf(fact, "—— ${t.name}")
}
