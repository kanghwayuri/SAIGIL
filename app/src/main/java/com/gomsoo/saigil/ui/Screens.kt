package com.gomsoo.saigil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gomsoo.saigil.R
import com.gomsoo.saigil.Ui
import com.gomsoo.saigil.library.Flow
import com.gomsoo.saigil.library.Library
import com.gomsoo.saigil.library.Pair
import com.gomsoo.saigil.library.PersonId
import com.gomsoo.saigil.library.Relation
import com.gomsoo.saigil.library.Riders
import com.gomsoo.saigil.recommend.Approach
import com.gomsoo.saigil.recommend.Opener
import com.gomsoo.saigil.recommend.Reason
import com.gomsoo.saigil.recommend.Situation
import com.gomsoo.saigil.recommend.Suggestion
import com.gomsoo.saigil.screen.Capture
import com.gomsoo.saigil.screen.Event
import com.gomsoo.saigil.screen.InputKind
import com.gomsoo.saigil.screen.NAME_LIMIT
import com.gomsoo.saigil.screen.Problem
import com.gomsoo.saigil.screen.Purpose
import com.gomsoo.saigil.screen.Screen
import com.gomsoo.saigil.screen.Stt
import com.gomsoo.saigil.screen.SttFailure
import com.gomsoo.saigil.screen.TEXT_LIMIT
import com.gomsoo.saigil.ui.theme.PleosColor
import com.gomsoo.saigil.ui.theme.PleosType
import com.gomsoo.saigil.vehicle.Restriction

@Composable
fun SaigilApp(ui: Ui, send: (Event) -> Unit) {
    if (ui.model.vehicle == Restriction.Restricted) {
        DrivingScreen(ui)
        return
    }
    val screen = ui.model.screen
    // Before the user is known there is nowhere to go back to, so the system leaves the app.
    BackHandler(enabled = screen != Screen.Home && !(screen == Screen.WhoAmI && ui.me == null)) { send(Event.Back) }
    val library = ui.library
    when (screen) {
        Screen.WhoAmI -> WhoAmIScreen(ui, send)
        Screen.Home -> HomeScreen(ui, send)
        Screen.People -> PeopleScreen(library, ui.me, send)
        is Screen.PersonEdit -> PersonEditScreen(screen, send)
        is Screen.PickCompanion -> PickCompanionScreen(screen, library, send)
        is Screen.TopicCapture -> CaptureScreen(
            title = stringResource(R.string.capture_title),
            note = stringResource(R.string.capture_about, library.nameOf(screen.riders.companion)),
            field = stringResource(R.string.capture_field),
            capture = screen.capture,
            problems = screen.problems,
            back = stringResource(R.string.capture_later),
            submit = stringResource(R.string.action_save),
            send = send,
        )
        is Screen.ChooseInput -> ChooseInputScreen(send)
        is Screen.Listen -> CaptureScreen(
            title = stringResource(if (screen.kind == InputKind.Topic) R.string.listen_topic_title else R.string.listen_context_title),
            note = library.namesOf(screen.riders),
            field = stringResource(if (screen.kind == InputKind.Topic) R.string.listen_topic_field else R.string.listen_context_field),
            capture = screen.capture,
            problems = screen.problems,
            back = stringResource(R.string.action_back),
            submit = stringResource(R.string.listen_submit),
            send = send,
        )
        is Screen.Openers -> OpenersScreen(screen, send)
        is Screen.SavedTopics -> SavedTopicsScreen(screen, send)
        is Screen.Reflect -> ReflectScreen(screen, library, send)
        is Screen.Reflected -> ReflectedScreen(screen, library, send)
        is Screen.PersonSaved -> PersonSavedScreen(screen, library, send)
    }
}

@Composable
private fun ScreenFrame(
    title: String,
    note: String? = null,
    actions: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(PleosColor.BackgroundPrimary).safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 1240.dp).fillMaxWidth().padding(horizontal = 40.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Text(
                    title,
                    style = PleosType.Title,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().semantics { heading() },
                )
                if (note != null) {
                    Text(note, style = PleosType.Body, color = PleosColor.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                content()
            }
        }
        if (actions != null) {
            HorizontalDivider(color = PleosColor.Divider)
            FlowRow(
                Modifier.widthIn(max = 1240.dp).fillMaxWidth().padding(horizontal = 40.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) { actions() }
        }
    }
}

