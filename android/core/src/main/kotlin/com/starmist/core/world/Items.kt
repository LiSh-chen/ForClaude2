package com.starmist.core.world

/** Things picked up on the road; used later to restore the land at shrines. */
enum class ItemType(val displayName: String) {
    LIFE_SEED("生命之種"),
    MOON_DEW("月光露"),
    TIDE_PEARL("潮汐珠"),
    RAINBOW_CORAL("彩虹珊瑚芽"),
    WIND_FEATHER("風之羽"),
    RUNE_SHARD("符文碎片"),
}

data class Reward(val item: ItemType, val count: Int = 1)

/** Deterministic random numbers (SplitMix64), so a journey can be rebuilt from its seed. */
class Rng(private var state: Long) {
    fun nextLong(): Long {
        state += -7046029254386353131L
        var z = state
        z = (z xor (z ushr 30)) * -4658895280553007687L
        z = (z xor (z ushr 27)) * -7723592293110705685L
        return z xor (z ushr 31)
    }

    /** Uniform in [0, bound). */
    fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be positive" }
        return ((nextLong() ushr 1) % bound).toInt()
    }

    /** Picks a key with probability proportional to its weight; zero weights are never picked. */
    fun <T> weighted(weights: Map<T, Int>): T {
        val total = weights.values.sumOf { it.coerceAtLeast(0) }
        require(total > 0) { "no positive weights" }
        var roll = nextInt(total)
        for ((key, weight) in weights) {
            val w = weight.coerceAtLeast(0)
            if (roll < w) return key
            roll -= w
        }
        error("unreachable")
    }

    companion object {
        /** The generator for the n-th event of a journey; independent of how steps were batched. */
        fun forEvent(seed: Long, index: Int): Rng {
            val rng = Rng(seed xor (index.toLong() * -7046029254386353131L))
            rng.nextLong() // mix the seed well before first use
            return rng
        }
    }
}
