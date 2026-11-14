package com.gomsoo.saigil.session

enum class Role { Parent, Child, Partner, Friend, Other }
enum class StartField { MyAge, MyWork, MyArea, CompanionAge, CompanionGender, CompanionStyle, CompanionInterest }

enum class Guide { Off, OnceGeneric }

enum class Restriction { Parked, Restricted, Unknown }

sealed interface Material {
    data class Story(
        val scene: String = "",
        val feeling: String = "",
        val choice: String = "",
        val leftover: String = "",
    ) : Material

    data class Interest(
        val topic: String = "",
        val source: String = "",
    ) : Material
}

object Limit {
    const val SCENE = 300
    const val STORY_OPTIONAL = 300
    const val INTEREST = 200
    const val RESULT = 1500
}

enum class Problem {
    NoRole, NotAlone, NotParkedConfirmed, VehicleNotParked,
    NoMethod,
    NoScene, NoTopic, NoSource,
    BlankResult,
}

/** Each variant is one screen. Only the preparation screens can hold personal text. */
sealed interface Session {
    /** Becomes [Protected] on Abort, PageHidden, or any vehicle state other than parked (F02, F05). */
    sealed interface Preparing : Session

    /** W01 */
    data class Start(
        val role: Role? = null,
        val step: Int = 0,
        val details: Map<StartField, String> = emptyMap(),
        val alone: Boolean = false,
        val parked: Boolean = false,
        val problems: Set<Problem> = emptySet(),
    ) : Preparing

    /** W09. Choices only, no personal text. */
    data class Situation(
        val role: Role,
        val profile: Profile,
        val purpose: Purpose? = null,
        val known: Known? = null,
    ) : Preparing

    /**
     * W10. The cards are derived from [situation] on every read and never stored, so Previous
     * restores the W09 choices and the next Next recomputes them. [chosen] is null or one of [cards].
     */
    data class Methods(
        val situation: Situation,
        val chosen: Method? = null,
        val problems: Set<Problem> = emptySet(),
    ) : Preparing {
        val cards: List<Recommended> get() = recommend(situation.purpose, situation.known, situation.profile)
    }

    /** W02 for a [Material.Story], W02B for a [Material.Interest]. [method] is the Share card being prepared. */
    data class Compose(
        val role: Role,
        val material: Material,
        val method: Method,
        val problems: Set<Problem> = emptySet(),
    ) : Preparing

    /** W03 */
    data class Edit(
        val role: Role,
        val material: Material,
        val result: String,
        val method: Method,
        val showSource: Boolean = false,
        val problems: Set<Problem> = emptySet(),
    ) : Preparing

    /** W04 */
    data class GuideChoice(val role: Role, val method: Method, val guide: Guide = Guide.Off) : Preparing

    /**
     * W05. Holds only catalog text, so it survives PageHidden while parked and goes once driving starts.
     * [role] picks the register of the first words. [method] is null when the user left W10 without a card.
     */
    data class Ready(val guide: Guide, val role: Role, val method: Method?) : Session

    /** W06 */
    data object GuideWaiting : Session

    /** W07 */
    data object Ended : Session

    /** W08 */
    data object Protected : Session
}

data class Model(
    val session: Session = Session.Start(),
    val vehicle: Restriction = Restriction.Unknown,
)

sealed interface Event {
    data class SelectRole(val role: Role) : Event
    data class SetStartDetail(val field: StartField, val value: String) : Event
    data object Previous : Event
    data class SetAlone(val value: Boolean) : Event
    data class SetParked(val value: Boolean) : Event
    data class EditMaterial(val material: Material) : Event
    data class EditResult(val text: String) : Event
    data object Next : Event
    data object SwitchToInterest : Event
    data object SwitchToStory : Event
    data object ToggleSource : Event
    data class ChooseGuide(val guide: Guide) : Event
    data object Finish : Event
    data object Abort : Event
    data object PageHidden : Event
    data class VehicleChanged(val restriction: Restriction) : Event
    data class SetPurpose(val purpose: Purpose?) : Event
    data class SetKnown(val known: Known?) : Event
    data class ChooseMethod(val method: Method) : Event
    data object SkipMethod : Event
    data object Restart : Event
}

fun problems(start: Session.Start, vehicle: Restriction): Set<Problem> = buildSet {
    if (start.role == null) add(Problem.NoRole)
    if (!start.alone) add(Problem.NotAlone)
    if (!start.parked) add(Problem.NotParkedConfirmed)
    if (vehicle != Restriction.Parked) add(Problem.VehicleNotParked)
}

fun problems(material: Material): Set<Problem> = when (material) {
    is Material.Story -> buildSet { if (material.scene.isBlank()) add(Problem.NoScene) }
    is Material.Interest -> buildSet {
        if (material.topic.isBlank()) add(Problem.NoTopic)
        if (material.source.isBlank()) add(Problem.NoSource)
    }
}

fun problems(result: String): Set<Problem> =
    if (result.isBlank()) setOf(Problem.BlankResult) else emptySet()

fun Material.parts(): List<String> = when (this) {
    is Material.Story -> listOf(scene, feeling, choice, leftover)
    is Material.Interest -> listOf(topic, source)
}

/** W03 starts from the user's own words only, in input order. Nothing is generated. */
fun Material.draft(): String = parts().filter { it.isNotBlank() }.joinToString("\n") { it.trim() }

