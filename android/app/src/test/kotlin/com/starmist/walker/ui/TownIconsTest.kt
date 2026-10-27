package com.starmist.walker.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Every township feature has a small emoji picture in front of its tag, and the app splits it off the name. */
class TownIconsTest {
    @Test
    fun everyTownFeatureHasAnIcon() {
        val lines = File("src/main/res/raw/town_blurbs.txt").readLines().filter { it.isNotBlank() && !it.startsWith("#") }
        assertTrue(lines.size > 100)
        for (l in lines) for (x in l.split('|').drop(2)) {
            val (tag, fact) = x.split(':', limit = 2)
            val t = parseTag(tag, fact)
            assertTrue("no icon: ${l.split('|')[1]} $tag", t.icon.isNotEmpty())
            assertTrue("name left empty: $tag", t.tag.isNotBlank())
            assertTrue("icon is not a symbol: ${t.icon}", t.icon.codePoints().noneMatch { Character.isLetterOrDigit(it) })
        }
    }

    @Test
    fun tagWithoutAnIconIsJustTheName() {
        val t = parseTag("包種茶鄉", "x")
        assertEquals("", t.icon); assertEquals("包種茶鄉", t.tag)
        val u = parseTag("🍵 包種茶鄉", "x")
        assertEquals("🍵", u.icon); assertEquals("包種茶鄉", u.tag)
    }
}
