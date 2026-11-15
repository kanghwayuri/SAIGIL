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

/** What the companion is picked for. */
enum class Purpose { SaveTopic, Recommend, Reflect }

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

enum class Problem { NoName, NoCompanion, NoRelation, NoText, NoFlow }

data class PersonDraft(
    val id: PersonId? = null,
    val name: String = "",
    val places: String = "",
    val interests: String = "",
)

sealed interface Screen {
    /** Every entry starts here: the user says which profile is theirs. */
    data object WhoAmI : Screen
    data object Home : Screen
    data object People : Screen
    /** [forMe] when the profile is being made from [WhoAmI], so saving it makes it the user. */
    data class PersonEdit(val draft: PersonDraft, val forMe: Boolean = false, val problems: Set<Problem> = emptySet()) : Screen
    /**
     * The user is fixed as [me]; only the companion is picked. [relation] is what the user chose on this screen.
     * The relation is asked when the pair has none yet, or when [changingRelation].
     */
    data class PickCompanion(
        val purpose: Purpose,
        val me: PersonId,
        val companion: PersonId? = null,
        val relation: Relation? = null,
        val changingRelation: Boolean = false,
        val problems: Set<Problem> = emptySet(),
    ) : Screen
    data class TopicCapture(val riders: Riders, val capture: Capture = Capture(), val problems: Set<Problem> = emptySet()) : Screen
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
data class Model(val screen: Screen = Screen.WhoAmI, val vehicle: Restriction = Restriction.Unknown)

sealed interface Event {
    data class ChooseMe(val id: PersonId) : Event
    data object ChangeMe : Event
    data object OpenPeople : Event
    data object OpenSaveTopic : Event
    data object OpenRecommend : Event
    data object OpenReflect : Event
    data object Back : Event
    data object Done : Event
    /** The primary action of the current screen: save, confirm, or get recommendations. */
    data object Submit : Event
    data object AddPerson : Event
    data class EditPerson(val id: PersonId) : Event
    data class EditDraft(val draft: PersonDraft) : Event
    data object Mic : Event
    data class Speech(val heard: Heard) : Event
    data class EditText(val text: String) : Event
    data class ChooseCompanion(val id: PersonId) : Event
    data class PickRelation(val relation: Relation) : Event
    data object ChangeRelation : Event
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

    data class Relate(val riders: Riders, val relation: Relation) : Write {
        override fun applyTo(library: Library) = library.relate(riders, relation)
    }

    /** Makes [me] the current user for the rest of the process. */
    data class BecomeMe(val me: PersonId) : Effect

