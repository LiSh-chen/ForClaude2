package com.starmist.core.world

sealed interface JourneyEvent {
    /** Something simply found on the road. */
    data class Found(val encounterIndex: Int, val regionId: String, val line: String, val item: ItemType) : JourneyEvent

    /** A story card with two choices was added to the inbox; [item] was found regardless. */
    data class ChoiceAppeared(val encounterIndex: Int, val defId: String, val item: ItemType) : JourneyEvent

    data class RegionCleared(val regionId: String) : JourneyEvent

    /** The end of the content that exists so far was reached. */
    data object RouteEnd : JourneyEvent
}

data class AdvanceResult(
    val state: JourneyState,
    val events: List<JourneyEvent>,
    /** Steps that moved the traveller this time. */
    val stepsWalked: Long,
)

/** Where the traveller is on the route, for drawing a progress bar. */
data class RouteProgress(
    val region: Region?,
    /** 0-based index of [region] in the route. */
    val regionIndex: Int,
    val stepsIntoRegion: Long,
    /** Share of the current region walked, 0.0 to 1.0. */
    val regionFraction: Double,
    /** True once the end of the available route is reached. */
    val finished: Boolean,
)

data class Resolution(val state: JourneyState, val def: EncounterDef, val option: ChoiceOption)

data class JourneyConfig(
    /** Steps between encounters. */
    val encounterInterval: Long = 2_000,
    /** After this many encounters without the key item, the next one is guaranteed to drop it. */
    val pityLimit: Int = 4,
    /** How many unanswered cards the inbox holds; older ones are answered automatically. */
    val pendingCap: Int = 8,
    /** Chance (percent) that an encounter is a two-choice card instead of a plain find. */
    val choicePercent: Int = 40,
)

class JourneyEngine(
    private val route: List<Region> = Prologue.regions,
    private val encounters: List<EncounterDef> = Prologue.encounters,
    private val config: JourneyConfig = JourneyConfig(),
) {
    val totalSteps: Long = route.sumOf { it.lengthSteps }

    fun definition(id: String): EncounterDef? = encounters.firstOrNull { it.id == id }

    fun region(id: String): Region? = route.firstOrNull { it.id == id }

    val regions: List<Region> get() = route

    fun progress(position: Long): RouteProgress {
        var start = 0L
        route.forEachIndexed { index, region ->
            val end = start + region.lengthSteps
            if (position < end) {
                val into = (position - start).coerceAtLeast(0)
                return RouteProgress(region, index, into, into.toDouble() / region.lengthSteps, finished = false)
            }
            start = end
        }
        val last = route.lastOrNull()
        return RouteProgress(last, (route.size - 1).coerceAtLeast(0), last?.lengthSteps ?: 0, 1.0, finished = true)
    }

    fun regionAt(position: Long): Region? {
        var start = 0L
        for (region in route) {
            val end = start + region.lengthSteps
            if (position > start && position <= end) return region
            start = end
        }
        return if (position <= 0 && route.isNotEmpty()) route.first() else null
    }

    /**
     * Turns newly counted steps into travel. [allTimeSteps] is the total on record; whatever the state
     * has not consumed yet is walked now, up to the end of the available route. Steps beyond the end
     * are left unconsumed so they count once more content exists.
     */
    fun advance(state: JourneyState, allTimeSteps: Long): AdvanceResult {
        // If the total shrank (a manual correction), do not wait for it to grow back before counting again.
        val base = minOf(state.consumed, allTimeSteps)
        var consumed = base
        var current = state.copy(consumed = base)
        val available = allTimeSteps - base
        var budget = minOf(available, (totalSteps - current.position).coerceAtLeast(0))
        val events = ArrayList<JourneyEvent>()
        var walked = 0L

        while (budget > 0) {
            val sinceEncounter = current.position % config.encounterInterval
            val toNext = config.encounterInterval - sinceEncounter
            val segment = minOf(budget, toNext)
            current = current.copy(position = current.position + segment)
            consumed += segment
            budget -= segment
            walked += segment

            if (segment == toNext) current = encounter(current, events)
            current = clearRegions(current, events)
        }
        return AdvanceResult(current.copy(consumed = consumed), events, walked)
    }

    /** Answers a card from the inbox. Returns null if it is not there any more. */
    fun resolve(state: JourneyState, encounterIndex: Int, chooseA: Boolean): Resolution? {
        val pending = state.pending.firstOrNull { it.encounterIndex == encounterIndex } ?: return null
        val def = encounters.firstOrNull { it.id == pending.defId } ?: return null
        val option = if (chooseA) def.a else def.b
        val next = state
            .copy(pending = state.pending - pending)
            .withItem(option.bonus.item, option.bonus.count)
        return Resolution(next, def, option)
    }

    private fun encounter(state: JourneyState, events: MutableList<JourneyEvent>): JourneyState {
        val index = state.encountersDone
        val region = regionAt(state.position) ?: return state
        val rng = Rng.forEvent(state.seed, index)

        val forceKey = state.missesSinceKey >= config.pityLimit
        val item = if (forceKey) region.keyItem else rng.weighted(region.lootWeights)
        val misses = if (item == region.keyItem) 0 else state.missesSinceKey + 1

        var next = state.withItem(item, 1).copy(encountersDone = index + 1, missesSinceKey = misses)

        val regionDefs = encounters.filter { it.regionId == region.id }
        val isChoice = regionDefs.isNotEmpty() && rng.nextInt(100) < config.choicePercent
        if (isChoice) {
            val def = regionDefs[rng.nextInt(regionDefs.size)]
            next = next.copy(pending = next.pending + PendingChoice(index, def.id))
            events += JourneyEvent.ChoiceAppeared(index, def.id, item)
            next = trimInbox(next)
        } else {
            val line = region.foundLines[rng.nextInt(region.foundLines.size)]
            events += JourneyEvent.Found(index, region.id, line, item)
        }
        return next
    }

    /** Keeps the inbox bounded by answering the oldest cards with their first option. */
    private fun trimInbox(state: JourneyState): JourneyState {
        var current = state
        while (current.pending.size > config.pendingCap) {
            val oldest = current.pending.first()
            current = resolve(current, oldest.encounterIndex, chooseA = true)?.state
                ?: current.copy(pending = current.pending.drop(1))
        }
        return current
    }

    private fun clearRegions(state: JourneyState, events: MutableList<JourneyEvent>): JourneyState {
        var cleared = state.clearedRegions
        var end = 0L
        for (region in route) {
            end += region.lengthSteps
            if (state.position >= end && region.id !in cleared) {
                cleared = cleared + region.id
                events += JourneyEvent.RegionCleared(region.id)
                if (region == route.last()) events += JourneyEvent.RouteEnd
            }
        }
        return if (cleared === state.clearedRegions) state else state.copy(clearedRegions = cleared)
    }
}
