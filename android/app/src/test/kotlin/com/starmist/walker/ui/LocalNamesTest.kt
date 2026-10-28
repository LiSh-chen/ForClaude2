package com.starmist.walker.ui

import com.starmist.core.route.ContentPack
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every township key in the local-names file must be a real township, spelled as in the township data. */
class LocalNamesTest {
    private val areas = File("src/main/res/raw/town_areas.bin").inputStream().use { TownAreas.parse(it) }
    private val names = ContentPack.parseLocalNames(File("src/main/res/raw/local_names.txt").readLines())

    @Test
    fun theFileIsRead() {
        assertTrue("entries read: ${names.size}", names.size >= 10)
        assertEquals("Pang-liâu", names["fangliao"]?.roman)
    }

    @Test
    fun townshipKeysAreRealTownships() {
        val real = areas.towns.map { "${it.county}/${it.name}" }.toSet()
        for (k in names.keys.filter { '/' in it }) assertTrue("no township $k", k in real)
    }

    @Test
    fun theTownshipIntroductionsMentionTheOldName() {
        // an entry sits under the introduction that talks about that name
        val blurbs = File("src/main/res/raw/town_blurbs.txt").readLines().filter { !it.startsWith("#") }
        for ((k, n) in names.filter { '/' in it.key }) {
            val line = blurbs.firstOrNull { it.startsWith(k.replace('/', '|') + "|") }
            assertTrue("no blurb for $k", line != null)
            assertTrue("$k: the introduction does not mention ${n.han}", line!!.contains(n.han) || line.contains(n.han.dropLast(1)))
        }
    }
}
