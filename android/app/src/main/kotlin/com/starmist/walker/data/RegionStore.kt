package com.starmist.walker.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.starmist.core.route.TagDiscovery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.regionDataStore by preferencesDataStore(name = "regions")

/** One township's collection: which of its three tags have been found, and in which lap the last one was found. */
data class RegionLog(val mask: Int, val lap: Int, val count: Int = 1)

/**
 * The tags found in each township. In one lap a township gives one tag when it is entered and, if the walk
 * through it is long, a second one about halfway; the third has to wait for the next lap.
 */
class RegionStore(private val context: Context) {
    private val logKey = stringPreferencesKey("log")
    private val lapKey = intPreferencesKey("lap")

    val logs: Flow<Map<String, RegionLog>> = context.regionDataStore.data.map { decode(it[logKey]) }

    /** Starts a new lap: every township can give a new tag again. */
    suspend fun newLap() {
        context.regionDataStore.edit { it[lapKey] = (it[lapKey] ?: 0) + 1 }
    }

    /**
     * Records that the walker reached [stage] in township [key] (1 = just entered, 2 = about halfway through a long
     * one). Returns the index of the tag found now, or null when this lap has already given what that stage gives
     * or all the tags are found.
     */
    suspend fun visit(key: String, tagCount: Int, stage: Int = 1): Int? {
        var found: Int? = null
        context.regionDataStore.edit { prefs ->
            val lap = prefs[lapKey] ?: 0
            val map = decode(prefs[logKey]).toMutableMap()
            val cur = map[key]
            val count = if (cur?.lap == lap) cur.count else 0
            val mask = cur?.mask ?: 0
            val next = TagDiscovery.next(mask, count, tagCount, stage)
            if (next == null) { map[key] = RegionLog(mask, lap, count); prefs[logKey] = encode(map); return@edit }
            map[key] = RegionLog(mask or (1 shl next), lap, count + 1)
            prefs[logKey] = encode(map)
            found = next
        }
        return found
    }

    private fun decode(s: String?): Map<String, RegionLog> =
        s?.lineSequence()?.mapNotNull { l -> l.split('\t').takeIf { it.size >= 3 }?.let { it[0] to RegionLog(it[1].toIntOrNull() ?: 0, it[2].toIntOrNull() ?: 0, it.getOrNull(3)?.toIntOrNull() ?: 1) } }?.toMap() ?: emptyMap()

    private fun encode(m: Map<String, RegionLog>) = m.entries.joinToString("\n") { "${it.key}\t${it.value.mask}\t${it.value.lap}\t${it.value.count}" }
}
