package com.gomsoo.saigil.screen

import com.gomsoo.saigil.library.Flow
import com.gomsoo.saigil.library.Library
import com.gomsoo.saigil.library.Pair
import com.gomsoo.saigil.library.Person
import com.gomsoo.saigil.library.PersonId
import com.gomsoo.saigil.library.Relation
import com.gomsoo.saigil.library.Riders
import com.gomsoo.saigil.vehicle.Restriction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Drives the reducer the way the app does: each effect is applied before the next event. */
class ScreenTest {

    private val dad = Person(PersonId("dad"), "아빠")
    private val mom = Person(PersonId("mom"), "엄마", interests = listOf("요가"))
    private val friend = Person(PersonId("f"), "민호")

    private var model = Model(vehicle = Restriction.Parked)
    private var library = Library()
    private var me: PersonId? = null
    private val effects = mutableListOf<Effect>()
    private var tick = 0

    private fun send(vararg events: Event) = events.forEach { event ->
        tick++
        val next = reduce(model, event, library, me, Stamp(now = 1000L * tick, id = "id$tick"))
        model = next.model
        next.effects.forEach {
            if (it is Effect.Write) library = it.applyTo(library)
            if (it is Effect.BecomeMe) me = it.me
        }
        effects += next.effects
    }

    private inline fun <reified T : Screen> screen(): T = model.screen as T

    private fun startAs(person: Person, vararg people: Person) {
        library = library.copy(people = people.toList())
        send(Event.ChooseMe(person.id))
    }

    @Test
    fun `the app starts by asking who the user is`() {
        assertEquals(Screen.WhoAmI, Model().screen)
    }

    @Test
    fun `with no profiles the first one made becomes the user`() {
        send(Event.AddPerson, Event.Submit)
        assertEquals(setOf(Problem.NoName), screen<Screen.PersonEdit>().problems)

        send(Event.EditDraft(PersonDraft(name = "엄마", places = "요가원, 시장", interests = "요가")), Event.Submit)

        val saved = library.people.single()
        assertEquals(listOf("요가원", "시장"), saved.places)
        assertEquals(saved.id, me)
        assertEquals(Screen.Home, model.screen)
    }

    @Test
    fun `picking a profile makes it the user, and the user can change later`() {
        startAs(mom, dad, mom)
        assertEquals(mom.id, me)
        assertEquals(Screen.Home, model.screen)

        send(Event.ChangeMe, Event.ChooseMe(dad.id))

        assertEquals(dad.id, me)
    }

    @Test
    fun `a topic is saved for the user and the chosen companion, and asks their relation the first time`() {
        startAs(mom, dad, mom)
        send(Event.OpenPeople, Event.OpenSaveTopic)
        assertEquals(mom.id, screen<Screen.PickCompanion>().me)

        send(Event.ChooseCompanion(dad.id), Event.Submit)
        assertEquals(setOf(Problem.NoRelation), screen<Screen.PickCompanion>().problems)

        send(Event.PickRelation(Relation.Partner), Event.Submit, Event.Mic)
        assertEquals(Effect.StartListening, effects.last())
        send(Event.Speech(Heard.Partial("바다")))
        assertEquals(Stt.Listening("바다"), screen<Screen.TopicCapture>().capture.stt)
        send(Event.Speech(Heard.Final("바다 여행 가고 싶다는 얘기")), Event.EditText("바다 여행 가고 싶다던 얘기"), Event.Submit)

        val topic = library.topics.single()
        assertEquals("바다 여행 가고 싶다던 얘기", topic.text)
        assertEquals(Pair.of(mom.id, dad.id), topic.pair)
        assertEquals(Relation.Partner, library.relation(Riders.of(dad.id, mom.id)!!))
        assertEquals(Screen.People, model.screen)
    }

    @Test
    fun `the final text fills the field even when the recognizer reports its end first`() {
        startAs(mom, dad, mom)
        send(Event.OpenPeople, Event.OpenSaveTopic, Event.ChooseCompanion(dad.id), Event.PickRelation(Relation.Partner), Event.Submit, Event.Mic)
        send(Event.Speech(Heard.Partial("바다 여행")), Event.Speech(Heard.Ended))
        assertEquals(Capture(Stt.Idle, "바다 여행"), screen<Screen.TopicCapture>().capture)

        send(Event.Speech(Heard.Final("바다 여행 가는 얘기")))
        assertEquals(Capture(Stt.Idle, "바다 여행 가는 얘기"), screen<Screen.TopicCapture>().capture)
    }

    @Test
    fun `a pair that already has a relation is not asked again, but can change it`() {
        startAs(mom, dad, mom)
        library = library.relate(Riders.of(dad.id, mom.id)!!, Relation.Child)
        send(Event.OpenRecommend, Event.ChooseCompanion(dad.id), Event.Submit)
        assertEquals(Riders.of(mom.id, dad.id), screen<Screen.ChooseInput>().riders)
        assertFalse(effects.any { it is Effect.Relate })

        send(Event.Back, Event.ChangeRelation, Event.PickRelation(Relation.Partner), Event.Submit)

        assertEquals(Relation.Partner, library.relation(Riders.of(mom.id, dad.id)!!))
    }

