package com.gomsoo.saigil.library

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class PersonId(val value: String)

@Serializable
@JvmInline
value class TopicId(val value: String)

/** Two different people, in no order: Pair.of(a, b) == Pair.of(b, a). */
@Serializable
@ConsistentCopyVisibility
data class Pair private constructor(val first: PersonId, val second: PersonId) {
    init {
        // Also runs when a pair is read back from library.json.
        require(first.value < second.value) { "a pair is two different people in sorted order" }
    }

    companion object {
        /** Null when [a] and [b] are the same person. */
        fun of(a: PersonId, b: PersonId): Pair? = when {
            a == b -> null
            a.value < b.value -> Pair(a, b)
            else -> Pair(b, a)
        }
    }
}

/** How someone relates to the person they are seen from. */
enum class Relation { Parent, Child, Partner, Sibling, Friend, Other }

/** The same relation seen from the other side. */
val Relation.reverse: Relation
    get() = when (this) {
        Relation.Parent -> Relation.Child
        Relation.Child -> Relation.Parent
        else -> this
    }

/** How the user speaks to someone: 존댓말 or 반말. */
enum class Register { Formal, Casual }

/** [to] is [from]'s [relation]: Tie(me, mom, Parent) reads "엄마 is my parent". */
@Serializable
data class Tie(val from: PersonId, val to: PersonId, val relation: Relation)

@Serializable
data class Person(
    val id: PersonId,
    val name: String,
    val places: List<String> = emptyList(),
    val interests: List<String> = emptyList(),
)

@Serializable
data class Topic(
    val id: TopicId,
    val text: String,
    /** The two people this topic is saved for. */
    val pair: Pair,
    val addedAt: Long,
    val usedAt: Long? = null,
)

enum class Flow { Smooth, Okay, Awkward }

@Serializable
data class Ride(
    val me: PersonId,
    val companion: PersonId,
    val at: Long,
    val flow: Flow,
    val discovered: List<String> = emptyList(),
)

/** The current user and the person with them. Never the same person. */
@ConsistentCopyVisibility
data class Riders private constructor(val me: PersonId, val companion: PersonId) {
    val pair: Pair get() = Pair.of(me, companion)!!

    companion object {
        /** Null when [me] and [companion] are the same person. */
        fun of(me: PersonId, companion: PersonId): Riders? = if (me == companion) null else Riders(me, companion)
    }
}

@Serializable
data class Library(
    val people: List<Person> = emptyList(),
    val ties: List<Tie> = emptyList(),
    val topics: List<Topic> = emptyList(),
    val rides: List<Ride> = emptyList(),
) {
    fun person(id: PersonId): Person? = people.firstOrNull { it.id == id }

    fun personNamed(name: String): Person? = people.firstOrNull { it.name == name.trim() }

    fun upsert(person: Person): Library {
        val index = people.indexOfFirst { it.id == person.id }
        return copy(people = if (index < 0) people + person else people.toMutableList().also { it[index] = person })
    }

    /** What [riders]' companion is to me, or null if it was never said. */
    fun relation(riders: Riders): Relation? = ties.firstOrNull { it.from == riders.me && it.to == riders.companion }?.relation

    /** Records that the companion is my [relation], and the reverse, replacing whatever the pair had. */
    fun relate(riders: Riders, relation: Relation): Library = copy(
        ties = ties.filter { Pair.of(it.from, it.to) != riders.pair } +
            Tie(riders.me, riders.companion, relation) +
            Tie(riders.companion, riders.me, relation.reverse),
    )

    /** Only a parent is spoken to formally. A pair with no relation yet is spoken to casually. */
    fun register(riders: Riders): Register = if (relation(riders) == Relation.Parent) Register.Formal else Register.Casual

    fun add(topic: Topic): Library = copy(topics = topics + topic)

    fun topicsOf(pair: Pair): List<Topic> = topics.filter { it.pair == pair }

    fun markUsed(id: TopicId, at: Long): Library =
        copy(topics = topics.map { if (it.id == id) it.copy(usedAt = at) else it })

    /** Records a ride and adds what was discovered to the companion's interests. */
    fun record(ride: Ride): Library {
        val withRide = copy(rides = rides + ride)
        val companion = person(ride.companion) ?: return withRide
        return withRide.upsert(companion.copy(interests = (companion.interests + ride.discovered).distinct()))
    }

    /** What the rides of one pair say about them, whoever was the user. */
    fun bond(pair: Pair): Bond = Bond(rides.filter { Pair.of(it.me, it.companion) == pair }.sortedBy { it.at })
}

/** Relationship data, derived from rides so it can never disagree with them. */
data class Bond(val rides: List<Ride>) {
    val count: Int get() = rides.size
    val lastFlow: Flow? get() = rides.lastOrNull()?.flow
    val discovered: List<String> get() = rides.flatMap { it.discovered }.distinct()
}
