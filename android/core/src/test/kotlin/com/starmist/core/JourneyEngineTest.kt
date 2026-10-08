package com.starmist.core

import com.starmist.core.world.ItemType
import com.starmist.core.world.JourneyConfig
import com.starmist.core.world.JourneyEngine
import com.starmist.core.world.JourneyEvent
import com.starmist.core.world.JourneyState
import com.starmist.core.world.JourneyStateCodec
import com.starmist.core.world.PendingChoice
import com.starmist.core.world.Prologue
import com.starmist.core.world.Region
import com.starmist.core.world.WorldMap
import com.starmist.core.world.Reward
import com.starmist.core.world.Rng
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JourneyEngineTest {
    /** Fixed 2,000-step gaps keep the arithmetic in most tests simple; variable gaps have their own tests. */
    private fun config(choicePercent: Int = 40, pendingCap: Int = 8, pityLimit: Int = 4) = JourneyConfig(
        minInterval = 2_000, maxInterval = 2_000, pityLimit = pityLimit, pendingCap = pendingCap, choicePercent = choicePercent,
    )

    private val engine = JourneyEngine(config = config())
    private val start = JourneyState.start(seed = 42, allTimeSteps = 10_000)

    @Test
    fun `steps before the journey started do not count`() {
        val result = engine.advance(start, allTimeSteps = 10_000)
        assertEquals(0, result.stepsWalked)
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun `a partial interval moves the traveller without an encounter`() {
        val result = engine.advance(start, allTimeSteps = 11_500)
        assertEquals(1_500, result.stepsWalked)
        assertEquals(1_500, result.state.position)
        assertEquals(11_500, result.state.consumed)
        assertTrue(result.events.isEmpty())
        assertEquals(0, result.state.encountersDone)
    }

    @Test
    fun `reaching an interval triggers exactly one encounter and carries leftover progress`() {
        val first = engine.advance(start, allTimeSteps = 11_500)
        val second = engine.advance(first.state, allTimeSteps = 12_100) // 1,500 + 600 = 2,100
        assertEquals(1, second.state.encountersDone)
        assertEquals(2_100, second.state.position)
        assertEquals(1, second.events.size)
    }

    @Test
    fun `batching steps differently gives the same journey`() {
        val oneGo = engine.advance(start, allTimeSteps = 10_000 + 30_000)
        var piecewise = start
        var events = 0
        var total = 10_000L
        repeat(60) {
            total += 500
            val r = engine.advance(piecewise, total)
            piecewise = r.state
            events += r.events.size
        }
        assertEquals(oneGo.state, piecewise)
        assertEquals(oneGo.events.size, events)
    }

    @Test
    fun `same seed and steps always give the same events`() {
        val a = engine.advance(start, 10_000 + 24_000)
        val b = engine.advance(start, 10_000 + 24_000)
        assertEquals(a, b)
        val other = engine.advance(start.copy(seed = 7), 10_000 + 24_000)
        assertTrue(a.events != other.events)
    }

    @Test
    fun `every encounter yields one base item and the inventory matches`() {
        val result = engine.advance(start, 10_000 + 40_000) // 20 encounters
        assertEquals(20, result.state.encountersDone)
        assertEquals(20, result.state.inventory.values.sum())
        val fromEvents = result.events.sumOf {
            when (it) {
                is JourneyEvent.Found -> 1
                is JourneyEvent.ChoiceAppeared -> 1
                else -> 0
            }.toInt()
        }
        assertEquals(20, fromEvents)
    }

    @Test
    fun `pity rule guarantees the key item after enough misses`() {
        // A region whose loot table can never roll the key item on its own.
        val region = Region(
            "r", "R", 100_000, ItemType.LIFE_SEED,
            lootWeights = mapOf(ItemType.MOON_DEW to 1),
            foundLines = listOf("line"), clearedText = "",
        )
        val pityEngine = JourneyEngine(listOf(region), emptyList(), config(choicePercent = 0))
        val result = pityEngine.advance(JourneyState.start(1, 0), 2_000L * 10)
        // Pattern: 4 misses, then a forced key item, repeating: 10 encounters -> 2 key items.
        assertEquals(2, result.state.count(ItemType.LIFE_SEED))
        assertEquals(8, result.state.count(ItemType.MOON_DEW))
        val keyEvents = result.events.filterIsInstance<JourneyEvent.Found>().filter { it.item == ItemType.LIFE_SEED }
        assertEquals(listOf(4, 9), keyEvents.map { it.encounterIndex })
    }

    @Test
    fun `finishing a region is announced and the route end stops progress`() {
        val total = engine.totalSteps
        val result = engine.advance(start, 10_000 + total + 5_000)
        assertEquals(total, result.state.position)
        assertEquals(total, result.stepsWalked)
        assertEquals(setOf(Prologue.forest.id, Prologue.coast.id), result.state.clearedRegions)
        assertEquals(1, result.events.count { it is JourneyEvent.RouteEnd })
        assertEquals(2, result.events.count { it is JourneyEvent.RegionCleared })
        // The extra 5,000 steps stay unused for when more route exists.
        assertEquals(10_000 + total, result.state.consumed)
        val again = engine.advance(result.state, 10_000 + total + 5_000)
        assertEquals(0, again.stepsWalked)
        assertTrue(again.events.isEmpty())
    }

    @Test
    fun `regions are entered in order`() {
        assertEquals(Prologue.forest, engine.regionAt(1))
        assertEquals(Prologue.forest, engine.regionAt(90_000))
        assertEquals(Prologue.coast, engine.regionAt(90_001))
        assertNull(engine.regionAt(engine.totalSteps + 1))
    }

    @Test
    fun `progress reports the region and how far into it`() {
        val early = engine.progress(45_000)
        assertEquals(Prologue.forest, early.region)
        assertEquals(0.5, early.regionFraction, 1e-9)
        assertTrue(!early.finished)

        val boundary = engine.progress(90_000) // exactly the end of the forest: now on the coast
        assertEquals(Prologue.coast, boundary.region)
        assertEquals(1, boundary.regionIndex)
        assertEquals(0.0, boundary.regionFraction, 1e-9)

        val end = engine.progress(engine.totalSteps)
        assertTrue(end.finished)
        assertEquals(1.0, end.regionFraction, 1e-9)
    }

    @Test
    fun `definitions and regions can be looked up`() {
        assertNotNull(engine.definition("forest_fawn"))
        assertNull(engine.definition("nope"))
        assertEquals(Prologue.coast, engine.region("moonlit_coast"))
    }

    @Test
    fun `a shrinking total does not freeze progress`() {
        val walked = engine.advance(start, 10_000 + 5_000).state
        // The user corrected their history down; later steps must count again straight away.
        val shrunk = engine.advance(walked, 12_000)
        assertEquals(0, shrunk.stepsWalked)
        assertEquals(12_000, shrunk.state.consumed)
        val after = engine.advance(shrunk.state, 13_000)
        assertEquals(1_000, after.stepsWalked)
    }

    @Test
    fun `choice cards go to the inbox and can be answered`() {
        val choiceEngine = JourneyEngine(config = config(choicePercent = 100))
        val result = choiceEngine.advance(start, 10_000 + 4_000)
        assertEquals(2, result.state.pending.size)
        val card = result.state.pending.first()
        val before = result.state.inventory.values.sum()

        val resolution = assertNotNull(choiceEngine.resolve(result.state, card.encounterIndex, chooseA = true))
        assertEquals(1, resolution.state.pending.size)
        assertEquals(before + 1, resolution.state.inventory.values.sum())
        assertEquals(resolution.def.a, resolution.option)
        // Answering twice is not possible.
        assertNull(choiceEngine.resolve(resolution.state, card.encounterIndex, chooseA = true))
    }

    @Test
    fun `the inbox is capped by answering the oldest cards automatically`() {
        val choiceEngine = JourneyEngine(config = config(choicePercent = 100, pendingCap = 3))
        val result = choiceEngine.advance(start, 10_000 + 20_000) // 10 cards
        assertEquals(3, result.state.pending.size)
        // The newest three are the ones left.
        assertEquals(listOf(7, 8, 9), result.state.pending.map { it.encounterIndex })
        // Nothing is lost: 10 base items plus 7 automatic bonuses.
        assertEquals(17, result.state.inventory.values.sum())
    }

    @Test
    fun `every encounter definition points at a real region and a distinct pair of options`() {
        val ids = Prologue.regions.map { it.id }.toSet()
        for (def in Prologue.encounters) {
            assertTrue(def.regionId in ids, def.id)
            assertTrue(def.a.label != def.b.label, def.id)
        }
        assertEquals(Prologue.encounters.size, Prologue.encounters.map { it.id }.toSet().size)
        for (region in Prologue.regions) {
            assertTrue(Prologue.encounters.any { it.regionId == region.id })
            assertTrue((region.lootWeights[region.keyItem] ?: 0) > 0, "${region.id} must be able to drop its key item")
        }
    }

    @Test
    fun `weighted picks follow the weights and skip zero weights`() {
        val rng = Rng(5)
        val counts = HashMap<String, Int>()
        repeat(3_000) { counts.merge(rng.weighted(mapOf("a" to 3, "b" to 1, "c" to 0)), 1, Int::plus) }
        assertEquals(null, counts["c"])
        val a = counts.getValue("a")
        assertTrue(a in 2_100..2_400, "a was $a")
    }

    @Test
    fun `state survives a round trip through text`() {
        val result = JourneyEngine(config = config(choicePercent = 60)).advance(start, 10_000 + 70_000)
        val decoded = JourneyStateCodec.decode(JourneyStateCodec.encode(result.state))
        assertEquals(result.state, decoded)
    }

    @Test
    fun `empty or damaged text decodes to nothing`() {
        assertNull(JourneyStateCodec.decode(null))
        assertNull(JourneyStateCodec.decode(""))
        assertNull(JourneyStateCodec.decode("v1|oops"))
        assertNull(JourneyStateCodec.decode("v3|1|2|3|4|5|6|||"))
        assertNull(JourneyStateCodec.decode("v1|1|2|3|4|5|NOT_AN_ITEM=1||"))
        assertNull(JourneyStateCodec.decode("v2|1|2|3|4|5|6|NOT_AN_ITEM=1||"))
    }

    @Test
    fun `codec keeps pending cards and cleared regions`() {
        val state = JourneyState(
            seed = -5, consumed = 123, position = 456, nextEncounterAt = 900, encountersDone = 7, missesSinceKey = 2,
            inventory = mapOf(ItemType.LIFE_SEED to 3, ItemType.MOON_DEW to 1),
            pending = listOf(PendingChoice(3, "forest_fawn"), PendingChoice(5, "coast_whale")),
            clearedRegions = setOf("whispering_forest"),
        )
        assertEquals(state, JourneyStateCodec.decode(JourneyStateCodec.encode(state)))
    }

    // ---- variable gaps between encounters ----------------------------------------------------

    private val variable = JourneyEngine() // default config: 3,500 to 5,500 steps

    @Test
    fun `gaps between encounters stay within the configured range and actually vary`() {
        var state = JourneyState.start(seed = 11, allTimeSteps = 0)
        var total = 0L
        var lastEncounterPosition = 0L
        val gaps = ArrayList<Long>()
        while (gaps.size < 30) {
            total += 50
            val before = state.encountersDone
            state = variable.advance(state, total).state
            if (state.encountersDone > before) {
                gaps += state.position - lastEncounterPosition
                lastEncounterPosition = state.position
            }
        }
        // Positions are sampled every 50 steps, so allow that much slack on each side.
        assertTrue(gaps.all { it in 3_450..5_600 }, gaps.toString())
        assertTrue(gaps.toSet().size > 10, "gaps should differ: $gaps")
        val average = gaps.average()
        assertTrue(average in 4_000.0..5_000.0, "average gap was $average")
    }

    @Test
    fun `variable gaps do not depend on how steps are batched`() {
        val oneGo = variable.advance(start, 10_000 + 60_000)
        var piecewise = start
        var total = 10_000L
        repeat(120) {
            total += 500
            piecewise = variable.advance(piecewise, total).state
        }
        assertEquals(oneGo.state, piecewise)
        assertTrue(oneGo.state.encountersDone in 10..20, "got ${oneGo.state.encountersDone}")
    }

    @Test
    fun `gaps differ between journeys but are repeatable for one seed`() {
        fun firstPositions(seed: Long): Long =
            variable.advance(JourneyState.start(seed, 0), 0).state.let {
                // The first encounter is due at nextEncounterAt once the engine has drawn it.
                variable.advance(JourneyState.start(seed, 0), 6_000).state.nextEncounterAt
            }
        assertEquals(firstPositions(3), firstPositions(3))
        val distinct = (1L..12L).map { firstPositions(it) }.toSet()
        assertTrue(distinct.size > 3, distinct.toString())
    }

    @Test
    fun `old saved journeys without a next encounter are upgraded`() {
        val old = "v1|9|100|5000|3|1|MOON_DEW=2||whispering_forest"
        val decoded = assertNotNull(JourneyStateCodec.decode(old))
        assertEquals(-1, decoded.nextEncounterAt)
        assertEquals(5_000, decoded.position)
        assertEquals(3, decoded.encountersDone)
        // The engine draws the missing gap when it next runs, and carries on from there.
        val next = variable.advance(decoded, 100 + 10_000)
        assertTrue(next.state.nextEncounterAt > next.state.position)
    }

    @Test
    fun `an encounter on the last step of a region is announced before the region clears`() {
        val region = Region(
            "r", "R", 4_000, ItemType.LIFE_SEED, mapOf(ItemType.LIFE_SEED to 1),
            foundLines = listOf("line"), clearedText = "",
        )
        val small = JourneyEngine(listOf(region), emptyList(), config(choicePercent = 0))
        val result = small.advance(JourneyState.start(1, 0), 10_000)
        val kinds = result.events.map { it::class.simpleName }
        assertEquals(listOf("Found", "Found", "RegionCleared", "RouteEnd"), kinds)
    }

    // ---- the world map and its fog -----------------------------------------------------------

    @Test
    fun `every playable region has a place on the map in the same order`() {
        val ids = Prologue.regions.map { it.id }
        assertEquals(ids, WorldMap.nodes.take(ids.size).map { it.id })
        assertTrue(WorldMap.nodes.take(ids.size).all { it.available })
        assertTrue(WorldMap.nodes.drop(ids.size).none { it.available })
        assertTrue(WorldMap.nodes.all { it.at.x in 0.0..1.0 && it.at.y in 0.0..1.0 })
    }

    @Test
    fun `at the start only the first region is in view`() {
        val walked = WorldMap.walkedPoints(regionIndex = 0, fraction = 0.0, finished = false)
        assertTrue(WorldMap.isRevealed(WorldMap.start, walked))
        assertTrue(!WorldMap.isRevealed(WorldMap.nodes[3].at, walked))
        assertTrue(!WorldMap.isRevealed(WorldMap.nodes.last().at, walked))
    }

    @Test
    fun `walking reveals the way and finished regions stay revealed`() {
        val early = WorldMap.walkedPoints(0, 0.1, false)
        val later = WorldMap.walkedPoints(1, 0.5, false)
        assertTrue(!WorldMap.isRevealed(WorldMap.nodes[1].at, early))
        assertTrue(WorldMap.isRevealed(WorldMap.nodes[1].at, later))
        assertTrue(WorldMap.isRevealed(WorldMap.nodes[0].at, later), "the first region stays visible")
    }

    @Test
    fun `after the last open region the next place is glimpsed but not the far end`() {
        val engine = JourneyEngine()
        val progress = engine.progress(engine.totalSteps)
        val walked = WorldMap.walkedPoints(progress.regionIndex, progress.regionFraction, progress.finished)
        assertTrue(WorldMap.isRevealed(WorldMap.nodes[2].at, walked), "coral isles are in sight")
        assertTrue(!WorldMap.isRevealed(WorldMap.nodes.last().at, walked))
    }

    @Test
    fun `the traveller moves continuously along each region`() {
        var last = WorldMap.pointAt(0, 0.0)
        for (k in 0 until WorldMap.nodes.size) {
            for (i in 1..20) {
                val p = WorldMap.pointAt(k, i / 20.0)
                assertTrue(p.distanceTo(last) < 0.25, "jump at region $k step $i")
                last = p
            }
        }
        assertEquals(WorldMap.nodes.last().at.x, last.x, 1e-9)
    }
}
