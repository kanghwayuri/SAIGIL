package com.gomsoo.saigil.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.unit.dp
import com.gomsoo.saigil.R
import com.gomsoo.saigil.session.Event
import com.gomsoo.saigil.session.Guide
import com.gomsoo.saigil.session.Limit
import com.gomsoo.saigil.session.Material
import com.gomsoo.saigil.session.Model
import com.gomsoo.saigil.session.Problem
import com.gomsoo.saigil.session.Restriction
import com.gomsoo.saigil.session.Role
import com.gomsoo.saigil.session.Session
import com.gomsoo.saigil.ui.theme.PleosColor
import com.gomsoo.saigil.ui.theme.PleosType

@Composable
fun SaigilApp(model: Model, send: (Event) -> Unit, close: () -> Unit) {
    val parked = model.vehicle == Restriction.Parked
    when (val session = model.session) {
        is Session.Start -> StartScreen(session, send)
        is Session.Compose -> when (val material = session.material) {
            is Material.Story -> StoryScreen(material, session.problems, send)
            is Material.Interest -> InterestScreen(material, session.problems, send)
        }
        is Session.Edit -> EditScreen(session, send)
        is Session.GuideChoice -> GuideScreen(session.guide, send)
        is Session.Ready -> NoticeScreen(
            title = stringResource(R.string.ready_title),
            body = stringResource(if (session.guide == Guide.OnceGeneric) R.string.ready_guide_once else R.string.ready_guide_off),
        ) {
            PleosButton(stringResource(R.string.ready_close), close, style = ButtonStyle.Primary)
        }
        Session.GuideWaiting -> NoticeScreen(
            title = stringResource(R.string.waiting_title),
            body = stringResource(R.string.waiting_body),
        )
        Session.Ended -> RestartScreen(R.string.ended_title, R.string.ended_body, R.string.ended_restart, parked, send)
        Session.Protected -> RestartScreen(R.string.protected_title, R.string.protected_body, R.string.protected_restart, parked, send)
    }
}

@Composable
private fun ScreenFrame(
    title: String,
    actions: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().background(PleosColor.BackgroundPrimary).safeDrawingPadding()) {
        Box(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier.widthIn(max = 1600.dp).fillMaxWidth().padding(horizontal = 48.dp, vertical = 40.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                Text(title, style = PleosType.TitleLarge, modifier = Modifier.semantics { heading() })
                content()
            }
        }
        if (actions != null) {
            HorizontalDivider(color = PleosColor.Divider)
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) { actions() }
        }
    }
}

/** Side by side on a wide window, stacked on a narrow one. */
@Composable
private fun Panes(first: @Composable ColumnScope.() -> Unit, second: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints {
        if (maxWidth >= 1000.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(24.dp), content = first)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(24.dp), content = second)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                first()
                second()
            }
        }
    }
}

@Composable
private fun Problem.message(): String = stringResource(
    when (this) {
        Problem.NoRole -> R.string.problem_no_role
        Problem.NotAlone -> R.string.problem_not_alone
        Problem.NotParkedConfirmed -> R.string.problem_not_parked_confirmed
        Problem.VehicleNotParked -> R.string.problem_vehicle_not_parked
        Problem.NoScene -> R.string.problem_no_scene
        Problem.NoTopic -> R.string.problem_no_topic
        Problem.NoSource -> R.string.problem_no_source
        Problem.BlankResult -> R.string.problem_blank_result
    },
)

@Composable
private fun Set<Problem>.messageFor(problem: Problem): String? = if (problem in this) problem.message() else null

