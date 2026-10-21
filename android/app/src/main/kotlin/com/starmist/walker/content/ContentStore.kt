package com.starmist.walker.content

import android.content.Context
import com.starmist.core.route.ContentPack
import com.starmist.core.route.Routes

/**
 * The text content (card lines, "did you know" facts, township features), read from the files bundled
 * in the app (res/raw). Changing text means a new app version; the files are checked by the unit
 * tests (see [ContentPack]) so a bad edit never reaches a build.
 */
class ContentStore(private val context: Context) {
    fun lines(name: String): List<String> {
        val id = context.resources.getIdentifier(name.substringBeforeLast('.'), "raw", context.packageName)
        if (id == 0) return emptyList()
        return runCatching { context.resources.openRawResource(id).bufferedReader(Charsets.UTF_8).readLines() }.getOrDefault(emptyList())
    }

    /** Puts the card names and lines onto the routes. */
    fun applyToRoutes() {
        Routes.applyTexts(ContentPack.parseCards(lines(ContentPack.CARDS)))
    }
}
