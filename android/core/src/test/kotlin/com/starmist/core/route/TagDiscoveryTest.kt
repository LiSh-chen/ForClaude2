package com.starmist.core.route

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TagDiscoveryTest {
    @Test fun `entering a township gives its first feature`() = assertEquals(0, TagDiscovery.next(0, 0, 3, 1))

    @Test fun `entering again on the same lap gives nothing`() = assertNull(TagDiscovery.next(0b1, 1, 3, 1))

    @Test fun `halfway through a long township gives the second feature`() = assertEquals(1, TagDiscovery.next(0b1, 1, 3, 2))

    @Test fun `a lap gives at most two features`() = assertNull(TagDiscovery.next(0b11, 2, 3, 2))

    @Test fun `the third feature comes on the next lap`() = assertEquals(2, TagDiscovery.next(0b11, 0, 3, 1))

    @Test fun `nothing is left once every feature is found`() = assertNull(TagDiscovery.next(0b111, 0, 3, 1))

    @Test fun `a township with one feature gives only that one`() {
        assertEquals(0, TagDiscovery.next(0, 0, 1, 1))
        assertNull(TagDiscovery.next(0b1, 1, 1, 2))
    }

    @Test fun `only long townships reach the second stage, at half the way`() {
        assertFalse(TagDiscovery.reachedHalfway(5.0, 4.0))
        assertFalse(TagDiscovery.reachedHalfway(12.0, 5.5))
        assertTrue(TagDiscovery.reachedHalfway(12.0, 6.0))
    }
}
