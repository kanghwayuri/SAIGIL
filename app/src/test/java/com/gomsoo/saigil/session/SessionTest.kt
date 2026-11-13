package com.gomsoo.saigil.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTest {

    private fun Model.after(vararg events: Event): Model = events.fold(this) { model, event -> reduce(model, event) }

    private val parkedStart = Model().after(Event.VehicleChanged(Restriction.Parked))

    private val atCompose = parkedStart.after(
        Event.SelectRole(Role.Parent),
        Event.SetAlone(true),
        Event.SetParked(true),
        Event.Next,
    )

    private val story = Material.Story(
        scene = "hesitated in the meeting",
        feeling = "afraid to be wrong",
        choice = "",
        leftover = "speak earlier next time",
    )

    private val atEdit = atCompose.after(Event.EditMaterial(story), Event.Next)

    private val atGuideChoice = atEdit.after(Event.EditResult("I said one sentence at the end"), Event.Next)

    private val personalTexts = listOf(
        "hesitated in the meeting",
        "afraid to be wrong",
        "speak earlier next time",
        "I said one sentence at the end",
    )

    private fun assertHoldsNoPersonalText(model: Model) {
        val dump = model.toString()
        personalTexts.forEach { assertFalse("'$it' still reachable in $dump", dump.contains(it)) }
    }

    @Test
    fun mainPathEndsReadyWithChosenGuideAndNoContent() {
        val ready = atGuideChoice.after(Event.ChooseGuide(Guide.OnceGeneric), Event.Finish)

        assertEquals(Session.Ready(Guide.OnceGeneric), ready.session)
        assertHoldsNoPersonalText(ready)
    }

    @Test
    fun guideDefaultsToOff() {
        assertEquals(Session.Ready(Guide.Off), atGuideChoice.after(Event.Finish).session)
    }

    @Test
    fun startNeedsRoleBothConfirmationsAndParkedVehicle() {
        val blocked = Model().after(Event.Next)

        assertEquals(
            Session.Start(
                problems = setOf(
                    Problem.NoRole,
                    Problem.NotAlone,
                    Problem.NotParkedConfirmed,
                    Problem.VehicleNotParked,
                ),
            ),
            blocked.session,
        )
    }

    @Test
    fun startStaysWhileVehicleParkingIsNotKnown() {
        val stuck = Model().after(
            Event.SelectRole(Role.Child),
            Event.SetAlone(true),
            Event.SetParked(true),
            Event.Next,
        )

        assertEquals(setOf(Problem.VehicleNotParked), (stuck.session as Session.Start).problems)
        assertEquals(
            Session.Compose(Role.Child, Material.Story()),
            stuck.after(Event.VehicleChanged(Restriction.Parked), Event.Next).session,
        )
    }

    @Test
    fun startProblemsClearAsTheUserFixesThem() {
        val fixed = parkedStart.after(Event.Next, Event.SelectRole(Role.Parent), Event.SetAlone(true))

        assertEquals(setOf(Problem.NotParkedConfirmed), (fixed.session as Session.Start).problems)
    }

    @Test
    fun preparationEndsProtectedWithNothingLeftOnAnyInterruption() {
        val steps = mapOf(
            "W01" to parkedStart.after(Event.SelectRole(Role.Parent), Event.SetAlone(true)),
            "W02" to atCompose.after(Event.EditMaterial(story)),
            "W02B" to atCompose.after(
                Event.SwitchToInterest,
                Event.EditMaterial(Material.Interest("hesitated in the meeting", "afraid to be wrong")),
            ),
            "W03" to atEdit.after(Event.EditResult("I said one sentence at the end")),
            "W04" to atGuideChoice.after(Event.ChooseGuide(Guide.OnceGeneric)),
        )
        val interruptions = listOf(
            Event.Abort,
            Event.PageHidden,
            Event.VehicleChanged(Restriction.Restricted),
            Event.VehicleChanged(Restriction.Unknown),
        )
        steps.forEach { (step, model) ->
            interruptions.forEach { interruption ->
                val after = model.after(interruption)
                assertEquals("$step + $interruption", Session.Protected, after.session)
                assertHoldsNoPersonalText(after)
            }
        }
    }

    @Test
    fun nothingComesBackWhenTheVehicleIsParkedAgain() {
        val protected = atEdit.after(
            Event.VehicleChanged(Restriction.Restricted),
            Event.VehicleChanged(Restriction.Parked),
        )

        assertEquals(Session.Protected, protected.session)
        val restarted = protected.after(Event.Restart)
        assertEquals(Session.Start(), restarted.session)
        assertHoldsNoPersonalText(restarted)
    }

    @Test
    fun protectedRestartIsBlockedUnlessParked() {
        listOf(Restriction.Restricted, Restriction.Unknown).forEach { restriction ->
            val protected = atCompose.after(Event.VehicleChanged(restriction), Event.Restart)
            assertEquals(Session.Protected, protected.session)
        }
    }

    @Test
    fun switchingToInterestClearsTheStory() {
        val interest = atCompose.after(Event.EditMaterial(story), Event.SwitchToInterest)

        assertEquals(Session.Compose(Role.Parent, Material.Interest()), interest.session)
        assertHoldsNoPersonalText(interest)
    }

    @Test
    fun switchingBackToStoryClearsTheInterest() {
        val back = atCompose.after(
            Event.SwitchToInterest,
            Event.EditMaterial(Material.Interest("hesitated in the meeting", "afraid to be wrong")),
            Event.SwitchToStory,
        )

        assertEquals(Session.Compose(Role.Parent, Material.Story()), back.session)
        assertHoldsNoPersonalText(back)
    }

    @Test
    fun storyNextNeedsANonBlankScene() {
        val blocked = atCompose.after(Event.EditMaterial(Material.Story(scene = "   ", feeling = "x")), Event.Next)

        assertEquals(setOf(Problem.NoScene), (blocked.session as Session.Compose).problems)
        val fixed = blocked.after(Event.EditMaterial(Material.Story(scene = "a", feeling = "x")))
        assertEquals(emptySet<Problem>(), (fixed.session as Session.Compose).problems)
    }

    @Test
    fun interestNextNeedsTopicAndSource() {
        val interest = atCompose.after(Event.SwitchToInterest)

        assertEquals(
            setOf(Problem.NoTopic, Problem.NoSource),
            (interest.after(Event.Next).session as Session.Compose).problems,
        )
        assertEquals(
            setOf(Problem.NoSource),
            (interest.after(Event.EditMaterial(Material.Interest(topic = "basketball")), Event.Next).session as Session.Compose).problems,
        )
    }

    @Test
    fun editStartsFromTheUsersOwnStoryInOrder() {
        val edit = atEdit.session as Session.Edit

        assertEquals("hesitated in the meeting\nafraid to be wrong\nspeak earlier next time", edit.result)
    }

    @Test
    fun editStartsFromTheUsersOwnInterestInOrder() {
        val edit = atCompose.after(
            Event.SwitchToInterest,
            Event.EditMaterial(Material.Interest(topic = "basketball game", source = "they told me yesterday")),
            Event.Next,
        ).session as Session.Edit

        assertEquals("basketball game\nthey told me yesterday", edit.result)
    }

    @Test
    fun editNextNeedsANonBlankResult() {
        val blocked = atEdit.after(Event.EditResult(" \n "), Event.Next)

        assertEquals(setOf(Problem.BlankResult), (blocked.session as Session.Edit).problems)
    }

    @Test
    fun editCanShowTheOriginalMaterial() {
        val shown = atEdit.after(Event.ToggleSource).session as Session.Edit

        assertTrue(shown.showSource)
        assertEquals(story, shown.material)
    }

    @Test
    fun inputsStopAtTheirLimits() {
        val compose = atCompose.after(
            Event.EditMaterial(Material.Story(scene = "a".repeat(301), feeling = "b".repeat(301))),
        ).session as Session.Compose
        assertEquals(Material.Story(scene = "a".repeat(300), feeling = "b".repeat(300)), compose.material)

        val interest = atCompose.after(
            Event.SwitchToInterest,
            Event.EditMaterial(Material.Interest(topic = "t".repeat(201), source = "s".repeat(201))),
        ).session as Session.Compose
        assertEquals(Material.Interest(topic = "t".repeat(200), source = "s".repeat(200)), interest.material)

        val edit = atEdit.after(Event.EditResult("r".repeat(1501))).session as Session.Edit
        assertEquals("r".repeat(1500), edit.result)
    }

    @Test
    fun drivingAwayFromReadyWaitsForTheGuideOnlyWhenChosen() {
        val withGuide = atGuideChoice.after(Event.ChooseGuide(Guide.OnceGeneric), Event.Finish)
        val withoutGuide = atGuideChoice.after(Event.Finish)
        val driving = Event.VehicleChanged(Restriction.Restricted)

        assertEquals(Session.GuideWaiting, withGuide.after(driving).session)
        assertEquals(Session.Ended, withoutGuide.after(driving).session)
    }

    @Test
    fun guideWaitingEndsSilentlyWhenParkedOrSignalLost() {
        val waiting = atGuideChoice.after(
            Event.ChooseGuide(Guide.OnceGeneric),
            Event.Finish,
            Event.VehicleChanged(Restriction.Restricted),
        )

        assertEquals(Session.Ended, waiting.after(Event.VehicleChanged(Restriction.Parked)).session)
        assertEquals(Session.Ended, waiting.after(Event.VehicleChanged(Restriction.Unknown)).session)
        assertEquals(Session.GuideWaiting, waiting.after(Event.PageHidden).session)
    }

    @Test
    fun newPreparationAfterEndRequiresChoosingTheRoleAgain() {
        val ended = atGuideChoice.after(Event.Finish, Event.VehicleChanged(Restriction.Restricted))
        assertEquals(Session.Ended, ended.after(Event.Restart).session)

        val restarted = ended.after(Event.VehicleChanged(Restriction.Parked), Event.Restart)
        assertEquals(Session.Start(), restarted.session)
        assertEquals(
            setOf(Problem.NoRole, Problem.NotAlone, Problem.NotParkedConfirmed),
            (restarted.after(Event.Next).session as Session.Start).problems,
        )
    }
}
