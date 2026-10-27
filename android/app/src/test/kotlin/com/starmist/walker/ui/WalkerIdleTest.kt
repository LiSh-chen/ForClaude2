package com.starmist.walker.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The little standing-still acts must start and end exactly in the standing pose, or the traveller would jump when one begins or ends. */
class WalkerIdleTest {
    @Test
    fun everyActStartsAndEndsInTheStandingPose() {
        for (act in 1..Idle.COUNT) for (t in listOf(0f, 1f)) {
            val p = idlePose(act, t)
            assertEquals("act $act at $t sx", 1f, p.sx, 1e-3f)
            assertEquals("act $act at $t sy", 1f, p.sy, 1e-3f)
            assertEquals("act $act at $t rot", 0f, p.rot, 1e-2f)
            assertEquals("act $act at $t lift", 0f, p.lift, 1e-3f)
            assertEquals("act $act at $t face", 1f, p.face, 1e-3f)
        }
    }

    @Test
    fun actsStayWithinSmallBounds() {
        for (act in 1..Idle.COUNT) {
            var t = 0f
            while (t <= 1f) {
                val p = idlePose(act, t)
                assertTrue("act $act sy ${p.sy}", p.sy in 0.9f..1.15f)
                assertTrue("act $act sx ${p.sx}", p.sx in 0.9f..1.1f)
                assertTrue("act $act rot ${p.rot}", kotlin.math.abs(p.rot) < 12f)
                assertTrue("act $act lift ${p.lift}", p.lift in -1e-4f..0.12f)
                assertTrue("act $act face ${p.face}", p.face in -1f..1f)
                t += 0.01f
            }
        }
    }

    @Test
    fun lookingRoundTurnsAllTheWayAndBack() {
        assertEquals(-1f, idlePose(Idle.LOOK, 0.4f).face, 1e-3f)
        assertEquals(1f, idlePose(Idle.LOOK, 0.9f).face, 1e-3f)
    }
}
