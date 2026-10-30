package com.starmist.core.route

import java.io.File

/** The content pack files as they are bundled in the app (the unit tests run from the core folder). */
object TestPack {
    private val raw = File("../app/src/main/res/raw")

    fun lines(name: String): List<String> = File(raw, name).readLines(Charsets.UTF_8)

    /** Puts the bundled card texts onto the routes, as the app does when it starts. */
    fun install() = Routes.applyTexts(ContentPack.parseCards(lines(ContentPack.CARDS)))
}
