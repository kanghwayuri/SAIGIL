package com.gomsoo.saigil.drive

import ai.pleos.playground.navi.constants.TrafficCongestionType
import ai.pleos.playground.navi.constants.TurnType
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.navi.data.TrafficCongestionInfo
import com.gomsoo.saigil.library.Library
import com.gomsoo.saigil.library.Person
import com.gomsoo.saigil.library.PersonId
import com.gomsoo.saigil.library.Riders
import com.gomsoo.saigil.library.Topic
import com.gomsoo.saigil.library.TopicId
import com.gomsoo.saigil.recommend.Situation
import com.gomsoo.saigil.recommend.Suggestion
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

    private fun drive(withRide: Boolean = true, library: Library = this.library): Drive {
        val rides = Rides()
        if (withRide) rides.begin(ride.riders)
        return Drive(rides) { library }
    }

    @Test
    fun `an interchange entry close ahead means a highway, with the distance left when known`() {
        assertEquals(Situation.Highway(42), highwayFrom(listOf(TBTInfo(TurnType.TURN_ENTER_IC_R, 300.0, "")), 42))
        assertEquals(Situation.Highway(null), highwayFrom(listOf(TBTInfo(TurnType.TURN_ENTER_IC_L, 300.0, "")), null))
        assertNull(highwayFrom(listOf(TBTInfo(TurnType.TURN_ENTER_IC_R, 2000.0, "")), 42))
        assertNull(highwayFrom(listOf(TBTInfo(TurnType.TURN_EXIT_IC_R, 100.0, "")), 42))
        assertNull(highwayFrom(emptyList(), 42))
    }

    @Test
    fun `slow or heavy traffic just ahead means a jam with its length and speed`() {
        assertEquals(Situation.Jam(1200, 12), jamFrom(listOf(congestion(100.0, TrafficCongestionType.HEAVY))))
        assertNull(jamFrom(listOf(congestion(100.0, TrafficCongestionType.MODERATE))))
        assertNull(jamFrom(listOf(congestion(5000.0, TrafficCongestionType.SLOW))))
    }

    @Test
    fun `without an active ride the board shows the road but nothing is said`() {
        val drive = drive(withRide = false)

        assertEquals(Decision.NoRide, drive.on(Situation.Jam(2300, 12), now = 0))
        assertEquals(DriveBoard(Situation.Jam(2300, 12), latest = null), drive.board.value)
    }

    @Test
    fun `a jam shows the situation and the balance game it speaks`() {
        val drive = drive()

        val spoken = drive.on(Situation.Jam(2300, 12), now = 0) as Decision.Speak

        assertTrue(spoken.suggestion is Suggestion.BalanceGame)
        assertTrue(spoken.line, spoken.line.startsWith("길이 막히네요.") && spoken.suggestion.sentence in spoken.line)
        assertEquals(DriveBoard(Situation.Jam(2300, 12), spoken.suggestion), drive.board.value)
    }

    @Test
    fun `a highway shows the saved topic it speaks`() {
        val pair = ride.riders.pair
        val drive = drive(library = library.copy(topics = listOf(Topic(TopicId("sea"), "바다 여행 얘기", pair, 0, null))))

        val spoken = drive.on(Situation.Highway(42), now = 0) as Decision.Speak

        assertEquals(Suggestion.SavedTopic(TopicId("sea"), "바다 여행 얘기"), spoken.suggestion)
        assertTrue(spoken.line, spoken.line.startsWith("한동안 길게 달려요.") && "바다 여행 얘기" in spoken.line)
        assertEquals(DriveBoard(Situation.Highway(42), spoken.suggestion), drive.board.value)
    }

    @Test
    fun `each kind of situation speaks at most once every ten minutes, never repeating`() {
        val drive = drive()
        val first = drive.on(Situation.Jam(2300, 12), now = 0) as Decision.Speak

        assertEquals(Decision.CoolingDown, drive.on(Situation.Jam(1000, 20), now = COOLDOWN_MS - 1))
        assertEquals("a quiet update still shows the new road", DriveBoard(Situation.Jam(1000, 20), first.suggestion), drive.board.value)
        assertTrue(drive.on(Situation.Highway(null), now = 1) is Decision.Speak)

        val later = drive.on(Situation.Jam(2300, 12), now = COOLDOWN_MS) as Decision.Speak
        assertNotEquals(first.suggestion.key, later.suggestion.key)
    }
}