/** Items side by side on a wide window, stacked on a narrow one. */
@Composable
private fun <T> Columns(items: List<T>, perRow: Int, item: @Composable ColumnScope.(T) -> Unit) {
    BoxWithConstraints {
        val columns = if (maxWidth >= 1000.dp) perRow else 1
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            items.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    row.forEach { Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) { item(it) } }
                    repeat(columns - row.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String) {
    Text(
        text,
        style = PleosType.Caption,
        color = PleosColor.TextPrimary,
        modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(PleosColor.BasicEnabled).padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

@Composable
private fun Problem.message(): String = stringResource(
    when (this) {
        Problem.NoName -> R.string.problem_no_name
        Problem.NoCompanion -> R.string.problem_no_companion
        Problem.NoRelation -> R.string.problem_no_relation
        Problem.NoText -> R.string.problem_no_text
        Problem.NoFlow -> R.string.problem_no_flow
    },
)

@Composable
private fun Problems(problems: Set<Problem>, vararg shown: Problem) {
    shown.filter { it in problems }.forEach { ErrorText(it.message()) }
}

@Composable
private fun Relation.label(): String = stringResource(
    when (this) {
        Relation.Parent -> R.string.relation_parent
        Relation.Child -> R.string.relation_child
        Relation.Partner -> R.string.relation_partner
        Relation.Sibling -> R.string.relation_sibling
        Relation.Friend -> R.string.relation_friend
        Relation.Other -> R.string.relation_other
    },
)

private fun Library.nameOf(id: PersonId): String = person(id)?.name.orEmpty()

private fun Library.namesOf(riders: Riders): String = "${nameOf(riders.me)} · ${nameOf(riders.companion)}"

@Composable
private fun WhoAmIScreen(ui: Ui, send: (Event) -> Unit) {
    val people = ui.library.people
    ScreenFrame(
        title = stringResource(R.string.whoami_title),
        note = stringResource(if (people.isEmpty()) R.string.whoami_empty else R.string.whoami_body),
        actions = {
            if (ui.me != null) PleosButton(stringResource(R.string.action_back), { send(Event.Back) })
            PleosButton(
                stringResource(R.string.people_add),
                { send(Event.AddPerson) },
                style = if (people.isEmpty()) ButtonStyle.Primary else ButtonStyle.Secondary,
            )
        },
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            people.forEach { person ->
                ChoiceOption(person.name, ui.me == person.id, { send(Event.ChooseMe(person.id)) }, Modifier.widthIn(min = 200.dp))
            }
        }
    }
}

@Composable
private fun HomeScreen(ui: Ui, send: (Event) -> Unit) {
    val ride = ui.ride
    ScreenFrame(
        title = stringResource(R.string.home_title),
        note = ride?.let { stringResource(R.string.ride_active, ui.library.namesOf(it.riders)) }
            ?: stringResource(R.string.home_body),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.home_me, ui.me?.let(ui.library::nameOf).orEmpty()), style = PleosType.Body)
            PleosButton(stringResource(R.string.home_change_me), { send(Event.ChangeMe) }, size = ButtonSize.Small)
        }
        val modes = listOf(
            Triple(R.string.home_input, R.string.home_input_body, Event.OpenPeople),
            Triple(R.string.home_recommend, R.string.home_recommend_body, Event.OpenRecommend),
            Triple(R.string.home_reflect, R.string.home_reflect_body, Event.OpenReflect),
        )
        Columns(modes, perRow = 3) { (title, body, event) ->
            SelectableCard(selected = false, onSelect = { send(event) }) {
                Text(stringResource(title), style = PleosType.Body)
                Text(stringResource(body), style = PleosType.Caption, color = PleosColor.TextSecondary)
            }
        }
    }
}

