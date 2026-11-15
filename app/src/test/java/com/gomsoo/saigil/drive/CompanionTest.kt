package com.gomsoo.saigil.drive

import ai.pleos.playground.navi.constants.TrafficCongestionType
import ai.pleos.playground.navi.constants.TurnType
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.navi.data.TrafficCongestionInfo
import com.gomsoo.saigil.library.Library
import com.gomsoo.saigil.library.Person
import com.gomsoo.saigil.library.PersonId
import com.gomsoo.saigil.library.Riders
import com.gomsoo.saigil.recommend.Trigger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionTest {

    private val me = Person(PersonId("me"), "지수")
    private val friend = Person(PersonId("f"), "민호")
    private val library = Library(people = listOf(me, friend))
    private val ride = ActiveRide(Riders.of(me.id, friend.id)!!)

    private fun congestion(offset: Double, type: TrafficCongestionType) =
        TrafficCongestionInfo(offset, 1200.0, 300, type, 12.0, "경부고속도로", "서초IC", "양재IC")

    @Test
    fun `an interchange entry close ahead means a long drive`() {
        assertEquals(Trigger.LongDrive, longDriveFrom(listOf(TBTInfo(TurnType.TURN_ENTER_IC_R, 300.0, ""))))
        assertNull(longDriveFrom(listOf(TBTInfo(TurnType.TURN_ENTER_IC_R, 2000.0, ""))))
        assertNull(longDriveFrom(listOf(TBTInfo(TurnType.TURN_EXIT_IC_R, 100.0, ""))))
        assertNull(longDriveFrom(emptyList()))
    }

    @Test
    fun `slow or heavy traffic just ahead means a jam`() {
        assertEquals(Trigger.Jam, jamFrom(listOf(congestion(100.0, TrafficCongestionType.HEAVY))))
        assertNull(jamFrom(listOf(congestion(100.0, TrafficCongestionType.MODERATE))))
        assertNull(jamFrom(listOf(congestion(5000.0, TrafficCongestionType.SLOW))))
    }

    @Test
    fun `nothing is said without an active ride`() {
        assertEquals(Decision.NoRide, decide(Trigger.Jam, null, library, emptyMap(), now = 0))
    }

    @Test
    fun `each kind of trigger speaks at most once every ten minutes`() {
        val first = decide(Trigger.Jam, ride, library, emptyMap(), now = 0)
        assertTrue(first is Decision.Speak)
        val said = ride.copy(said = setOf((first as Decision.Speak).suggestion.key))

        assertEquals(Decision.CoolingDown, decide(Trigger.Jam, said, library, mapOf(Trigger.Jam to 0L), now = COOLDOWN_MS - 1))
        assertTrue(decide(Trigger.LongDrive, said, library, mapOf(Trigger.Jam to 0L), now = 1) is Decision.Speak)

        val later = decide(Trigger.Jam, said, library, mapOf(Trigger.Jam to 0L), now = COOLDOWN_MS)
        assertNotEquals(first.suggestion.key, (later as Decision.Speak).suggestion.key)
    }
}
