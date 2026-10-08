package com.starmist.walker.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.starmist.core.world.AdvanceResult
import com.starmist.core.world.JourneyEngine
import com.starmist.core.world.JourneyState
import com.starmist.core.world.JourneyStateCodec
import com.starmist.core.world.Resolution
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.random.Random

private val Context.journeyDataStore by preferencesDataStore(name = "journey")

/** Keeps the world state as one small text value, so it needs no database changes. */
class JourneyStore(private val context: Context) {
    private val key = stringPreferencesKey("state")

    /** Null until the first journey has been started. */
    val flow: Flow<JourneyState?> = context.journeyDataStore.data.map { JourneyStateCodec.decode(it[key]) }

    /**
     * Turns the steps counted since the last call into travel. The first call starts the journey from
     * the current total, so steps from before the world existed are not counted retroactively.
     */
    suspend fun settle(engine: JourneyEngine, allTimeSteps: Long): AdvanceResult {
        lateinit var result: AdvanceResult
        context.journeyDataStore.edit { prefs ->
            val current = JourneyStateCodec.decode(prefs[key]) ?: JourneyState.start(Random.nextLong(), allTimeSteps)
            result = engine.advance(current, allTimeSteps)
            prefs[key] = JourneyStateCodec.encode(result.state)
        }
        return result
    }

    suspend fun resolve(engine: JourneyEngine, encounterIndex: Int, chooseA: Boolean): Resolution? {
        var resolution: Resolution? = null
        context.journeyDataStore.edit { prefs ->
            val current = JourneyStateCodec.decode(prefs[key]) ?: return@edit
            resolution = engine.resolve(current, encounterIndex, chooseA)
            resolution?.let { prefs[key] = JourneyStateCodec.encode(it.state) }
        }
        return resolution
    }
}
