package com.gomsoo.saigil.library

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LibraryStoreTest {

    @get:Rule val folder = TemporaryFolder()

    private val dad = Person(PersonId("dad"), "아빠", places = listOf("회사"))
    private val mom = Person(PersonId("mom"), "엄마", interests = listOf("요가"))
    private val friend = Person(PersonId("f"), "민호")
    private val momWithDad = Riders.of(mom.id, dad.id)!!
    private val dadWithMom = Riders.of(dad.id, mom.id)!!

    @Test
    fun `a new store on the same file sees everything saved before`() = runBlocking {
        val file = folder.root.resolve("library.json")
        val topic = Topic(TopicId("t1"), "바다 여행 가고 싶다던 얘기", momWithDad.pair, addedAt = 10)
        LibraryStore(file).update {
            it.upsert(dad).upsert(mom).relate(momWithDad, Relation.Partner).add(topic)
                .record(Ride(mom.id, dad.id, at = 20, flow = Flow.Okay))
        }

        val reopened = LibraryStore(file).library.value

        assertEquals(listOf(dad, mom), reopened.people)
        assertEquals(listOf(topic), reopened.topics)
        assertEquals(Relation.Partner, reopened.relation(dadWithMom))
        assertEquals(1, reopened.rides.size)
    }

    @Test
    fun `a missing file opens as an empty library`() {
        assertEquals(Library(), LibraryStore(folder.root.resolve("library.json")).library.value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a saved pair of one person is unreadable, which the store moves aside`() {
        Json.decodeFromString(Library.serializer(), """{"topics":[{"id":"t","text":"x","pair":{"first":"a","second":"a"},"addedAt":1}]}""")
    }

    @Test
    fun `a pair is the same whichever person comes first, and one person is not a pair`() {
        assertEquals(Pair.of(mom.id, dad.id), Pair.of(dad.id, mom.id))
        assertNull(Pair.of(mom.id, mom.id))
        assertNull(Riders.of(mom.id, mom.id))
    }

    @Test
    fun `a parent is spoken to formally and the child back casually`() {
        val library = Library(people = listOf(dad, mom)).relate(Riders.of(dad.id, mom.id)!!, Relation.Parent)

        assertEquals(Relation.Parent, library.relation(dadWithMom))
        assertEquals(Register.Formal, library.register(dadWithMom))
        assertEquals(Relation.Child, library.relation(momWithDad))
        assertEquals(Register.Casual, library.register(momWithDad))
    }

    @Test
    fun `changing a relation replaces it on both sides, and a pair without one is casual`() {
        val library = Library().relate(dadWithMom, Relation.Parent).relate(momWithDad, Relation.Sibling)

        assertEquals(Relation.Sibling, library.relation(dadWithMom))
        assertEquals(Relation.Sibling, library.relation(momWithDad))
        assertEquals(2, library.ties.size)
        assertNull(library.relation(Riders.of(dad.id, friend.id)!!))
        assertEquals(Register.Casual, library.register(Riders.of(dad.id, friend.id)!!))
    }

    @Test
    fun `a ride adds what was discovered to the companion without duplicates`() {
        val library = Library(people = listOf(dad, mom))
            .record(Ride(dad.id, mom.id, at = 1, flow = Flow.Smooth, discovered = listOf("요가", "캠핑")))

        assertEquals(listOf("요가", "캠핑"), library.person(mom.id)!!.interests)
        assertEquals(emptyList<String>(), library.person(dad.id)!!.interests)
    }

    @Test
    fun `the bond of a pair counts rides whoever was the user and remembers the last flow`() {
        val library = Library(people = listOf(dad, mom, friend))
            .record(Ride(dad.id, mom.id, at = 1, flow = Flow.Smooth))
            .record(Ride(mom.id, dad.id, at = 2, flow = Flow.Awkward, discovered = listOf("캠핑")))
            .record(Ride(dad.id, friend.id, at = 3, flow = Flow.Okay))

        val bond = library.bond(dadWithMom.pair)

        assertEquals(2, bond.count)
        assertEquals(Flow.Awkward, bond.lastFlow)
        assertEquals(listOf("캠핑"), bond.discovered)
    }
}
