package com.starmist.walker.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.exploreDataStore by preferencesDataStore(name = "explore")

/**
 * Free exploration: the walker goes from township to township. [current] is the township last reached, [target] the one being
 * walked to (-1 for none), and the walker is [progressKm] along the straight way from ([startLat], [startLng]) to the target.
 * [visited] are the townships reached this way, [consumedSteps] the all-time step total already turned into distance.
 */
data class ExploreState(
    val active: Boolean,
    val current: Int,
    val target: Int,
    val startLat: Double,
    val startLng: Double,
    val progressKm: Double,
    val consumedSteps: Long,
    val visited: Set<Int>,
    val totalKm: Double,
    /** The way the last leg ran, degrees clockwise from north. */
    val heading: Float,
)

object ExploreCodec {
    fun encode(s: ExploreState) = listOf(
        "v1", if (s.active) "1" else "0", s.current, s.target, s.startLat, s.startLng, s.progressKm, s.consumedSteps,
        s.visited.joinToString(","), s.totalKm, s.heading,
    ).joinToString("|")

    fun decode(text: String?): ExploreState? {
        if (text.isNullOrBlank()) return null
        val p = text.split("|")
        if (p.size != 11 || p[0] != "v1") return null
        return ExploreState(
            active = p[1] == "1",
            current = p[2].toIntOrNull() ?: return null,
            target = p[3].toIntOrNull() ?: return null,
            startLat = p[4].toDoubleOrNull() ?: return null,
            startLng = p[5].toDoubleOrNull() ?: return null,
            progressKm = p[6].toDoubleOrNull() ?: return null,
            consumedSteps = p[7].toLongOrNull() ?: return null,
            visited = p[8].split(",").mapNotNull { it.toIntOrNull() }.toSet(),
            totalKm = p[9].toDoubleOrNull() ?: return null,
            heading = p[10].toFloatOrNull() ?: return null,
        )
    }
}

/** Keeps the free exploration as one small text value, apart from the route. */
class ExploreStore(private val context: Context) {
    private val key = stringPreferencesKey("state")

    val flow: Flow<ExploreState?> = context.exploreDataStore.data.map { ExploreCodec.decode(it[key]) }

    suspend fun read(): ExploreState? = flow.first()

    /** Changes the state with [change] (given the stored one, or null) and returns the new one. */
    suspend fun update(change: (ExploreState?) -> ExploreState?): ExploreState? {
        var result: ExploreState? = null
        context.exploreDataStore.edit { prefs ->
            val next = change(ExploreCodec.decode(prefs[key]))
            if (next == null) prefs.remove(key) else prefs[key] = ExploreCodec.encode(next)
            result = next
        }
        return result
    }
}
