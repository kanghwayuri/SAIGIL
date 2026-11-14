package com.gomsoo.saigil.screen

import com.gomsoo.saigil.library.Flow
import com.gomsoo.saigil.library.Library
import com.gomsoo.saigil.library.Person
import com.gomsoo.saigil.library.PersonId
import com.gomsoo.saigil.library.Relation
import com.gomsoo.saigil.library.Riders
import com.gomsoo.saigil.library.Topic
import com.gomsoo.saigil.library.TopicId
import com.gomsoo.saigil.vehicle.Restriction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Drives the reducer the way the app does: each library-changing effect is applied before the next event. */
class ScreenTest {

    private val me = Person(PersonId("me"), "지수", Relation.Me)
    private val mom = Person(PersonId("mom"), "엄마", Relation.Parent)

    private var model = Model(vehicle = Restriction.Parked)
    private var library = Library()
    private val effects = mutableListOf<Effect>()
    private var tick = 0

    private fun send(vararg events: Event) = events.forEach { event ->
        tick++
        val next = reduce(model, event, library, Stamp(now = 1000L * tick, id = "id$tick"))
        model = next.model
        next.effects.forEach { if (it is Effect.Write) library = it.applyTo(library) }
        effects += next.effects
    }

    private inline fun <reified T : Screen> screen(): T = model.screen as T

    @Test
    fun `a new profile is saved and leads straight to capturing a topic about that person`() {
        send(Event.OpenPeople, Event.AddPerson, Event.Submit)
        assertEquals(setOf(Problem.NoName, Problem.NoRelation), screen<Screen.PersonEdit>().problems)

        send(Event.EditDraft(PersonDraft(name = "엄마", relation = Relation.Parent, places = "요가원, 시장", interests = "요가")))
        send(Event.Submit)

        val saved = library.people.single()
        assertEquals("엄마", saved.name)
        assertEquals(listOf("요가원", "시장"), saved.places)
        assertEquals(saved.id, screen<Screen.TopicCapture>().person)
    }

    @Test
    fun `a spoken topic can be corrected before it is saved`() {
        library = Library(people = listOf(mom))
        send(Event.OpenPeople, Event.AddTopicFor(mom.id), Event.Mic)
        assertEquals(Effect.StartListening, effects.last())

        send(Event.Speech(Heard.Partial("바다")))
        assertEquals(Stt.Listening("바다"), screen<Screen.TopicCapture>().capture.stt)
        send(Event.Speech(Heard.Final("바다 여행 가고 싶다는 얘기")), Event.EditText("바다 여행 가고 싶다던 얘기"), Event.Submit)

        val topic = library.topics.single()
        assertEquals("바다 여행 가고 싶다던 얘기", topic.text)
        assertEquals(setOf(mom.id), topic.about)
        assertEquals(Screen.People, model.screen)
    }

    @Test
    fun `when recognition fails the keyboard still saves the topic`() {
        library = Library(people = listOf(mom))
        send(Event.OpenPeople, Event.AddTopicFor(mom.id), Event.Mic, Event.Speech(Heard.Failed(SttFailure.Unavailable)))
        assertEquals(Stt.Failed(SttFailure.Unavailable), screen<Screen.TopicCapture>().capture.stt)

        send(Event.EditText("주말 시장 구경"), Event.Submit)

        assertEquals("주말 시장 구경", library.topics.single().text)
    }

    @Test
    fun `a topic gets three openers and starts the ride`() {
        library = Library(people = listOf(me, mom))
        send(Event.OpenRecommend)
        assertEquals(me.id, screen<Screen.PickPair>().driver)

        send(Event.PickPassenger(mom.id), Event.Submit, Event.ChooseKind(InputKind.Topic), Event.Submit)
        assertEquals(setOf(Problem.NoText), screen<Screen.Listen>().problems)

        send(Event.EditText("요가"), Event.Submit)

        val openers = screen<Screen.Openers>()
        assertEquals(3, openers.cards.size)
        assertTrue(openers.cards.all { "요가" in it.first })
        assertTrue(Effect.BeginRide(Riders(me.id, mom.id)) in effects)
    }

    @Test
    fun `a context picks from saved topics and choosing one marks it used`() {
        val sea = Topic(TopicId("sea"), "바다 여행 가고 싶다던 얘기", setOf(mom.id), addedAt = 0)
        val shoes = Topic(TopicId("shoes"), "새로 산 운동화", addedAt = 0)
        library = Library(people = listOf(me, mom), topics = listOf(shoes, sea))
        send(Event.OpenRecommend, Event.PickPassenger(mom.id), Event.Submit, Event.ChooseKind(InputKind.Context))
        send(Event.EditText("주말에 바다 보러 갈까"), Event.Submit)

        val picks = screen<Screen.SavedTopics>().picks
        assertEquals(sea.id, picks.first().topic.id)

        send(Event.ChooseTopic(sea.id))

        assertEquals(sea.text, screen<Screen.Openers>().topic)
        assertTrue(library.topics.first { it.id == sea.id }.usedAt != null)
    }

    @Test
    fun `the same person cannot drive and ride`() {
        library = Library(people = listOf(me, mom))
        send(Event.OpenRecommend, Event.PickPassenger(me.id), Event.Submit)

        assertEquals(setOf(Problem.SamePerson), screen<Screen.PickPair>().problems)
    }

    @Test
    fun `a reflection records the ride and adds discovered interests to the passenger`() {
        library = Library(people = listOf(me, mom))
        send(Event.OpenReflect, Event.PickPassenger(mom.id), Event.Submit, Event.Submit)
        assertEquals(setOf(Problem.NoFlow), screen<Screen.Reflect>().problems)

        send(Event.PickFlow(Flow.Smooth), Event.EditText("캠핑, 트로트"), Event.Submit)

        assertEquals(Screen.Home, model.screen)
        assertEquals(listOf("캠핑", "트로트"), library.person(mom.id)!!.interests)
        assertEquals(Flow.Smooth, library.bond(Riders(me.id, mom.id)).lastFlow)
    }

    @Test
    fun `driving stops the microphone and ignores the screen until parked`() {
        library = Library(people = listOf(mom))
        send(Event.OpenPeople, Event.AddTopicFor(mom.id), Event.Mic)
        send(Event.VehicleChanged(Restriction.Restricted))

        assertEquals(Effect.StopListening, effects.last())
        assertEquals(Stt.Idle, screen<Screen.TopicCapture>().capture.stt)

        send(Event.EditText("주행 중 입력"), Event.Submit)
        assertTrue(library.topics.isEmpty())

        send(Event.VehicleChanged(Restriction.Parked), Event.EditText("주차 후 입력"), Event.Submit)
        assertEquals("주차 후 입력", library.topics.single().text)
    }

    @Test
    fun `leaving the app stops listening`() {
        library = Library(people = listOf(mom))
        send(Event.OpenPeople, Event.AddTopicFor(mom.id), Event.Mic, Event.PageHidden)

        assertEquals(Effect.StopListening, effects.last())
        assertEquals(Stt.Idle, screen<Screen.TopicCapture>().capture.stt)
    }
}
