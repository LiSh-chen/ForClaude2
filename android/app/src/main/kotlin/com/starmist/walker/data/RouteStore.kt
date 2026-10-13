package com.starmist.walker.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.starmist.core.route.RouteAdvance
import com.starmist.core.route.RouteDef
import com.starmist.core.route.RouteState
import com.starmist.core.route.RouteStateCodec
import com.starmist.core.route.RouteTracker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.routeDataStore by preferencesDataStore(name = "routes")

/** Keeps the real-world route being walked as one small text value. Cards and stamps follow from the distance. */
class RouteStore(private val context: Context) {
    private val key = stringPreferencesKey("state")

    /** Null until a route has been chosen. */
    val flow: Flow<RouteState?> = context.routeDataStore.data.map { RouteStateCodec.decode(it[key]) }

    /** Starts [route] from the steps on record now; earlier steps do not count. */
    suspend fun start(route: RouteDef, allTimeSteps: Long) {
        context.routeDataStore.edit { it[key] = RouteStateCodec.encode(RouteTracker.start(route, allTimeSteps)) }
    }

    /** Turns steps counted since the last call into distance along the active route. */
    suspend fun settle(route: RouteDef, allTimeSteps: Long, strideM: Double): RouteAdvance? {
        var result: RouteAdvance? = null
        context.routeDataStore.edit { prefs ->
            val current = RouteStateCodec.decode(prefs[key]) ?: return@edit
            if (current.routeId != route.id) return@edit
            val advance = RouteTracker.advance(route, current, allTimeSteps, strideM)
            prefs[key] = RouteStateCodec.encode(advance.state)
            result = advance
        }
        return result
    }

    suspend fun clear() {
        context.routeDataStore.edit { it.remove(key) }
    }
}
