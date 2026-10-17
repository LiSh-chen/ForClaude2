package com.starmist.walker.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** The detailed island picture is cut into [TILE] pixel squares of a [FULL_W] by [FULL_H] picture (art_maptile_<row>_<col>). */
const val MAP_TILE = 1024
const val MAP_FULL_W = 5563
const val MAP_FULL_H = 9969
const val MAP_TILE_COLS = 6
const val MAP_TILE_ROWS = 10
private const val KEEP = 18

/**
 * Loads the detailed tiles on demand and keeps a few in memory. In previews (no background threads wanted)
 * the tiles are decoded on the spot so a screenshot shows them.
 */
@Stable
class MapTiles internal constructor(private val context: Context, private val inline: Boolean) {
    private val loaded = mutableStateMapOf<Int, ImageBitmap>()
    private val pending = HashSet<Int>()
    private val order = ArrayList<Int>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun decode(id: Int): ImageBitmap? {
        val res = context.resources.getIdentifier("art_maptile_${id / MAP_TILE_COLS}_${id % MAP_TILE_COLS}", "drawable", context.packageName)
        if (res == 0) return null
        return runCatching { ImageBitmap.imageResource(context.resources, res) }.getOrNull()
    }

    /** The tile if it is in memory; otherwise asks for it and returns null. */
    fun get(id: Int): ImageBitmap? {
        loaded[id]?.let { touch(id); return it }
        if (inline) {
            val b = decode(id) ?: return null
            loaded[id] = b; return b
        }
        synchronized(pending) { if (!pending.add(id)) return null }
        scope.launch {
            val b = decode(id)
            synchronized(pending) { pending.remove(id) }
            if (b != null) {
                loaded[id] = b
                synchronized(order) { order.remove(id); order.add(id); while (order.size > KEEP) loaded.remove(order.removeAt(0)) }
            }
        }
        return null
    }

    private fun touch(id: Int) = synchronized(order) { if (order.lastOrNull() != id) { order.remove(id); order.add(id) } }

    /** Ids of the tiles that overlap [view] (given in pixels of a map drawn [w] by [h]). */
    fun visible(view: Rect, w: Float, h: Float): List<Int> {
        val sx = MAP_FULL_W / w; val sy = MAP_FULL_H / h
        val c0 = (view.left * sx / MAP_TILE).toInt().coerceIn(0, MAP_TILE_COLS - 1); val c1 = (view.right * sx / MAP_TILE).toInt().coerceIn(0, MAP_TILE_COLS - 1)
        val r0 = (view.top * sy / MAP_TILE).toInt().coerceIn(0, MAP_TILE_ROWS - 1); val r1 = (view.bottom * sy / MAP_TILE).toInt().coerceIn(0, MAP_TILE_ROWS - 1)
        return buildList { for (r in r0..r1) for (c in c0..c1) add(r * MAP_TILE_COLS + c) }
    }
}

@Composable
fun rememberMapTiles(): MapTiles {
    val context = LocalContext.current
    val inline = LocalInspectionMode.current
    return remember(context, inline) { MapTiles(context.applicationContext, inline) }
}