@Composable
private fun PeopleScreen(library: Library, me: PersonId?, send: (Event) -> Unit) {
    ScreenFrame(
        title = stringResource(R.string.people_title),
        actions = {
            PleosButton(stringResource(R.string.action_back), { send(Event.Back) })
            PleosButton(stringResource(R.string.people_add), { send(Event.AddPerson) })
            PleosButton(stringResource(R.string.people_save_topic), { send(Event.OpenSaveTopic) }, style = ButtonStyle.Primary)
        },
    ) {
        if (library.people.isEmpty()) Text(stringResource(R.string.people_empty), style = PleosType.Body, color = PleosColor.TextSecondary)
        Columns(library.people, perRow = 2) { person ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(PleosColor.BackgroundSecondary).padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(person.name, style = PleosType.Body)
                    if (person.id == me) Chip(stringResource(R.string.people_me))
                }
                if (person.places.isNotEmpty()) {
                    Text(stringResource(R.string.people_places, person.places.joinToString(", ")), style = PleosType.Caption, color = PleosColor.TextSecondary)
                }
                if (person.interests.isNotEmpty()) {
                    Text(stringResource(R.string.people_interests, person.interests.joinToString(", ")), style = PleosType.Caption, color = PleosColor.TextSecondary)
                }
                me?.let { Pair.of(it, person.id) }?.let { pair ->
                    Text(
                        stringResource(R.string.people_topics, library.topicsOf(pair).size),
                        style = PleosType.Caption,
                        color = PleosColor.TextTertiary,
                    )
                }
                PleosButton(stringResource(R.string.people_edit), { send(Event.EditPerson(person.id)) }, size = ButtonSize.Small)
            }
        }
    }
}

@Composable
private fun PersonEditScreen(screen: Screen.PersonEdit, send: (Event) -> Unit) {
    val draft = screen.draft
    ScreenFrame(
        title = stringResource(if (draft.id == null) R.string.person_new_title else R.string.person_edit_title),
        actions = {
            PleosButton(stringResource(R.string.action_back), { send(Event.Back) })
            PleosButton(stringResource(R.string.action_save), { send(Event.Submit) }, style = ButtonStyle.Primary)
        },
    ) {
        LabeledField(
            label = stringResource(R.string.person_name),
            required = true,
            value = draft.name,
            onValueChange = { send(Event.EditDraft(draft.copy(name = it))) },
            limit = NAME_LIMIT,
            error = if (Problem.NoName in screen.problems) Problem.NoName.message() else null,
            minLines = 1,
        )
        LabeledField(
            label = stringResource(R.string.person_places),
            required = false,
            value = draft.places,
            onValueChange = { send(Event.EditDraft(draft.copy(places = it))) },
            limit = TEXT_LIMIT,
            error = null,
            minLines = 1,
        )
        LabeledField(
            label = stringResource(R.string.person_interests),
            required = false,
            value = draft.interests,
            onValueChange = { send(Event.EditDraft(draft.copy(interests = it))) },
            limit = TEXT_LIMIT,
            error = null,
            minLines = 1,
        )
    }
}

@Composable
private fun CaptureScreen(
    title: String,
    note: String,
    field: String,
    capture: Capture,
    problems: Set<Problem>,
    back: String,
    submit: String,
    send: (Event) -> Unit,
) {
    val listening = capture.stt is Stt.Listening
    ScreenFrame(
        title = title,
        note = note,
        actions = {
            PleosButton(back, { send(Event.Back) })
            PleosButton(submit, { send(Event.Submit) }, style = ButtonStyle.Primary)
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            PleosButton(
                stringResource(if (listening) R.string.mic_stop else R.string.mic_start),
                { send(Event.Mic) },
                style = if (listening) ButtonStyle.Secondary else ButtonStyle.Primary,
                size = ButtonSize.Large,
            )
            when (val stt = capture.stt) {
                is Stt.Listening -> Text(
                    stringResource(R.string.mic_listening) + if (stt.partial.isNotEmpty()) " ${stt.partial}" else "",
                    style = PleosType.Body,
                    modifier = Modifier.weight(1f),
                )
                is Stt.Failed -> ErrorText(
                    stringResource(if (stt.reason == SttFailure.NoSpeech) R.string.stt_no_speech else R.string.stt_unavailable),
                    Modifier.weight(1f),
                )
                Stt.Idle -> Text(stringResource(R.string.mic_hint), style = PleosType.Caption, color = PleosColor.TextSecondary, modifier = Modifier.weight(1f))
            }
        }
        LabeledField(
            label = field,
            required = true,
            value = capture.text,
            onValueChange = { send(Event.EditText(it)) },
            limit = TEXT_LIMIT,
            error = if (Problem.NoText in problems) Problem.NoText.message() else null,
        )
    }
}

