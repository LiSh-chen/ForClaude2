package com.starmist.core.route

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContentPackTest {
    private val ids = TaiwanLoop.route.cards.map { it.first.id }.toSet()

    @Test
    fun `the bundled files obey their rules`() {
        ContentPack.FILES.forEach { name ->
            val errors = ContentPack.validate(name, TestPack.lines(name), ids)
            assertTrue(errors.isEmpty(), "$name:\n" + errors.joinToString("\n"))
        }
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
}
