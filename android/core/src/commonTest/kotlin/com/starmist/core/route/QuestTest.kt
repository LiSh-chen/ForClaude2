package com.starmist.core.route

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuestTest {
    private val combo = Quest("combo", "g", "t", "d", cards = setOf("a", "b", "c"))
    private val any3 = Quest("any3", "g", "t", "d", needCards = 3)
    private val mixed = Quest("mix", "g", "t", "d", cards = setOf("a"), towns = setOf("X|1", "X|2"))

    @Test fun comboCountsOnlyItsOwnCards() {
        assertEquals(QuestProgress(2, 3), QuestEngine.progress(combo, setOf("a", "b", "z"), emptySet()))
        assertTrue(QuestEngine.progress(combo, setOf("a", "b", "c"), emptySet()).done)
    }

    @Test fun anyQuestCountsEverything() {
        assertFalse(QuestEngine.progress(any3, setOf("a", "b"), emptySet()).done)
        assertTrue(QuestEngine.progress(any3, setOf("a", "b", "z"), emptySet()).done)
    }

    @Test fun mixedNeedsBoth() {
        assertFalse(QuestEngine.progress(mixed, setOf("a"), setOf("X|1")).done)
        assertTrue(QuestEngine.progress(mixed, setOf("a"), setOf("X|1", "X|2")).done)
    }

    @Test fun emptyQuestIsNeverDone() {
        assertFalse(QuestEngine.progress(Quest("e", "g", "t", "d"), setOf("a"), setOf("b")).done)
    }

    @Test fun newlyDoneSkipsAlreadyDoneAndSortsSmallestFirst() {
        val got = QuestEngine.newlyDone(listOf(combo, any3), setOf("any3"), setOf("a", "b", "c"), emptySet())
        assertEquals(listOf("combo"), got.map { it.id })
        val both = QuestEngine.newlyDone(listOf(combo, any3), emptySet(), setOf("a", "b", "c"), emptySet())
        assertEquals(listOf("any3", "combo"), both.map { it.id })
    }
}