@Composable
private fun PickCompanionScreen(screen: Screen.PickCompanion, library: Library, send: (Event) -> Unit) {
    val others = library.people.filter { it.id != screen.me }
    ScreenFrame(
        title = stringResource(
            when (screen.purpose) {
                Purpose.SaveTopic -> R.string.companion_title_topic
                Purpose.Recommend -> R.string.companion_title_recommend
                Purpose.Reflect -> R.string.companion_title_reflect
            },
        ),
        note = stringResource(R.string.home_me, library.nameOf(screen.me)),
        actions = {
            PleosButton(stringResource(R.string.action_back), { send(Event.Back) })
            if (others.isNotEmpty()) PleosButton(stringResource(R.string.action_next), { send(Event.Submit) }, style = ButtonStyle.Primary)
            else PleosButton(stringResource(R.string.companion_make_profile), { send(Event.OpenPeople) }, style = ButtonStyle.Primary)
        },
    ) {
        if (others.isEmpty()) {
            Text(stringResource(R.string.companion_need_other), style = PleosType.Body, color = PleosColor.TextSecondary)
            return@ScreenFrame
        }
        Text(stringResource(R.string.companion_label), style = PleosType.Body)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            others.forEach { person ->
                ChoiceOption(person.name, screen.companion == person.id, { send(Event.ChooseCompanion(person.id)) }, Modifier.widthIn(min = 200.dp))
            }
        }
        Problems(screen.problems, Problem.NoCompanion)
        val riders = screen.companion?.let { Riders.of(screen.me, it) } ?: return@ScreenFrame
        val known = library.relation(riders)
        val name = library.nameOf(riders.companion)
        if (known != null && !screen.changingRelation) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.relation_known, name, known.label()), style = PleosType.Body, color = PleosColor.TextSecondary)
                PleosButton(stringResource(R.string.relation_change), { send(Event.ChangeRelation) }, size = ButtonSize.Small)
            }
            return@ScreenFrame
        }
        Text(stringResource(R.string.relation_question, name), style = PleosType.Body)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Relation.entries.forEach { relation ->
                ChoiceOption(relation.label(), (screen.relation ?: known) == relation, { send(Event.PickRelation(relation)) }, Modifier.widthIn(min = 200.dp))
            }
        }
        Problems(screen.problems, Problem.NoRelation)
    }
}

@Composable
private fun ChooseInputScreen(send: (Event) -> Unit) {
    ScreenFrame(
        title = stringResource(R.string.input_title),
        actions = { PleosButton(stringResource(R.string.action_back), { send(Event.Back) }) },
    ) {
        val kinds = listOf(
            Triple(R.string.input_topic, R.string.input_topic_body, InputKind.Topic),
            Triple(R.string.input_context, R.string.input_context_body, InputKind.Context),
        )
        Columns(kinds, perRow = 2) { (title, body, kind) ->
            SelectableCard(selected = false, onSelect = { send(Event.ChooseKind(kind)) }) {
                Text(stringResource(title), style = PleosType.Body)
                Text(stringResource(body), style = PleosType.Caption, color = PleosColor.TextSecondary)
            }
        }
    }
}

@Composable
private fun Approach.label(): String = stringResource(
    when (this) {
        Approach.Ask -> R.string.approach_ask
        Approach.ShareMine -> R.string.approach_share
        Approach.Celebrate -> R.string.approach_celebrate
        Approach.Listen -> R.string.approach_listen
    },
)

@Composable
private fun OpenersScreen(screen: Screen.Openers, send: (Event) -> Unit) {
    ScreenFrame(
        title = stringResource(R.string.openers_title),
        note = stringResource(R.string.openers_topic, screen.topic),
        actions = {
            PleosButton(stringResource(R.string.openers_other), { send(Event.Back) })
            PleosButton(stringResource(R.string.action_done), { send(Event.Done) }, style = ButtonStyle.Primary)
        },
    ) {
        Columns(screen.cards, perRow = 3) { OpenerCard(it) }
    }
}

@Composable
private fun OpenerCard(opener: Opener) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(PleosColor.BackgroundSecondary).padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Chip(opener.approach.label())
        Text(stringResource(R.string.openers_first), style = PleosType.Caption, color = PleosColor.TextTertiary)
        Text("“${opener.first}”", style = PleosType.Body)
        Text(stringResource(R.string.openers_follow), style = PleosType.Caption, color = PleosColor.TextTertiary)
        Text(opener.followUp, style = PleosType.Caption, color = PleosColor.TextSecondary)
    }
}

