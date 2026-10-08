package com.starmist.core.world

data class PendingChoice(val encounterIndex: Int, val defId: String)

/**
 * Where the traveller is and what they carry. Everything the world needs is in here plus the seed,
 * so it is small to store and the same steps always lead to the same journey.
 */
data class JourneyState(
    val seed: Long,
    /** All-time step total that has already been turned into travel. */
    val consumed: Long = 0,
    /** Steps walked along the route so far. */
    val position: Long = 0,
    /** Route position of the next encounter; -1 until the engine has drawn it. */
    val nextEncounterAt: Long = -1,
    val encountersDone: Int = 0,
    /** Encounters in a row that did not drop the region's key item (drives the pity rule). */
    val missesSinceKey: Int = 0,
    val inventory: Map<ItemType, Int> = emptyMap(),
    val pending: List<PendingChoice> = emptyList(),
    val clearedRegions: Set<String> = emptySet(),
) {
    fun count(item: ItemType): Int = inventory[item] ?: 0

    internal fun withItem(item: ItemType, count: Int): JourneyState =
        copy(inventory = inventory + (item to (count(item) + count)))

    companion object {
        /** A fresh journey that starts counting from the steps already on record. */
        fun start(seed: Long, allTimeSteps: Long) = JourneyState(seed = seed, consumed = allTimeSteps)
    }
}

/** Text form for storing in preferences. Unknown or damaged input decodes to null. */
object JourneyStateCodec {
    private const val VERSION = "v2"
    private const val LEGACY_VERSION = "v1"

    fun encode(state: JourneyState): String = listOf(
        VERSION,
        state.seed.toString(),
        state.consumed.toString(),
        state.position.toString(),
        state.nextEncounterAt.toString(),
        state.encountersDone.toString(),
        state.missesSinceKey.toString(),
        state.inventory.entries.filter { it.value > 0 }.joinToString(",") { "${it.key.name}=${it.value}" },
        state.pending.joinToString(",") { "${it.encounterIndex}/${it.defId}" },
        state.clearedRegions.sorted().joinToString(","),
    ).joinToString("|")

    fun decode(text: String?): JourneyState? {
        if (text.isNullOrBlank()) return null
        val raw = text.split("|")
        // The first format had no "next encounter" field; the engine draws it again on first use.
        val parts = when {
            raw.size == 10 && raw[0] == VERSION -> raw
            raw.size == 9 && raw[0] == LEGACY_VERSION -> raw.take(4) + "-1" + raw.drop(4)
            else -> return null
        }
        return try {
            JourneyState(
                seed = parts[1].toLong(),
                consumed = parts[2].toLong(),
                position = parts[3].toLong(),
                nextEncounterAt = parts[4].toLong(),
                encountersDone = parts[5].toInt(),
                missesSinceKey = parts[6].toInt(),
                inventory = parts[7].split(",").filter { it.isNotEmpty() }.associate {
                    val (name, count) = it.split("=")
                    ItemType.valueOf(name) to count.toInt()
                },
                pending = parts[8].split(",").filter { it.isNotEmpty() }.map {
                    val (index, id) = it.split("/", limit = 2)
                    PendingChoice(index.toInt(), id)
                },
                clearedRegions = parts[9].split(",").filter { it.isNotEmpty() }.toSet(),
            )
        } catch (e: IllegalArgumentException) {
            null
        } catch (e: IndexOutOfBoundsException) {
            null
        }
    }
}