    data class BeginRide(val riders: Riders) : Effect
    data object StartListening : Effect
    data object StopListening : Effect
}

/** The clock and a fresh id, passed in so the reducer stays pure. */
data class Stamp(val now: Long, val id: String)

data class Next(val model: Model, val effects: List<Effect> = emptyList())

private typealias Step = Pair<Screen, List<Effect>>

private fun stay(screen: Screen): Step = screen to emptyList()

/** [me] is the current user, null until [Screen.WhoAmI] is answered. */
fun reduce(model: Model, event: Event, library: Library, me: PersonId?, stamp: Stamp): Next = when {
    event is Event.VehicleChanged -> {
        val (screen, effects) = if (event.restriction == Restriction.Restricted) silence(model.screen) else stay(model.screen)
        Next(Model(screen, event.restriction), effects)
    }
    event is Event.PageHidden -> silence(model.screen).let { (screen, effects) -> Next(model.copy(screen = screen), effects) }
    // Nothing on screen takes input while driving. Spoken suggestions come from the drive companion instead.
    model.vehicle == Restriction.Restricted -> Next(model)
    else -> step(model.screen, event, library, me, stamp).let { (screen, effects) -> Next(model.copy(screen = screen), effects) }
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

private fun step(screen: Screen, event: Event, library: Library, me: PersonId?, stamp: Stamp): Step {
    screen.capture?.let { capture -> listen(capture, event)?.let { (next, effects) -> return screen.withCapture(next) to effects } }
    fun pickCompanion(purpose: Purpose): Screen = me?.let { Screen.PickCompanion(purpose, it) } ?: Screen.WhoAmI
    return when (screen) {
        Screen.WhoAmI -> when (event) {
            is Event.ChooseMe -> library.person(event.id)?.let { Screen.Home to listOf(Effect.BecomeMe(it.id)) } ?: stay(screen)
            Event.AddPerson -> stay(Screen.PersonEdit(PersonDraft(), forMe = true))
            Event.Back -> stay(if (me != null) Screen.Home else screen)
            else -> stay(screen)
        }
        Screen.Home -> when (event) {
            Event.ChangeMe -> stay(Screen.WhoAmI)
            Event.OpenPeople -> stay(Screen.People)
            Event.OpenRecommend -> stay(pickCompanion(Purpose.Recommend))
            Event.OpenReflect -> stay(pickCompanion(Purpose.Reflect))
            else -> stay(screen)
        }
        Screen.People -> when (event) {
            Event.Back -> stay(Screen.Home)
            Event.AddPerson -> stay(Screen.PersonEdit(PersonDraft()))
            is Event.EditPerson -> library.person(event.id)?.let { stay(Screen.PersonEdit(it.toDraft())) } ?: stay(screen)
            Event.OpenSaveTopic -> stay(pickCompanion(Purpose.SaveTopic))
            else -> stay(screen)
        }
        is Screen.PersonEdit -> when (event) {
            Event.Back -> stay(if (screen.forMe) Screen.WhoAmI else Screen.People)
            is Event.EditDraft -> stay(screen.copy(draft = event.draft.limited(), problems = emptySet()))
            Event.Submit -> savePerson(screen, stamp)
            else -> stay(screen)
        }
        is Screen.PickCompanion -> when (event) {
            Event.Back -> stay(if (screen.purpose == Purpose.SaveTopic) Screen.People else Screen.Home)
            Event.OpenPeople -> stay(Screen.People)
            is Event.ChooseCompanion -> stay(
                if (event.id == screen.me) screen else screen.copy(companion = event.id, relation = null, changingRelation = false, problems = emptySet()),
            )
            is Event.PickRelation -> stay(screen.copy(relation = event.relation, problems = emptySet()))
            Event.ChangeRelation -> stay(screen.copy(changingRelation = true))
            Event.Submit -> confirmCompanion(screen, library)
            else -> stay(screen)
        }
        is Screen.TopicCapture -> when (event) {
            Event.Back -> leaveCapture(screen.capture, backToPick(Purpose.SaveTopic, screen.riders))
            Event.Submit -> {
                val text = screen.capture.text.trim()
                if (text.isEmpty()) {
                    stay(screen.copy(problems = setOf(Problem.NoText)))
                } else {
                    Screen.People to stopIfListening(screen.capture) +
                        Effect.AddTopic(Topic(TopicId(stamp.id), text, screen.riders.pair, addedAt = stamp.now))
                }
            }
            else -> stay(screen)
        }
        is Screen.ChooseInput -> when (event) {
            Event.Back -> stay(backToPick(Purpose.Recommend, screen.riders))
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
            Event.Back -> stay(backToPick(Purpose.Reflect, screen.riders))
            is Event.PickFlow -> stay(screen.copy(flow = event.flow, problems = emptySet()))
            is Event.EditText -> stay(screen.copy(discovered = event.text.take(TEXT_LIMIT)))
            Event.Submit -> when (val flow = screen.flow) {
                null -> stay(screen.copy(problems = setOf(Problem.NoFlow)))
                else -> Screen.Home to listOf(
                    Effect.RecordRide(Ride(screen.riders.me, screen.riders.companion, stamp.now, flow, splitList(screen.discovered))),
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
    is Event.Speech -> when (val heard = event.heard) {
        // The Pleos recognizer reports Ended before the final text, so the final text is kept whenever it arrives.
        is Heard.Final -> Capture(Stt.Idle, heard.text.trim().take(TEXT_LIMIT))
        else -> when (val stt = capture.stt) {
            !is Stt.Listening -> capture
            else -> when (heard) {
                is Heard.Partial -> capture.copy(stt = Stt.Listening(heard.text))
                is Heard.Failed -> capture.copy(stt = Stt.Failed(heard.reason))
                else -> Capture(Stt.Idle, stt.partial.trim().ifBlank { capture.text }.take(TEXT_LIMIT))
            }
        }
    } to emptyList()
    else -> null
}

private fun leaveCapture(capture: Capture, to: Screen): Step = to to stopIfListening(capture)

private fun backToPick(purpose: Purpose, riders: Riders) = Screen.PickCompanion(purpose, riders.me, riders.companion)

/** A pair is asked its relation once. After that it is only changed on request. */
private fun confirmCompanion(screen: Screen.PickCompanion, library: Library): Step {
    val riders = screen.companion?.let { Riders.of(screen.me, it) } ?: return stay(screen.copy(problems = setOf(Problem.NoCompanion)))
    val known = library.relation(riders)
    val relation = screen.relation ?: known ?: return stay(screen.copy(problems = setOf(Problem.NoRelation)))
    val next = when (screen.purpose) {
        Purpose.SaveTopic -> Screen.TopicCapture(riders)
        Purpose.Recommend -> Screen.ChooseInput(riders)
        Purpose.Reflect -> Screen.Reflect(riders)
    }
    return next to if (relation == known) emptyList() else listOf(Effect.Relate(riders, relation))
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
    if (draft.name.isBlank()) return stay(screen.copy(problems = setOf(Problem.NoName)))
    val person = Person(
        id = draft.id ?: PersonId(stamp.id),
        name = draft.name.trim(),
        places = splitList(draft.places),
        interests = splitList(draft.interests),
    )
    return if (screen.forMe) {
        Screen.Home to listOf(Effect.PutPerson(person), Effect.BecomeMe(person.id))
    } else {
        Screen.People to listOf(Effect.PutPerson(person))
    }
}

private fun Person.toDraft() = PersonDraft(id, name, places.joinToString(", "), interests.joinToString(", "))

private fun PersonDraft.limited() = copy(
    name = name.take(NAME_LIMIT),
    places = places.take(TEXT_LIMIT),
    interests = interests.take(TEXT_LIMIT),
)

fun splitList(text: String): List<String> = text.split(',', '，', '\n').map { it.trim() }.filter { it.isNotEmpty() }.distinct()
