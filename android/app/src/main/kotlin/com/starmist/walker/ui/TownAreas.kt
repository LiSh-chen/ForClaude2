package com.starmist.walker.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import com.starmist.core.route.RouteDef
import java.io.DataInputStream
import java.util.zip.GZIPInputStream

/** A township or district (鄉鎮市區): outline rings as (lat, lon) pairs and a point well inside it for its name. */
class TownArea(val name: String, val lat: Double, val lon: Double, val rings: List<DoubleArray>) {
    private val minLat = rings.minOf { r -> (0 until r.size step 2).minOf { r[it] } }
    private val maxLat = rings.maxOf { r -> (0 until r.size step 2).maxOf { r[it] } }
    private val minLon = rings.minOf { r -> (1 until r.size step 2).minOf { r[it] } }
    private val maxLon = rings.maxOf { r -> (1 until r.size step 2).maxOf { r[it] } }

    fun contains(la: Double, lo: Double): Boolean {
        if (la < minLat || la > maxLat || lo < minLon || lo > maxLon) return false
        for (r in rings) {
            var inside = false
            var j = r.size - 2
            var i = 0
            while (i < r.size) {
                val ay = r[i]; val ax = r[i + 1]; val by = r[j]; val bx = r[j + 1]
                if ((ay > la) != (by > la) && lo < (bx - ax) * (la - ay) / (by - ay) + ax) inside = !inside
                j = i; i += 2
            }
            if (inside) return true
        }
        return false
    }
}

/** The townships of the island (res/raw/town_areas.bin, see tools/make_town_areas.py). */
class TownAreas internal constructor(val towns: List<TownArea>) {
    private val paths = HashMap<Int, Path>()
    private var pathsW = -1f

    fun find(la: Double, lo: Double): Int = towns.indexOfFirst { it.contains(la, lo) }

    /** The outline of township [id] for a map drawn [w] by [h]; kept until the size changes. */
    fun path(id: Int, w: Float, h: Float): Path {
        if (pathsW != w) { paths.clear(); pathsW = w }
        return paths.getOrPut(id) {
            val p = Path()
            for (r in towns[id].rings) {
                var i = 0
                while (i < r.size) {
                    val x = ((r[i + 1] - MAP_LON_MIN) / (MAP_LON_MAX - MAP_LON_MIN) * w).toFloat()
                    val y = ((MAP_LAT_MAX - r[i]) / (MAP_LAT_MAX - MAP_LAT_MIN) * h).toFloat()
                    if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                    i += 2
                }
                p.close()
            }
            p
        }
    }

    /** The townships the walk has passed through so far (sampled every half kilometre). */
    fun visited(route: RouteDef, walkedKm: Double): Set<Int> {
        val seen = LinkedHashSet<Int>()
        var km = 0.0
        while (km <= walkedKm + 1e-6) {
            val g = route.positionAt(km)
            val id = find(g.lat, g.lng)
            if (id >= 0) seen += id
            km += 0.5
        }
        val g = route.positionAt(walkedKm)
        find(g.lat, g.lng).let { if (it >= 0) seen += it }
        return seen
    }

    companion object {
        fun load(context: Context): TownAreas {
            val id = context.resources.getIdentifier("town_areas", "raw", context.packageName)
            if (id == 0) return TownAreas(emptyList())
            return runCatching {
                DataInputStream(GZIPInputStream(context.resources.openRawResource(id)).buffered()).use { s ->
                    val n = s.readUnsignedShort()
                    List(n) {
                        val nameBytes = ByteArray(s.readUnsignedByte()); s.readFully(nameBytes)
                        val lat = s.readInt() / 1e5; val lon = s.readInt() / 1e5
                        val rings = List(s.readUnsignedShort()) {
                            val c = s.readUnsignedShort()
                            var la = s.readInt(); var lo = s.readInt()
                            val out = DoubleArray(c * 2); out[0] = la / 1e5; out[1] = lo / 1e5
                            for (k in 1 until c) { la += s.readShort(); lo += s.readShort(); out[k * 2] = la / 1e5; out[k * 2 + 1] = lo / 1e5 }
                            out
                        }
                        TownArea(String(nameBytes, Charsets.UTF_8), lat, lon, rings)
                    }
                }
            }.getOrDefault(emptyList()).let { TownAreas(it) }
        }
    }
}

@Composable
fun rememberTownAreas(): TownAreas {
    val context = LocalContext.current
    return remember(context) { TownAreas.load(context.applicationContext) }
}
