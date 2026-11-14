package com.gomsoo.saigil.library

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LibraryStoreTest {

    @get:Rule val folder = TemporaryFolder()

    private val me = Person(PersonId("me"), "지수", Relation.Me, places = listOf("회사"))
    private val mom = Person(PersonId("mom"), "엄마", Relation.Parent, interests = listOf("요가"))

    @Test
    fun `a new store on the same file sees everything saved before`() = runBlocking {
        val file = folder.root.resolve("library.json")
        val topic = Topic(TopicId("t1"), "바다 여행 가고 싶다던 얘기", about = setOf(mom.id), addedAt = 10)
        LibraryStore(file).update {
            it.upsert(me).upsert(mom).add(topic).record(Ride(me.id, mom.id, at = 20, flow = Flow.Okay))
        }

        val reopened = LibraryStore(file).library.value

        assertEquals(listOf(me, mom.copy(interests = listOf("요가"))), reopened.people)
        assertEquals(listOf(topic), reopened.topics)
        assertEquals(1, reopened.rides.size)
    }

    @Test
    fun `a missing file opens as an empty library`() {
        assertEquals(Library(), LibraryStore(folder.root.resolve("library.json")).library.value)
    }

    @Test
    fun `a ride adds what was discovered to the passenger without duplicates`() {
        val library = Library(people = listOf(me, mom))
            .record(Ride(me.id, mom.id, at = 1, flow = Flow.Smooth, discovered = listOf("요가", "캠핑")))

        assertEquals(listOf("요가", "캠핑"), library.person(mom.id)!!.interests)
        assertEquals(emptyList<String>(), library.person(me.id)!!.interests)
    }

    @Test
    fun `the bond of a pair counts rides in either seat and remembers the last flow`() {
        val friend = Person(PersonId("f"), "민호", Relation.Friend)
        val library = Library(people = listOf(me, mom, friend))
            .record(Ride(me.id, mom.id, at = 1, flow = Flow.Smooth))
            .record(Ride(mom.id, me.id, at = 2, flow = Flow.Awkward, discovered = listOf("캠핑")))
            .record(Ride(me.id, friend.id, at = 3, flow = Flow.Okay))

        val bond = library.bond(Riders(me.id, mom.id))

        assertEquals(2, bond.count)
        assertEquals(Flow.Awkward, bond.lastFlow)
        assertEquals(listOf("캠핑"), bond.discovered)
    }

    @Test
    fun `only parents are spoken to formally`() {
        assertEquals(Register.Formal, Relation.Parent.register)
        Relation.entries.filter { it != Relation.Parent }.forEach { assertEquals(Register.Casual, it.register) }
    }
}
