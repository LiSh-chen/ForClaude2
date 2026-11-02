package com.starmist.core.route

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContentPackTest {
    private val ids = TaiwanLoop.route.cards.map { it.first.id }.toSet()

    @Test
    fun `the bundled files obey their rules`() {
        ContentPack.FILES.forEach { name ->
            val known = if (name == ContentPack.POIS) TaiwanLoop.route.pois.map { it.id }.toSet() else ids
            val errors = ContentPack.validate(name, TestPack.lines(name), known)
            assertTrue(errors.isEmpty(), "$name:\n" + errors.joinToString("\n"))
        }
    }

    @Test
    fun `every card has at least one feature and no more than three`() {
        val lines = TestPack.lines(ContentPack.FACTS).filter { it.isNotBlank() && !it.startsWith("#") }
        val counts = lines.associate { it.split('|').let { f -> f[0] to f.size - 1 } }
        ids.forEach { id -> assertTrue((counts[id] ?: 0) in 1..3, "$id has ${counts[id] ?: 0} features") }
    }

    @Test
    fun `texts replace the card name on both directions`() {
        Routes.applyTexts(mapOf("keelung" to CardText("廟口")))
        try {
            Routes.all.forEach { r ->
                val c = r.cards.first { it.first.id == "keelung" }.first
                assertEquals("廟口", c.name)
            }
        } finally {
            TestPack.install()
        }
    }

    @Test
    fun `a bad card file is reported`() {
        val bad = listOf("keelung|基隆廟口", "nowhere|某地")
        val errors = ContentPack.validateCards(bad, ids)
        assertTrue(errors.any { "unknown card id nowhere" in it })
        assertTrue(errors.any { "missing card" in it })
    }

    @Test
    fun `a fact with a superlative is rejected`() {
        assertTrue(ContentPack.validateFacts(listOf("keelung|小吃:這是全台灣最有名的廟口小吃街區，每天都有很多人來排隊"), ids).isNotEmpty())
    }

    @Test
    fun `a township sentence may say first only with the checked mark`() {
        val plain = "臺北市|信義區|🏘️ 四四南村:四四南村是台北第一座眷村，位在聯勤四四兵工廠的南側，現在改為信義公民會館。"
        assertTrue(ContentPack.validateTowns(listOf(plain)).isNotEmpty())
        assertTrue(ContentPack.validateTowns(listOf(plain + "※")).isEmpty())
    }

    @Test
    fun `an editor note left in a township sentence is rejected`() {
        val line = "臺北市|信義區|🏘️ 四四南村:四四南村是台北眷村，隨兵工廠遷臺的員工與家屬在廠區南側住下，改為信義公民會館。（拿掉冠軍）"
        assertTrue(ContentPack.validateTowns(listOf(line)).any { "editor" in it })
    }
}
