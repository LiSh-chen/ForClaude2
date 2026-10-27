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

    @Test
    fun drawnFramesStartAndEndInTheStandingPicture() {
        for (act in 1..Idle.COUNT) {
            val keys = Idle.KEYS[act]
            assertEquals("act $act has six frames", 6, keys.size)
            assertEquals(0f, keys.first(), 0f); assertEquals(1f, keys.last(), 0f)
            for (i in 1 until keys.size) assertTrue("act $act keys rise", keys[i] > keys[i - 1])
            val first = idleFrame(act, 0f); val last = idleFrame(act, 1f)
            assertEquals(0, first.a); assertEquals(0f, first.mix, 1e-4f)
            assertEquals(5, last.b); assertEquals(1f, last.mix, 1e-4f)
        }
    }

    @Test
    fun drawnFramesChangeWithoutJumps() {
        for (act in 1..Idle.COUNT) {
            // what is on screen is frame a plus mix of b over it: the weight of each frame index must move smoothly with time
            fun weights(t: Float): FloatArray {
                val f = idleFrame(act, t)
                return FloatArray(6).also { it[f.a] += 1f - f.mix; it[f.b] += f.mix }
            }
            var previous = weights(0f)
            var t = 0.002f
            while (t <= 1f) {
                val w = weights(t)
                for (i in 0 until 6) assertTrue("act $act at $t frame $i", kotlin.math.abs(w[i] - previous[i]) < 0.08f)
                previous = w
                t += 0.002f
            }
        }
    }

    @Test
    fun everyActHasItsDrawings() {
        val res = java.io.File("src/main/res/drawable-nodpi")
        for (act in 1..Idle.COUNT) for (i in 0 until 6) assertTrue("art_idle_${Idle.NAMES[act]}_$i", java.io.File(res, "art_idle_${Idle.NAMES[act]}_$i.webp").exists())
    }
}
