package com.gomsoo.saigil.screen

import com.gomsoo.saigil.library.Flow
import com.gomsoo.saigil.library.Library
import com.gomsoo.saigil.library.Person
import com.gomsoo.saigil.library.PersonId
import com.gomsoo.saigil.library.Relation
import com.gomsoo.saigil.library.Ride
import com.gomsoo.saigil.library.Riders
import com.gomsoo.saigil.library.Topic
import com.gomsoo.saigil.library.TopicId
import com.gomsoo.saigil.recommend.Opener
import com.gomsoo.saigil.recommend.TopicPick
import com.gomsoo.saigil.recommend.openers
import com.gomsoo.saigil.recommend.topicsFor
import com.gomsoo.saigil.vehicle.Restriction

const val NAME_LIMIT = 20
const val TEXT_LIMIT = 100

enum class Purpose { Recommend, Reflect }

enum class InputKind { Topic, Context }

enum class SttFailure { NoSpeech, Unavailable }

sealed interface Stt {
    data object Idle : Stt
    data class Listening(val partial: String) : Stt
    data class Failed(val reason: SttFailure) : Stt
}

/** What the speech recognizer reports, already translated from the SDK. */
sealed interface Heard {
    data class Partial(val text: String) : Heard
    data class Final(val text: String) : Heard
    data class Failed(val reason: SttFailure) : Heard
    data object Ended : Heard
}

/** Spoken input that stays editable. The keyboard works whether or not recognition does. */
data class Capture(val stt: Stt = Stt.Idle, val text: String = "")

enum class Problem { NoName, NoRelation, NoDriver, NoPassenger, SamePerson, NoText, NoFlow }

data class PersonDraft(
    val id: PersonId? = null,
    val name: String = "",
    val relation: Relation? = null,
    val places: String = "",
    val interests: String = "",
)

sealed interface Screen {
    data object Home : Screen
    data object People : Screen
    data class PersonEdit(val draft: PersonDraft, val problems: Set<Problem> = emptySet()) : Screen
    data class TopicCapture(val person: PersonId, val capture: Capture = Capture(), val problems: Set<Problem> = emptySet()) : Screen
    data class PickPair(
        val purpose: Purpose,
        val driver: PersonId?,
        val passenger: PersonId?,
        val problems: Set<Problem> = emptySet(),
    ) : Screen
    data class ChooseInput(val riders: Riders) : Screen
    data class Listen(
        val riders: Riders,
        val kind: InputKind,
        val capture: Capture = Capture(),
        val problems: Set<Problem> = emptySet(),
    ) : Screen
    data class Openers(val riders: Riders, val topic: String, val cards: List<Opener>) : Screen
    data class SavedTopics(val riders: Riders, val context: String, val picks: List<TopicPick>) : Screen
    data class Reflect(
        val riders: Riders,
        val flow: Flow? = null,
        val discovered: String = "",
        val problems: Set<Problem> = emptySet(),
    ) : Screen
}

/** The app shows the driving screen over [screen] whenever [vehicle] is restricted. */
data class Model(val screen: Screen = Screen.Home, val vehicle: Restriction = Restriction.Unknown)

sealed interface Event {
    data object OpenPeople : Event
    data object OpenRecommend : Event
    data object OpenReflect : Event
    data object Back : Event
    data object Done : Event
    /** The primary action of the current screen: save, confirm, or get recommendations. */
    data object Submit : Event
    data object AddPerson : Event
    data class EditPerson(val id: PersonId) : Event
    data class AddTopicFor(val id: PersonId) : Event
    data class EditDraft(val draft: PersonDraft) : Event
    data object Mic : Event
    data class Speech(val heard: Heard) : Event
    data class EditText(val text: String) : Event
    data class PickDriver(val id: PersonId) : Event
    data class PickPassenger(val id: PersonId) : Event
    data class ChooseKind(val kind: InputKind) : Event
    data class ChooseTopic(val id: TopicId) : Event
    data class PickFlow(val flow: Flow) : Event
    data class VehicleChanged(val restriction: Restriction) : Event
    data object PageHidden : Event
}

sealed interface Effect {
    /** An effect that changes the saved library. */
    sealed interface Write : Effect {
        fun applyTo(library: Library): Library
    }

    data class PutPerson(val person: Person) : Write {
        override fun applyTo(library: Library) = library.upsert(person)
    }

    data class AddTopic(val topic: Topic) : Write {
        override fun applyTo(library: Library) = library.add(topic)
    }

    data class UseTopic(val id: TopicId, val at: Long) : Write {
        override fun applyTo(library: Library) = library.markUsed(id, at)
    }

    data class RecordRide(val ride: Ride) : Write {
        override fun applyTo(library: Library) = library.record(ride)
    }

    data class BeginRide(val riders: Riders) : Effect
    data object StartListening : Effect
    data object StopListening : Effect
}

/** The clock and a fresh id, passed in so the reducer stays pure. */
data class Stamp(val now: Long, val id: String)

