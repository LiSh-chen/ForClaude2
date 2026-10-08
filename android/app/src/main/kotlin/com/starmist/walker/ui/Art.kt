package com.starmist.walker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource

/**
 * Looks up a picture from the asset library (android/assets) by name, e.g. "fog_puff".
 *
 * Pictures are exported to `drawable-nodpi/art_<name>.webp`. Anything that has not been drawn yet
 * simply returns null, and the caller falls back to its built-in drawing, so art can arrive one
 * piece at a time without breaking the build.
 */
@Composable
fun rememberArt(name: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(name) {
        val id = context.resources.getIdentifier("art_$name", "drawable", context.packageName)
        if (id == 0) null else runCatching { ImageBitmap.imageResource(context.resources, id) }.getOrNull()
    }
}