private fun Material.clamped(): Material = when (this) {
    is Material.Story -> Material.Story(
        scene = scene.take(Limit.SCENE),
        feeling = feeling.take(Limit.STORY_OPTIONAL),
        choice = choice.take(Limit.STORY_OPTIONAL),
        leftover = leftover.take(Limit.STORY_OPTIONAL),
    )
    is Material.Interest -> Material.Interest(
        topic = topic.take(Limit.INTEREST),
        source = source.take(Limit.INTEREST),
    )
}

fun reduce(model: Model, event: Event): Model {
    if (event is Event.VehicleChanged) {
        return Model(onVehicle(model.session, event.restriction), event.restriction)
    }
    val session = model.session
    if ((event == Event.Abort || event == Event.PageHidden) && session is Session.Preparing) {
        return model.copy(session = Session.Protected)
    }
    val next = when (session) {
        is Session.Start -> onStart(session, event, model.vehicle)
        is Session.Situation -> onSituation(session, event)
        is Session.Methods -> onMethods(session, event)
        is Session.Compose -> onCompose(session, event)
        is Session.Edit -> onEdit(session, event)
        is Session.GuideChoice -> when (event) {
            is Event.ChooseGuide -> session.copy(guide = event.guide)
            Event.Finish -> Session.Ready(session.guide, session.role, session.method)
            else -> session
        }
        is Session.Ended, Session.Protected ->
            if (event == Event.Restart && model.vehicle == Restriction.Parked) Session.Start() else session
        is Session.Ready, Session.GuideWaiting -> session
    }
    return model.copy(session = next)
}

private fun onVehicle(session: Session, restriction: Restriction): Session = when {
    restriction == Restriction.Parked -> when (session) {
        // No playback exists yet, so returning to park with the guide still pending ends silently.
        Session.GuideWaiting -> Session.Ended
        is Session.Start ->
            if (session.problems.isEmpty()) session else session.copy(problems = problems(session, restriction))
        else -> session
    }
    session is Session.Preparing -> Session.Protected
    session is Session.Ready -> when {
        restriction == Restriction.Restricted && session.guide == Guide.OnceGeneric -> Session.GuideWaiting
        else -> Session.Ended
    }
    session == Session.GuideWaiting && restriction == Restriction.Unknown -> Session.Ended
    else -> session
}

private fun onStart(start: Session.Start, event: Event, vehicle: Restriction): Session {
    val edited = when (event) {
        is Event.SelectRole -> start.copy(role = event.role)
        is Event.SetStartDetail -> start.copy(details = start.details + (event.field to event.value))
        Event.Previous -> start.copy(step = (start.step - 1).coerceAtLeast(0), problems = emptySet())
        is Event.SetAlone -> start.copy(alone = event.value)
        is Event.SetParked -> start.copy(parked = event.value)
        Event.Next -> {
            if (start.step == 0 && start.role == null) return start.copy(problems = setOf(Problem.NoRole))
            if (start.step < 3) return start.copy(step = start.step + 1, problems = emptySet())
            val found = problems(start, vehicle)
            return if (found.isEmpty()) Session.Situation(start.role!!, profileOf(start.details)) else start.copy(problems = found)
        }
        else -> return start
    }
    return if (edited.problems.isEmpty()) edited else edited.copy(problems = problems(edited, vehicle))
}

private fun onSituation(situation: Session.Situation, event: Event): Session = when (event) {
    is Event.SetPurpose -> situation.copy(purpose = event.purpose)
    is Event.SetKnown -> situation.copy(known = event.known)
    Event.Next -> Session.Methods(situation)
    else -> situation
}

private fun onMethods(methods: Session.Methods, event: Event): Session = when (event) {
    is Event.ChooseMethod ->
        if (methods.cards.any { it.method == event.method }) methods.copy(chosen = event.method, problems = emptySet()) else methods
    Event.Previous -> methods.situation
    Event.SkipMethod -> Session.Ready(Guide.Off, methods.situation.role, method = null)
    Event.Next -> {
        val role = methods.situation.role
        when (val chosen = methods.chosen) {
            null -> methods.copy(problems = setOf(Problem.NoMethod))
            else -> if (chosen.needsOwnStory) Session.Compose(role, Material.Story(), chosen) else Session.Ready(Guide.Off, role, chosen)
        }
    }
    else -> methods
}

private fun onCompose(compose: Session.Compose, event: Event): Session = when (event) {
    is Event.EditMaterial -> {
        if (event.material::class != compose.material::class) {
            compose
        } else {
            val material = event.material.clamped()
            compose.copy(
                material = material,
                problems = if (compose.problems.isEmpty()) compose.problems else problems(material),
            )
        }
    }
    Event.SwitchToInterest -> Session.Compose(compose.role, Material.Interest(), compose.method)
    Event.SwitchToStory -> Session.Compose(compose.role, Material.Story(), compose.method)
    Event.Next -> {
        val found = problems(compose.material)
        if (found.isEmpty()) Session.Edit(compose.role, compose.material, compose.material.draft(), compose.method)
        else compose.copy(problems = found)
    }
    else -> compose
}

private fun onEdit(edit: Session.Edit, event: Event): Session = when (event) {
    is Event.EditResult -> {
        val result = event.text.take(Limit.RESULT)
        edit.copy(result = result, problems = if (edit.problems.isEmpty()) edit.problems else problems(result))
    }
    Event.ToggleSource -> edit.copy(showSource = !edit.showSource)
    Event.Next -> {
        val found = problems(edit.result)
        if (found.isEmpty()) Session.GuideChoice(edit.role, edit.method) else edit.copy(problems = found)
    }
    else -> edit
}
