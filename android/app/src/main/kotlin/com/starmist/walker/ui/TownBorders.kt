package com.starmist.walker.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import java.io.DataInputStream
import java.util.zip.GZIPInputStream

/** Township borders (res/raw/town_borders.bin, see tools/make_town_borders.py), as polylines of (lat, lon). */
class TownBorders internal constructor(private val lines: List<DoubleArray>) {
    private var cachedW = -1f
    private var cached: Path? = null

    /** All the borders as one path for a map drawn [w] by [h]; rebuilt only when the size changes. */
    fun path(w: Float, h: Float): Path {
        cached?.let { if (cachedW == w) return it }
        val p = Path()
        for (l in lines) {
            var i = 0
            while (i < l.size) {
                val x = ((l[i + 1] - MAP_LON_MIN) / (MAP_LON_MAX - MAP_LON_MIN) * w).toFloat()
                val y = ((MAP_LAT_MAX - l[i]) / (MAP_LAT_MAX - MAP_LAT_MIN) * h).toFloat()
                if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                i += 2
            }
        }
        cached = p; cachedW = w
        return p
    }

    companion object {
        fun load(context: Context): TownBorders {
            val id = context.resources.getIdentifier("town_borders", "raw", context.packageName)
            if (id == 0) return TownBorders(emptyList())
            return runCatching {
                DataInputStream(GZIPInputStream(context.resources.openRawResource(id)).buffered()).use { s ->
                    val n = s.readInt()
                    List(n) {
                        val c = s.readUnsignedShort()
                        var la = s.readInt(); var lo = s.readInt()
                        val out = DoubleArray(c * 2)
                        out[0] = la / 1e5; out[1] = lo / 1e5
                        for (k in 1 until c) { la += s.readShort(); lo += s.readShort(); out[k * 2] = la / 1e5; out[k * 2 + 1] = lo / 1e5 }
                        out
                    }
                }
            }.getOrDefault(emptyList()).let { TownBorders(it) }
        }
    }
}

@Composable
fun rememberTownBorders(): TownBorders {
    val context = LocalContext.current
    return remember(context) { TownBorders.load(context.applicationContext) }
}