@Composable
private fun StartScreen(start: Session.Start, send: (Event) -> Unit) {
    ScreenFrame(
        title = stringResource(R.string.start_title),
        actions = {
            PleosButton(stringResource(R.string.start_quit), { send(Event.Abort) })
            PleosButton(stringResource(R.string.action_next), { send(Event.Next) }, style = ButtonStyle.Primary)
        },
    ) {
        FlowRow(
            Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            listOf(Role.Parent to R.string.role_parent, Role.Child to R.string.role_child).forEach { (role, label) ->
                ChoiceOption(
                    text = stringResource(label),
                    selected = start.role == role,
                    onSelect = { send(Event.SelectRole(role)) },
                    modifier = Modifier.widthIn(min = 360.dp),
                )
            }
        }
        start.problems.messageFor(Problem.NoRole)?.let { ErrorText(it) }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.start_conditions), style = PleosType.BodySmall, color = PleosColor.TextSecondary)
            CheckOption(stringResource(R.string.start_alone), start.alone, { send(Event.SetAlone(it)) })
            start.problems.messageFor(Problem.NotAlone)?.let { ErrorText(it) }
            CheckOption(stringResource(R.string.start_parked), start.parked, { send(Event.SetParked(it)) })
            start.problems.messageFor(Problem.NotParkedConfirmed)?.let { ErrorText(it) }
        }
        start.problems.messageFor(Problem.VehicleNotParked)?.let { ErrorText(it) }
    }
}

@Composable
private fun StoryScreen(story: Material.Story, problems: Set<Problem>, send: (Event) -> Unit) {
    ScreenFrame(
        title = stringResource(R.string.story_title),
        actions = {
            PleosButton(stringResource(R.string.action_abort), { send(Event.Abort) })
            PleosButton(stringResource(R.string.story_to_interest), { send(Event.SwitchToInterest) })
            PleosButton(stringResource(R.string.action_next), { send(Event.Next) }, style = ButtonStyle.Primary)
        },
    ) {
        Panes(
            first = {
                LabeledField(
                    label = stringResource(R.string.story_scene),
                    required = true,
                    value = story.scene,
                    onValueChange = { send(Event.EditMaterial(story.copy(scene = it))) },
                    limit = Limit.SCENE,
                    error = problems.messageFor(Problem.NoScene),
                    minLines = 3,
                )
                LabeledField(
                    label = stringResource(R.string.story_feeling),
                    required = false,
                    value = story.feeling,
                    onValueChange = { send(Event.EditMaterial(story.copy(feeling = it))) },
                    limit = Limit.STORY_OPTIONAL,
                    error = null,
                )
            },
            second = {
                LabeledField(
                    label = stringResource(R.string.story_choice),
                    required = false,
                    value = story.choice,
                    onValueChange = { send(Event.EditMaterial(story.copy(choice = it))) },
                    limit = Limit.STORY_OPTIONAL,
                    error = null,
                )
                LabeledField(
                    label = stringResource(R.string.story_leftover),
                    required = false,
                    value = story.leftover,
                    onValueChange = { send(Event.EditMaterial(story.copy(leftover = it))) },
                    limit = Limit.STORY_OPTIONAL,
                    error = null,
                )
            },
        )
    }
}

@Composable
private fun InterestScreen(interest: Material.Interest, problems: Set<Problem>, send: (Event) -> Unit) {
    ScreenFrame(
        title = stringResource(R.string.interest_title),
        actions = {
            PleosButton(stringResource(R.string.action_abort), { send(Event.Abort) })
            PleosButton(stringResource(R.string.interest_to_story), { send(Event.SwitchToStory) })
            PleosButton(stringResource(R.string.action_next), { send(Event.Next) }, style = ButtonStyle.Primary)
        },
    ) {
        Text(stringResource(R.string.interest_note), style = PleosType.BodySmall, color = PleosColor.TextSecondary)
        Panes(
            first = {
                LabeledField(
                    label = stringResource(R.string.interest_topic),
                    required = true,
                    value = interest.topic,
                    onValueChange = { send(Event.EditMaterial(interest.copy(topic = it))) },
                    limit = Limit.INTEREST,
                    error = problems.messageFor(Problem.NoTopic),
                )
            },
            second = {
                LabeledField(
                    label = stringResource(R.string.interest_source),
                    required = true,
                    value = interest.source,
                    onValueChange = { send(Event.EditMaterial(interest.copy(source = it))) },
                    limit = Limit.INTEREST,
                    error = problems.messageFor(Problem.NoSource),
                )
            },
        )
    }
}