@Composable
private fun Reason.label(): String = when (this) {
    Reason.MatchesContext -> stringResource(R.string.reason_context)
    is Reason.SharesInterest -> stringResource(R.string.reason_interest, interest)
}

@Composable
private fun SavedTopicsScreen(screen: Screen.SavedTopics, send: (Event) -> Unit) {
    ScreenFrame(
        title = stringResource(R.string.saved_title),
        note = stringResource(R.string.saved_context, screen.context),
        actions = {
            PleosButton(stringResource(R.string.saved_again), { send(Event.Back) })
            PleosButton(stringResource(R.string.action_done), { send(Event.Done) }, style = ButtonStyle.Primary)
        },
    ) {
        if (screen.picks.isEmpty()) Text(stringResource(R.string.saved_empty), style = PleosType.Body, color = PleosColor.TextSecondary)
        Columns(screen.picks, perRow = 3) { pick ->
            SelectableCard(selected = false, onSelect = { send(Event.ChooseTopic(pick.topic.id)) }) {
                Text(pick.topic.text, style = PleosType.Body)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    pick.reasons.forEach { Chip(it.label()) }
                }
                Text(stringResource(R.string.saved_open), style = PleosType.Caption, color = PleosColor.FilledEnabled)
            }
        }
    }
}

@Composable
private fun ReflectScreen(screen: Screen.Reflect, library: Library, send: (Event) -> Unit) {
    val bond = library.bond(screen.riders.pair)
    val known = library.person(screen.riders.companion)?.interests.orEmpty()
    ScreenFrame(
        title = stringResource(R.string.reflect_title),
        note = stringResource(R.string.reflect_riders, library.namesOf(screen.riders), bond.count + 1),
        actions = {
            PleosButton(stringResource(R.string.action_back), { send(Event.Back) })
            PleosButton(stringResource(R.string.reflect_submit), { send(Event.Submit) }, style = ButtonStyle.Primary)
        },
    ) {
        Text(stringResource(R.string.reflect_why), style = PleosType.Caption, color = PleosColor.TextSecondary)
        Text(stringResource(R.string.reflect_flow), style = PleosType.Body)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(Flow.Smooth to R.string.flow_smooth, Flow.Okay to R.string.flow_okay, Flow.Awkward to R.string.flow_awkward).forEach { (flow, label) ->
                ChoiceOption(stringResource(label), screen.flow == flow, { send(Event.PickFlow(flow)) }, Modifier.widthIn(min = 260.dp))
            }
        }
        Problems(screen.problems, Problem.NoFlow)
        LabeledField(
            label = stringResource(R.string.reflect_discovered),
            required = false,
            value = screen.discovered,
            onValueChange = { send(Event.EditText(it)) },
            limit = TEXT_LIMIT,
            error = null,
            minLines = 1,
        )
        if (known.isNotEmpty()) {
            Text(stringResource(R.string.reflect_known, known.joinToString(", ")), style = PleosType.Caption, color = PleosColor.TextSecondary)
        }
    }
}

@Composable
private fun PersonSavedScreen(screen: Screen.PersonSaved, library: Library, send: (Event) -> Unit) {
    val name = library.nameOf(screen.person)
    ScreenFrame(
        title = stringResource(R.string.person_saved_title),
        note = stringResource(R.string.person_saved_note, name),
        actions = {
            PleosButton(stringResource(R.string.person_saved_list), { send(Event.Done) })
            PleosButton(stringResource(R.string.person_saved_topic), { send(Event.OpenSaveTopic) })
            PleosButton(stringResource(R.string.person_saved_recommend), { send(Event.OpenRecommend) }, style = ButtonStyle.Primary)
        },
    ) {
        val person = library.person(screen.person) ?: return@ScreenFrame
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(PleosColor.BackgroundSecondary).padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(person.name, style = PleosType.Body)
            if (person.places.isNotEmpty()) {
                Text(stringResource(R.string.people_places, person.places.joinToString(", ")), style = PleosType.Caption, color = PleosColor.TextSecondary)
            }
            if (person.interests.isNotEmpty()) {
                Text(stringResource(R.string.people_interests, person.interests.joinToString(", ")), style = PleosType.Caption, color = PleosColor.TextSecondary)
            }
        }
    }
}

