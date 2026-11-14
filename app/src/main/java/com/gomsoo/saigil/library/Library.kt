package com.gomsoo.saigil.library

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class PersonId(val value: String)

@Serializable
@JvmInline
value class TopicId(val value: String)

/** How a person relates to the app's user. */
enum class Relation { Me, Parent, Child, Partner, Friend, Other }

/** How the user speaks to someone: 존댓말 or 반말. */
enum class Register { Formal, Casual }

val Relation.register: Register get() = if (this == Relation.Parent) Register.Formal else Register.Casual

@Serializable
data class Person(
    val id: PersonId,
    val name: String,
    val relation: Relation,
    val places: List<String> = emptyList(),
    val interests: List<String> = emptyList(),
)

@Serializable
data class Topic(
    val id: TopicId,
    val text: String,
    /** People this topic is about. Empty means it suits anyone. */
    val about: Set<PersonId> = emptySet(),
    val addedAt: Long,
    val usedAt: Long? = null,
)

enum class Flow { Smooth, Okay, Awkward }

@Serializable
data class Ride(
    val driver: PersonId,
    val passenger: PersonId,
    val at: Long,
    val flow: Flow,
    val discovered: List<String> = emptyList(),
)

data class Riders(val driver: PersonId, val passenger: PersonId)

@Serializable
data class Library(
    val people: List<Person> = emptyList(),
    val topics: List<Topic> = emptyList(),
    val rides: List<Ride> = emptyList(),
) {
    fun person(id: PersonId): Person? = people.firstOrNull { it.id == id }

    fun personNamed(name: String): Person? = people.firstOrNull { it.name == name.trim() }

    fun upsert(person: Person): Library {
        val index = people.indexOfFirst { it.id == person.id }
        return copy(people = if (index < 0) people + person else people.toMutableList().also { it[index] = person })
    }

    fun add(topic: Topic): Library = copy(topics = topics + topic)

    fun markUsed(id: TopicId, at: Long): Library =
        copy(topics = topics.map { if (it.id == id) it.copy(usedAt = at) else it })

    /** Records a ride and adds what was discovered to the passenger's interests. */
    fun record(ride: Ride): Library {
        val withRide = copy(rides = rides + ride)
        val passenger = person(ride.passenger) ?: return withRide
        return withRide.upsert(passenger.copy(interests = (passenger.interests + ride.discovered).distinct()))
    }

    /** What the rides of one pair say about them, whoever drove. */
    fun bond(riders: Riders): Bond {
        val pair = setOf(riders.driver, riders.passenger)
        return Bond(rides.filter { setOf(it.driver, it.passenger) == pair }.sortedBy { it.at })
    }
}

/** Relationship data, derived from rides so it can never disagree with them. */
data class Bond(val rides: List<Ride>) {
    val count: Int get() = rides.size
    val lastFlow: Flow? get() = rides.lastOrNull()?.flow
    val discovered: List<String> get() = rides.flatMap { it.discovered }.distinct()
}