@Composable
private fun EditScreen(edit: Session.Edit, send: (Event) -> Unit) {
    ScreenFrame(
        title = stringResource(R.string.edit_title),
        actions = {
            PleosButton(stringResource(R.string.action_abort), { send(Event.Abort) })
            PleosButton(
                stringResource(if (edit.showSource) R.string.edit_hide_source else R.string.edit_show_source),
                { send(Event.ToggleSource) },
            )
            PleosButton(stringResource(R.string.action_next), { send(Event.Next) }, style = ButtonStyle.Primary)
        },
    ) {
        val result: @Composable ColumnScope.() -> Unit = {
            LabeledField(
                label = stringResource(R.string.edit_result),
                required = true,
                value = edit.result,
                onValueChange = { send(Event.EditResult(it)) },
                limit = Limit.RESULT,
                error = edit.problems.messageFor(Problem.BlankResult),
                minLines = 6,
            )
        }
        if (edit.showSource) Panes(first = result, second = { SourcePanel(edit.material) }) else result()
    }
}

@Composable
private fun SourcePanel(material: Material) {
    val rows = when (material) {
        is Material.Story -> listOf(
            R.string.story_scene to material.scene,
            R.string.story_feeling to material.feeling,
            R.string.story_choice to material.choice,
            R.string.story_leftover to material.leftover,
        )
        is Material.Interest -> listOf(
            R.string.interest_topic to material.topic,
            R.string.interest_source to material.source,
        )
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(PleosColor.BasicEnabled).padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.edit_source_title), style = PleosType.TitleSmall)
        rows.filter { it.second.isNotBlank() }.forEach { (label, text) ->
            Column {
                Text(stringResource(label), style = PleosType.LabelMedium, color = PleosColor.TextTertiary)
                Text(text, style = PleosType.BodyMedium)
            }
        }
    }
}

@Composable
private fun GuideScreen(guide: Guide, send: (Event) -> Unit) {
    ScreenFrame(
        title = stringResource(R.string.guide_title),
        actions = {
            PleosButton(stringResource(R.string.guide_finish), { send(Event.Finish) }, style = ButtonStyle.Primary)
        },
    ) {
        FlowRow(
            Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            listOf(Guide.Off to R.string.guide_off, Guide.OnceGeneric to R.string.guide_once).forEach { (option, label) ->
                ChoiceOption(
                    text = stringResource(label),
                    selected = guide == option,
                    onSelect = { send(Event.ChooseGuide(option)) },
                    modifier = Modifier.widthIn(min = 360.dp),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.guide_example_label), style = PleosType.LabelMedium, color = PleosColor.TextTertiary)
            Text(stringResource(R.string.guide_example), style = PleosType.BodyMedium, color = PleosColor.TextSecondary)
        }
    }
}

@Composable
private fun NoticeScreen(title: String, body: String, actions: (@Composable () -> Unit)? = null) {
    ScreenFrame(title = title, actions = actions) {
        Text(body, style = PleosType.BodyMedium, color = PleosColor.TextSecondary)
    }
}

@Composable
private fun RestartScreen(title: Int, body: Int, restart: Int, parked: Boolean, send: (Event) -> Unit) {
    ScreenFrame(
        title = stringResource(title),
        actions = {
            PleosButton(stringResource(restart), { send(Event.Restart) }, style = ButtonStyle.Primary, enabled = parked)
        },
    ) {
        Text(stringResource(body), style = PleosType.BodyMedium, color = PleosColor.TextSecondary)
        if (!parked) Text(stringResource(R.string.restart_needs_parked), style = PleosType.BodySmall)
    }
}
