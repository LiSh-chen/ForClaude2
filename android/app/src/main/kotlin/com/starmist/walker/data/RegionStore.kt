package com.starmist.walker.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.regionDataStore by preferencesDataStore(name = "regions")

/** One township's collection: which of its three tags have been found, and in which lap the last one was found. */
data class RegionLog(val mask: Int, val lap: Int)

/**
 * The tags found in each township. A visit (one lap of the route) finds one new tag in a township, so it takes
 * up to three laps to see all three.
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
     * Records a visit to [key] with [tagCount] tags. Returns the index of the tag found now, or null when this lap
     * already found one here or all the tags are found.
     */
    suspend fun visit(key: String, tagCount: Int): Int? {
        var found: Int? = null
        context.regionDataStore.edit { prefs ->
            val lap = prefs[lapKey] ?: 0
            val map = decode(prefs[logKey]).toMutableMap()
            val cur = map[key]
            if (cur != null && cur.lap == lap) return@edit
            val mask = cur?.mask ?: 0
            val next = (0 until tagCount).firstOrNull { mask and (1 shl it) == 0 }
            if (next == null) { map[key] = RegionLog(mask, lap); prefs[logKey] = encode(map); return@edit }
            map[key] = RegionLog(mask or (1 shl next), lap)
            prefs[logKey] = encode(map)
            found = next
        }
        return found
    }

    private fun decode(s: String?): Map<String, RegionLog> =
        s?.lineSequence()?.mapNotNull { l -> l.split('\t').takeIf { it.size == 3 }?.let { it[0] to RegionLog(it[1].toIntOrNull() ?: 0, it[2].toIntOrNull() ?: 0) } }?.toMap() ?: emptyMap()

    private fun encode(m: Map<String, RegionLog>) = m.entries.joinToString("\n") { "${it.key}\t${it.value.mask}\t${it.value.lap}" }
}