@Composable
private fun ReflectedScreen(screen: Screen.Reflected, library: Library, send: (Event) -> Unit) {
    val companion = library.nameOf(screen.riders.companion)
    ScreenFrame(
        title = stringResource(R.string.reflected_title),
        note = stringResource(R.string.reflect_why),
        actions = { PleosButton(stringResource(R.string.action_done), { send(Event.Done) }, style = ButtonStyle.Primary) },
    ) {
        Text(
            stringResource(R.string.reflected_ride, companion, library.bond(screen.riders.pair).count),
            style = PleosType.Body,
        )
        if (screen.discovered.isNotEmpty()) {
            Text(stringResource(R.string.reflected_interests, screen.discovered.joinToString(", "), companion), style = PleosType.Body)
        }
    }
}

/** No touch targets and no motion: the driver should take it in at a glance. */
@Composable
private fun DrivingScreen(ui: Ui) {
    val ride = ui.ride
    Column(
        Modifier.fillMaxSize().background(PleosColor.BackgroundPrimary).safeDrawingPadding().padding(horizontal = 48.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            ui.board.situation?.label() ?: stringResource(R.string.driving_title),
            style = PleosType.Body,
            textAlign = TextAlign.Center,
            modifier = Modifier.clip(RoundedCornerShape(24.dp)).background(PleosColor.BasicEnabled).padding(horizontal = 32.dp, vertical = 12.dp),
        )
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val latest = ui.board.latest
            when {
                ride == null -> DrivingIdle(stringResource(R.string.driving_idle_off), stringResource(R.string.driving_voice_off))
                latest == null -> DrivingIdle(
                    stringResource(R.string.driving_idle_on),
                    stringResource(R.string.driving_voice_on, ui.library.nameOf(ride.riders.companion)),
                )
                latest is Suggestion.BalanceGame -> BalanceBoard(latest)
                latest is Suggestion.SavedTopic -> SuggestionBoard(stringResource(R.string.driving_saved_topic), latest.text)
                latest is Suggestion.Trend -> SuggestionBoard(stringResource(R.string.driving_trend), latest.text)
            }
        }
        Text(stringResource(R.string.driving_body), style = PleosType.Caption, color = PleosColor.TextSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Situation.label(): String {
    val parts = when (this) {
        is Situation.Highway -> listOfNotNull(
            stringResource(R.string.driving_highway),
            remainingKm?.let { stringResource(R.string.driving_remaining, it) },
        )
        is Situation.Jam -> listOfNotNull(
            stringResource(R.string.driving_jam),
            lengthM?.let {
                if (it >= 1000) stringResource(R.string.driving_length_km, it / 1000.0) else stringResource(R.string.driving_length_m, it)
            },
            avgSpeedKmh?.let { stringResource(R.string.driving_avg_speed, it) },
        )
    }
    return parts.joinToString(" · ")
}

@Composable
private fun DrivingIdle(title: String, body: String) {
    Column(Modifier.widthIn(max = 1400.dp), verticalArrangement = Arrangement.spacedBy(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = PleosType.Title, textAlign = TextAlign.Center)
        Text(body, style = PleosType.Body, color = PleosColor.TextSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SuggestionBoard(label: String, text: String) {
    Column(Modifier.widthIn(max = 1600.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = PleosType.Body, color = PleosColor.FilledEnabled)
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(PleosColor.BackgroundSecondary).padding(horizontal = 56.dp, vertical = 48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, style = PleosType.Title, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun BalanceBoard(game: Suggestion.BalanceGame) {
    Column(Modifier.widthIn(max = 1600.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.driving_balance), style = PleosType.Body, color = PleosColor.FilledEnabled)
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
            BalanceHalf(game.a, Modifier.weight(1f))
            Text(stringResource(R.string.driving_versus), style = PleosType.Title, color = PleosColor.TextSecondary)
            BalanceHalf(game.b, Modifier.weight(1f))
        }
    }
}

@Composable
private fun BalanceHalf(text: String, modifier: Modifier) {
    Box(
        modifier.fillMaxHeight().clip(RoundedCornerShape(32.dp)).background(PleosColor.BackgroundSecondary).padding(horizontal = 40.dp, vertical = 56.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = PleosType.Title, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
