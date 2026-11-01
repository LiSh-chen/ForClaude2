package com.starmist.walker.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every piece of Chinese writing in the app's code has its English (see [I18n]), and texts with numbers and names are put into English right. */
class I18nTest {
    /** The text strings in a Kotlin source, without those that are in comments and those with a template ($) in them. */
    private fun literals(src: String): List<String> {
        val out = ArrayList<String>()
        var i = 0
        val n = src.length
        fun skipString(from: Int): Int {          // from: the index after an opening quote; returns the index after the closing quote
            var k = from
            while (k < n && src[k] != '"') {
                if (src[k] == '\\') k++
                else if (src[k] == '$' && k + 1 < n && src[k + 1] == '{') {
                    var depth = 1; k += 2
                    while (k < n && depth > 0) {
                        when (src[k]) { '{' -> depth++; '}' -> depth--; '"' -> { k = skipString(k + 1); continue } }
                        k++
                    }
                    continue
                }
                k++
            }
            return k + 1
        }
        while (i < n) {
            val c = src[i]
            when {
                c == '/' && i + 1 < n && src[i + 1] == '/' -> { while (i < n && src[i] != '\n') i++ }
                c == '/' && i + 1 < n && src[i + 1] == '*' -> { val e = src.indexOf("*/", i + 2); i = if (e < 0) n else e + 2 }
                c == '"' && src.startsWith("\"\"\"", i) -> { val e = src.indexOf("\"\"\"", i + 3); i = if (e < 0) n else e + 3 }
                c == '\'' -> { i += if (i + 1 < n && src[i + 1] == '\\') 4 else 3 }
                c == '"' -> {
                    val end = skipString(i + 1)
                    val text = src.substring(i + 1, end - 1)
                    if (!text.contains('$') && text.any { it in '㐀'..'鿿' }) out += text.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\")
                    i = end
                }
                else -> i++
            }
        }
        return out
    }

    @Test fun every_chinese_text_in_the_code_has_its_english() {
        // not texts for the user: the ones drawn as marks on the walker, the text of the content files' tags
        val skip = setOf("呼", "景")
        val files = File("src/main/kotlin").walkTopDown().filter { it.extension == "kt" && it.name != "StringsEn.kt" && it.name != "TownAreas.kt" }.toList()
        assertTrue("source files not found", files.isNotEmpty())
        val missing = files.flatMap { f -> literals(f.readText()).filter { it !in EN && it !in skip }.map { "${f.name}: $it" } }
        assertEquals("texts with no English:\n" + missing.joinToString("\n"), 0, missing.size)
    }

    @Test fun numbers_are_put_back_into_the_english() {
        assertEquals("Today: 4,321 steps", I18n.tr("今日 4,321 步", Lang.EN))
        assertEquals("Walked 85.0 / 1,045 km", I18n.tr("達成 85.0 / 1,045 km", Lang.EN))
        assertEquals("Set 2026-11-01 to 3000 steps", I18n.tr("已修正 2026-11-01 為 3000 步", Lang.EN))
        assertEquals("About 12.3 of 1045 km walked", I18n.tr("已走約 12.3 / 1045 公里", Lang.EN))
    }

    @Test fun names_are_put_into_the_templates() {
        assertEquals("Sight card: 台北101", I18n.trf("獲得景點卡：{0}", "台北101", lang = Lang.EN))
        assertEquals("16.0 km to go", I18n.trf("還有 {0} 公里", "16.0", lang = Lang.EN))
        assertEquals("獲得景點卡：台北101", I18n.trf("獲得景點卡：{0}", "台北101", lang = Lang.ZH))
    }

    @Test fun a_text_with_no_english_stays_as_it_is_and_chinese_is_never_changed() {
        assertEquals("沒有翻譯的字", I18n.tr("沒有翻譯的字", Lang.EN))
        assertEquals("今日", I18n.tr("今日", Lang.ZH))
    }

    @Test fun the_language_follows_the_phone_unless_picked() {
        assertEquals(Lang.ZH, langOf(0, "zh"))
        assertEquals(Lang.EN, langOf(0, "ja"))
        assertEquals(Lang.EN, langOf(2, "zh"))
        assertEquals(Lang.ZH, langOf(1, "en"))
    }
}
