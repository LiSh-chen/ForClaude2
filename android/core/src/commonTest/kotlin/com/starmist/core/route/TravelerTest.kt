package com.starmist.core.route

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TravelerTest {
    private val forward = Routes.byId(Travelers.FORWARD)!!
    private val reverse = Routes.byId(Travelers.REVERSE)!!

    @Test fun everyTravelerHasTwoMeetingsOnBothRoutesAndInTheFirstHalf() {
        for (route in listOf(forward, reverse)) {
            val ms = Travelers.meetingsOn(route)
            assertEquals(10, ms.size, route.id)
            for (t in Travelers.all) {
                val mine = ms.filter { it.first.id == t.id }
                assertEquals(listOf(1, 2), mine.map { it.second.stage }, "${route.id} ${t.id}")
                assertTrue(mine.last().third - mine.first().third >= 30.0, "${route.id} ${t.id} meetings too close")
                assertTrue(mine.last().third <= route.totalKm * 0.35, "${route.id} ${t.id} friends too late")
            }
        }
    }

    @Test fun meetingsComeInOrderAndMakeFriendsOnTheSecond() {
        val ev = Travelers.due(forward, 300.0, emptySet(), emptySet(), emptySet())
        assertEquals(10, ev.count { it is TravelerEvent.Met || it is TravelerEvent.BecameFriend })
        assertEquals(Travelers.meetingsOn(forward).first().first.id, ev.first().travelerId)
        assertTrue(ev.first() is TravelerEvent.Met)
    }

    @Test fun nothingIsRepeatedOnceRecorded() {
        val all = Travelers.all.map { it.id }.toSet()
        assertTrue(Travelers.due(forward, 300.0, all, all, emptySet()).none { it is TravelerEvent.Met || it is TravelerEvent.BecameFriend })
    }

    @Test fun friendsSendPostcardsEveryInterval() {
        val all = Travelers.all.map { it.id }.toSet()
        val due = Travelers.due(forward, forward.totalKm, all, all, emptySet()).filterIsInstance<TravelerEvent.MailDue>()
        // each friend can send at least three on a lap, and none beyond the end
        for (t in Travelers.all) assertTrue(due.count { it.travelerId == t.id } >= 3, t.id)
        assertTrue(due.all { it.atKm <= forward.totalKm })
        val done = due.map { it.key }.toSet()
        assertTrue(Travelers.due(forward, forward.totalKm, all, all, done).isEmpty())
        assertNotNull(Travelers.byId("momo"))
    }
}

class PostcardTownsTest {
    private val towns = listOf(
        TownProfile("A|高山", "A", "高山", 3000, 1500, 400.0, 5),
        TownProfile("A|平原", "A", "平原", 20, 5, 30.0, 12),
        TownProfile("A|丘陵", "A", "丘陵", 600, 150, 80.0, 9),
    )

    @Test fun eachTravelerLikesItsOwnKindOfPlace() {
        assertEquals("A|高山", PostcardTowns.choose("ahbo", towns, 0))
        assertEquals("A|平原", PostcardTowns.choose("xiaohe", towns, 0))
        assertEquals("A|丘陵", PostcardTowns.choose("atie", towns, 0))
        assertEquals("A|平原", PostcardTowns.choose("momo", towns, 0))
    }

    @Test fun noCandidatesGivesNothing() { assertEquals(null, PostcardTowns.choose("ahbo", emptyList(), 3)) }

    @Test fun everyTravelerWritesWithTheirNameAndOnlyFigures() {
        for (t in Travelers.all) {
            val l = PostcardTowns.lines(t, towns[0])
            assertTrue(l.last().endsWith(t.name))
            assertTrue(l.any { it.contains("A高山") })
        }
    }
}