data class Next(val model: Model, val effects: List<Effect> = emptyList())

private typealias Step = Pair<Screen, List<Effect>>

private fun stay(screen: Screen): Step = screen to emptyList()

fun reduce(model: Model, event: Event, library: Library, stamp: Stamp): Next = when {
    event is Event.VehicleChanged -> {
        val (screen, effects) = if (event.restriction == Restriction.Restricted) silence(model.screen) else stay(model.screen)
        Next(Model(screen, event.restriction), effects)
    }
    event is Event.PageHidden -> silence(model.screen).let { (screen, effects) -> Next(model.copy(screen = screen), effects) }
    // Nothing on screen takes input while driving. Spoken suggestions come from the drive companion instead.
    model.vehicle == Restriction.Restricted -> Next(model)
    else -> step(model.screen, event, library, stamp).let { (screen, effects) -> Next(model.copy(screen = screen), effects) }
}

private fun silence(screen: Screen): Step {
    val capture = screen.capture?.takeIf { it.stt is Stt.Listening } ?: return stay(screen)
    return screen.withCapture(capture.copy(stt = Stt.Idle)) to stopIfListening(capture)
}

private fun stopIfListening(capture: Capture): List<Effect> =
    if (capture.stt is Stt.Listening) listOf(Effect.StopListening) else emptyList()

private val Screen.capture: Capture?
    get() = when (this) {
        is Screen.TopicCapture -> capture
        is Screen.Listen -> capture
        else -> null
    }

private fun Screen.withCapture(capture: Capture): Screen = when (this) {
    is Screen.TopicCapture -> copy(capture = capture, problems = emptySet())
    is Screen.Listen -> copy(capture = capture, problems = emptySet())
    else -> this
}

private fun step(screen: Screen, event: Event, library: Library, stamp: Stamp): Step {
    screen.capture?.let { capture -> listen(capture, event)?.let { (next, effects) -> return screen.withCapture(next) to effects } }
    return when (screen) {
        Screen.Home -> when (event) {
            Event.OpenPeople -> stay(Screen.People)
            Event.OpenRecommend -> stay(pickPair(Purpose.Recommend, library))
            Event.OpenReflect -> stay(pickPair(Purpose.Reflect, library))
            else -> stay(screen)
        }
        Screen.People -> when (event) {
            Event.Back -> stay(Screen.Home)
            Event.AddPerson -> stay(Screen.PersonEdit(PersonDraft()))
            is Event.EditPerson -> library.person(event.id)?.let { stay(Screen.PersonEdit(it.toDraft())) } ?: stay(screen)
            is Event.AddTopicFor -> stay(Screen.TopicCapture(event.id))
            else -> stay(screen)
        }
        is Screen.PersonEdit -> when (event) {
            Event.Back -> stay(Screen.People)
            is Event.EditDraft -> stay(screen.copy(draft = event.draft.limited(), problems = emptySet()))
            Event.Submit -> savePerson(screen, stamp)
            else -> stay(screen)
        }
        is Screen.TopicCapture -> when (event) {
            Event.Back -> leaveCapture(screen.capture, Screen.People)
            Event.Submit -> {
                val text = screen.capture.text.trim()
                if (text.isEmpty()) {
                    stay(screen.copy(problems = setOf(Problem.NoText)))
                } else {
                    Screen.People to stopIfListening(screen.capture) +
                        Effect.AddTopic(Topic(TopicId(stamp.id), text, setOf(screen.person), addedAt = stamp.now))
                }
            }
            else -> stay(screen)
        }
        is Screen.PickPair -> when (event) {
            Event.Back -> stay(Screen.Home)
            Event.OpenPeople -> stay(Screen.People)
            is Event.PickDriver -> stay(screen.copy(driver = event.id, problems = emptySet()))
            is Event.PickPassenger -> stay(screen.copy(passenger = event.id, problems = emptySet()))
            Event.Submit -> confirmPair(screen)
            else -> stay(screen)
        }
        is Screen.ChooseInput -> when (event) {
            Event.Back -> stay(Screen.PickPair(Purpose.Recommend, screen.riders.driver, screen.riders.passenger))
            is Event.ChooseKind -> stay(Screen.Listen(screen.riders, event.kind))
            else -> stay(screen)
        }
        is Screen.Listen -> when (event) {
            Event.Back -> leaveCapture(screen.capture, Screen.ChooseInput(screen.riders))
            Event.Submit -> recommend(screen, library, stamp)
            else -> stay(screen)
        }
        is Screen.Openers -> when (event) {
            Event.Back -> stay(Screen.ChooseInput(screen.riders))
            Event.Done -> stay(Screen.Home)
            else -> stay(screen)
        }
        is Screen.SavedTopics -> when (event) {
            Event.Back -> stay(Screen.Listen(screen.riders, InputKind.Context, Capture(text = screen.context)))
            Event.Done -> stay(Screen.Home)
            is Event.ChooseTopic -> screen.picks.firstOrNull { it.topic.id == event.id }?.let { pick ->
                Screen.Openers(screen.riders, pick.topic.text, openers(pick.topic.text, screen.riders, library)) to
                    listOf(Effect.UseTopic(pick.topic.id, stamp.now))
            } ?: stay(screen)
            else -> stay(screen)
        }
        is Screen.Reflect -> when (event) {
            Event.Back -> stay(Screen.PickPair(Purpose.Reflect, screen.riders.driver, screen.riders.passenger))
            is Event.PickFlow -> stay(screen.copy(flow = event.flow, problems = emptySet()))
            is Event.EditText -> stay(screen.copy(discovered = event.text.take(TEXT_LIMIT)))
            Event.Submit -> when (val flow = screen.flow) {
                null -> stay(screen.copy(problems = setOf(Problem.NoFlow)))
                else -> Screen.Home to listOf(
                    Effect.RecordRide(Ride(screen.riders.driver, screen.riders.passenger, stamp.now, flow, splitList(screen.discovered))),
                )
            }
            else -> stay(screen)
        }
    }
}