    @Test
    fun `the user cannot be their own companion and must pick one`() {
        startAs(mom, dad, mom)
        send(Event.OpenRecommend, Event.ChooseCompanion(mom.id), Event.Submit)

        assertNull(screen<Screen.PickCompanion>().companion)
        assertEquals(setOf(Problem.NoCompanion), screen<Screen.PickCompanion>().problems)
    }

    @Test
    fun `when recognition fails the keyboard still saves the topic`() {
        startAs(mom, dad, mom)
        send(Event.OpenPeople, Event.OpenSaveTopic, Event.ChooseCompanion(dad.id), Event.PickRelation(Relation.Partner), Event.Submit)
        send(Event.Mic, Event.Speech(Heard.Failed(SttFailure.Unavailable)))
        assertEquals(Stt.Failed(SttFailure.Unavailable), screen<Screen.TopicCapture>().capture.stt)

        send(Event.EditText("주말 시장 구경"), Event.Submit)

        assertEquals("주말 시장 구경", library.topics.single().text)
    }

    @Test
    fun `a topic gets three openers and starts the ride`() {
        startAs(mom, dad, mom)
        send(Event.OpenRecommend, Event.ChooseCompanion(dad.id), Event.PickRelation(Relation.Partner), Event.Submit)
        send(Event.ChooseKind(InputKind.Topic), Event.Submit)
        assertEquals(setOf(Problem.NoText), screen<Screen.Listen>().problems)

        send(Event.EditText("요가"), Event.Submit)

        val openers = screen<Screen.Openers>()
        assertEquals(3, openers.cards.size)
        assertTrue(openers.cards.all { "요가" in it.first })
        assertTrue(Effect.BeginRide(Riders.of(mom.id, dad.id)!!) in effects)
    }

    @Test
    fun `a topic saved by one person is recommended when the other is the user, and choosing it marks it used`() {
        startAs(mom, dad, mom, friend)
        send(Event.OpenPeople, Event.OpenSaveTopic, Event.ChooseCompanion(dad.id), Event.PickRelation(Relation.Partner), Event.Submit)
        send(Event.EditText("바다 여행 가고 싶다던 얘기"), Event.Submit)
        val sea = library.topics.single()

        send(Event.Back, Event.ChangeMe, Event.ChooseMe(dad.id))
        send(Event.OpenRecommend, Event.ChooseCompanion(mom.id), Event.Submit, Event.ChooseKind(InputKind.Context))
        send(Event.EditText("주말에 바다 보러 갈까"), Event.Submit)
        assertEquals(listOf(sea.id), screen<Screen.SavedTopics>().picks.map { it.topic.id })

        send(Event.ChooseTopic(sea.id))
        assertEquals(sea.text, screen<Screen.Openers>().topic)
        assertTrue(library.topics.single().usedAt != null)

        send(Event.Done, Event.OpenRecommend, Event.ChooseCompanion(friend.id), Event.PickRelation(Relation.Friend), Event.Submit)
        send(Event.ChooseKind(InputKind.Context), Event.EditText("주말에 바다 보러 갈까"), Event.Submit)
        assertTrue(screen<Screen.SavedTopics>().picks.isEmpty())
    }

    @Test
    fun `a reflection records the ride and adds discovered interests to the companion`() {
        startAs(dad, dad, mom)
        library = library.relate(Riders.of(dad.id, mom.id)!!, Relation.Partner)
        send(Event.OpenReflect, Event.ChooseCompanion(mom.id), Event.Submit, Event.Submit)
        assertEquals(setOf(Problem.NoFlow), screen<Screen.Reflect>().problems)

        send(Event.PickFlow(Flow.Smooth), Event.EditText("캠핑, 트로트"), Event.Submit)

        assertEquals(listOf("캠핑", "트로트"), screen<Screen.Reflected>().discovered)
        send(Event.Done)
        assertEquals(Screen.Home, model.screen)
        assertEquals(listOf("요가", "캠핑", "트로트"), library.person(mom.id)!!.interests)
        assertEquals(Flow.Smooth, library.bond(Pair.of(dad.id, mom.id)!!).lastFlow)
    }

    @Test
    fun `driving stops the microphone and ignores the screen until parked`() {
        library = Library(people = listOf(dad, mom))
        model = model.copy(screen = Screen.TopicCapture(Riders.of(mom.id, dad.id)!!))
        send(Event.Mic, Event.VehicleChanged(Restriction.Restricted))

        assertEquals(Effect.StopListening, effects.last())
        assertEquals(Stt.Idle, screen<Screen.TopicCapture>().capture.stt)

        send(Event.EditText("주행 중 입력"), Event.Submit)
        assertTrue(library.topics.isEmpty())

        send(Event.VehicleChanged(Restriction.Parked), Event.EditText("주차 후 입력"), Event.Submit)
        assertEquals("주차 후 입력", library.topics.single().text)
    }

    @Test
    fun `leaving the app stops listening`() {
        model = model.copy(screen = Screen.TopicCapture(Riders.of(mom.id, dad.id)!!))
        send(Event.Mic, Event.PageHidden)

        assertEquals(Effect.StopListening, effects.last())
        assertEquals(Stt.Idle, screen<Screen.TopicCapture>().capture.stt)
    }
}
