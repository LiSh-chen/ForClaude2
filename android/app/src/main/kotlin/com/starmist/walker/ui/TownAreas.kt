package com.starmist.walker.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import com.starmist.core.route.ContentPack
import com.starmist.core.route.RouteDef
import com.starmist.walker.content.ContentStore
import java.io.DataInputStream
import java.util.zip.GZIPInputStream

/** A township or district (鄉鎮市區): outline rings as (lat, lon) pairs and a point well inside it for its name. */
class TownArea(
    val name: String, val county: String, val areaKm2: Double, val medianElevM: Int, val highElevM: Int, val villages: Int,
    val lat: Double, val lon: Double, val rings: List<DoubleArray>,
) {
    private val minLat = rings.minOf { r -> (0 until r.size step 2).minOf { r[it] } }
    private val maxLat = rings.maxOf { r -> (0 until r.size step 2).maxOf { r[it] } }
    private val minLon = rings.minOf { r -> (1 until r.size step 2).minOf { r[it] } }
    private val maxLon = rings.maxOf { r -> (1 until r.size step 2).maxOf { r[it] } }

    val widthKm: Double get() = (maxLon - minLon) * 101.5

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

    private var fogExplored: Set<Int>? = null
    private var fogW = -1f
    private var fogPath: Path? = null

    /**
     * Everything on a map [w] by [h] except the [explored] townships (even-odd fill: the box plus the township
     * outlines), cached until the set or the size changes. Drawn as mist over the places not walked yet.
     */
    fun fog(explored: Set<Int>, w: Float, h: Float): Path {
        val cached = fogPath
        if (cached != null && fogW == w && fogExplored == explored) return cached
        val p = Path().apply {
            fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
            addRect(androidx.compose.ui.geometry.Rect(-w, -h, 2 * w, 2 * h))
            explored.forEach { addPath(path(it, w, h)) }
        }
        fogExplored = explored.toSet(); fogW = w; fogPath = p
        return p
    }

    /** The townships the walk has passed through so far, with the kilometres walked in each (sampled every half kilometre). */
    fun visited(route: RouteDef, walkedKm: Double): Map<Int, Double> {
        val seen = LinkedHashMap<Int, Double>()
        var km = 0.0
        while (km <= walkedKm + 1e-6) {
            val g = route.positionAt(km)
            val id = find(g.lat, g.lng)
            if (id >= 0) seen[id] = (seen[id] ?: 0.0) + 0.5
            km += 0.5
        }
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
                        val cb = ByteArray(s.readUnsignedByte()); s.readFully(cb)
                        val area = s.readUnsignedShort() / 10.0; val med = s.readShort().toInt(); val hi = s.readShort().toInt(); val vill = s.readUnsignedShort()
                        val lat = s.readInt() / 1e5; val lon = s.readInt() / 1e5
                        val rings = List(s.readUnsignedShort()) {
                            val c = s.readUnsignedShort()
                            var la = s.readInt(); var lo = s.readInt()
                            val out = DoubleArray(c * 2); out[0] = la / 1e5; out[1] = lo / 1e5
                            for (k in 1 until c) { la += s.readShort(); lo += s.readShort(); out[k * 2] = la / 1e5; out[k * 2 + 1] = lo / 1e5 }
                            out
                        }
                        TownArea(String(nameBytes, Charsets.UTF_8), String(cb, Charsets.UTF_8), area, med, hi, vill, lat, lon, rings)
                    }
                }
            }.getOrDefault(emptyList()).let { TownAreas(it) }
        }
    }
}

/** One of a township's three features: a short tag and the "did you know" that goes with it. */
class TownTag(val tag: String, val fact: String)

/** Three features per township, for the township card (res/raw/town_blurbs.txt: county|name|tag:fact|tag:fact|tag:fact). */
fun loadTownBlurbs(context: Context): Map<String, List<TownTag>> {
    return runCatching {
        ContentStore(context).lines(ContentPack.TOWNS)
            .filter { !it.startsWith("#") }
            .mapNotNull { l ->
                val f = l.split('|')
                if (f.size < 3) null
                else "${f[0]}|${f[1]}" to f.drop(2).take(3).mapNotNull { x -> x.split(':', limit = 2).takeIf { it.size == 2 }?.let { TownTag(it[0].trim(), it[1].trim()) } }
            }.filter { it.second.isNotEmpty() }.toMap()
    }.getOrDefault(emptyMap())
}

@Composable
fun rememberTownBlurbs(): Map<String, List<TownTag>> {
    val context = LocalContext.current
    return remember(context) { loadTownBlurbs(context.applicationContext) }
}

@Composable
fun rememberTownAreas(): TownAreas {
    val context = LocalContext.current
    return remember(context) { TownAreas.load(context.applicationContext) }
}

/** "Did you know" lines of the landmark cards: `id|fact|fact` per line in res/raw/landmark_facts.txt. */
fun loadLandmarkFacts(context: Context): Map<String, List<String>> {
    return runCatching {
        ContentStore(context).lines(ContentPack.FACTS)
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull { l ->
                val f = l.split('|')
                if (f.size < 2) null else f[0] to f.drop(1).map { it.trim() }.filter { it.isNotEmpty() }
            }.filter { it.second.isNotEmpty() }.toMap()
    }.getOrDefault(emptyMap())
}

@Composable
fun rememberLandmarkFacts(): Map<String, List<String>> {
    val context = LocalContext.current
    return remember(context) { loadLandmarkFacts(context.applicationContext) }
}