/** Mic, recognizer and keyboard events, shared by every screen that captures speech. */
private fun listen(capture: Capture, event: Event): Pair<Capture, List<Effect>>? = when (event) {
    Event.Mic -> if (capture.stt is Stt.Listening) {
        capture.copy(stt = Stt.Idle) to listOf(Effect.StopListening)
    } else {
        capture.copy(stt = Stt.Listening("")) to listOf(Effect.StartListening)
    }
    is Event.EditText -> capture.copy(text = event.text.take(TEXT_LIMIT)) to emptyList()
    is Event.Speech -> if (capture.stt !is Stt.Listening) {
        capture to emptyList()
    } else {
        when (val heard = event.heard) {
            is Heard.Partial -> capture.copy(stt = Stt.Listening(heard.text))
            is Heard.Final -> Capture(Stt.Idle, heard.text.trim().take(TEXT_LIMIT))
            is Heard.Failed -> capture.copy(stt = Stt.Failed(heard.reason))
            Heard.Ended -> capture.copy(stt = Stt.Idle)
        } to emptyList()
    }
    else -> null
}

private fun leaveCapture(capture: Capture, to: Screen): Step = to to stopIfListening(capture)

private fun pickPair(purpose: Purpose, library: Library) =
    Screen.PickPair(purpose, driver = library.people.firstOrNull { it.relation == Relation.Me }?.id, passenger = null)

private fun confirmPair(screen: Screen.PickPair): Step {
    val problems = buildSet {
        if (screen.driver == null) add(Problem.NoDriver)
        if (screen.passenger == null) add(Problem.NoPassenger)
        if (screen.driver != null && screen.driver == screen.passenger) add(Problem.SamePerson)
    }
    if (problems.isNotEmpty()) return stay(screen.copy(problems = problems))
    val riders = Riders(screen.driver!!, screen.passenger!!)
    return stay(
        when (screen.purpose) {
            Purpose.Recommend -> Screen.ChooseInput(riders)
            Purpose.Reflect -> Screen.Reflect(riders)
        },
    )
}

private fun recommend(screen: Screen.Listen, library: Library, stamp: Stamp): Step {
    val text = screen.capture.text.trim()
    if (text.isEmpty()) return stay(screen.copy(problems = setOf(Problem.NoText)))
    val result = when (screen.kind) {
        InputKind.Topic -> Screen.Openers(screen.riders, text, openers(text, screen.riders, library))
        InputKind.Context -> Screen.SavedTopics(screen.riders, text, topicsFor(text, screen.riders, library, stamp.now))
    }
    return result to stopIfListening(screen.capture) + Effect.BeginRide(screen.riders)
}

private fun savePerson(screen: Screen.PersonEdit, stamp: Stamp): Step {
    val draft = screen.draft
    val problems = buildSet {
        if (draft.name.isBlank()) add(Problem.NoName)
        if (draft.relation == null) add(Problem.NoRelation)
    }
    if (problems.isNotEmpty()) return stay(screen.copy(problems = problems))
    val person = Person(
        id = draft.id ?: PersonId(stamp.id),
        name = draft.name.trim(),
        relation = draft.relation!!,
        places = splitList(draft.places),
        interests = splitList(draft.interests),
    )
    val next = if (draft.id == null) Screen.TopicCapture(person.id) else Screen.People
    return next to listOf(Effect.PutPerson(person))
}

private fun Person.toDraft() = PersonDraft(id, name, relation, places.joinToString(", "), interests.joinToString(", "))

private fun PersonDraft.limited() = copy(
    name = name.take(NAME_LIMIT),
    places = places.take(TEXT_LIMIT),
    interests = interests.take(TEXT_LIMIT),
)

fun splitList(text: String): List<String> = text.split(',', '，', '\n').map { it.trim() }.filter { it.isNotEmpty() }.distinct()
