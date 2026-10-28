package com.starmist.walker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource

/**
 * Looks up a picture from the asset library (android/assets) by name, e.g. "paper_page".
 *
 * Pictures are exported to `drawable-nodpi/art_<name>.webp`. Anything that has not been drawn yet
 * simply returns null, and the caller falls back to its built-in drawing, so art can arrive one
 * piece at a time without breaking the build.
 */
@Composable
fun rememberArt(name: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(name) { ArtCache.get(context.applicationContext, name) }
}

/**
 * Pictures already decoded, kept for the whole app: the map alone is 28 MB once decoded, and decoding it again every time
 * the Today tab came back was the pause when switching tabs. [preload] fills it in the background when the app starts.
 */
internal object ArtCache {
    private val cache = object : android.util.LruCache<String, ImageBitmap>(96 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }
    private val missing = HashSet<String>()

    fun get(context: android.content.Context, name: String): ImageBitmap? {
        cache.get(name)?.let { return it }
        synchronized(missing) { if (name in missing) return null }
        val id = context.resources.getIdentifier("art_$name", "drawable", context.packageName)
        val bitmap = if (id == 0) null else runCatching { ImageBitmap.imageResource(context.resources, id) }.getOrNull()
        if (bitmap == null) synchronized(missing) { missing += name } else cache.put(name, bitmap)
        return bitmap
    }

    /** Decodes what the first screens need, off the main thread. */
    suspend fun preload(context: android.content.Context) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        listOf("paper_page", "parchment_tile", "real_map_base", "traveller_body", "traveller_staff", "traveller_hand", "collection_slot_frame", "collection_slot_locked") .forEach { get(context, it) }
        (0 until 6).forEach { get(context, "traveller_walk_$it") }
    }
}

/** A card's picture at a quarter of its size, for the collection list: 45 full-size pictures decoded on the main thread made the tab stall. */
@Composable
fun rememberCardThumb(cardId: String): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    if (androidx.compose.ui.platform.LocalInspectionMode.current) return remember(cardId) { decodeThumb(context, cardId) }
    val state = produceState(CardThumbs.get(cardId), cardId) {
        if (value == null) value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { decodeThumb(context, cardId) }
    }
    return state.value
}

private object CardThumbs {
    private val cache = object : android.util.LruCache<String, ImageBitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }
    fun get(id: String): ImageBitmap? = cache.get(id)
    fun put(id: String, bitmap: ImageBitmap) { cache.put(id, bitmap) }
}

private fun decodeThumb(context: android.content.Context, cardId: String): ImageBitmap? {
    CardThumbs.get(cardId)?.let { return it }
    val res = context.resources
    val id = res.getIdentifier("art_card_$cardId", "drawable", context.packageName)
    if (id == 0) return null
    val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 }
    val bitmap = runCatching { android.graphics.BitmapFactory.decodeResource(res, id, options)?.asImageBitmap() }.getOrNull() ?: return null
    CardThumbs.put(cardId, bitmap)
    return bitmap
}
