package com.gomsoo.saigil.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MethodsTest {

    private fun methods(purpose: Purpose?, known: Known? = null, profile: Profile = Profile()) =
        recommend(purpose, known, profile).map { it.method }

    private fun sentences(text: String) = text.split('.', '?', '!').map { it.trim() }.filter { it.isNotEmpty() }

    @Test
    fun everySituationGivesThreeDistinctApproaches() {
        for (purpose in Purpose.entries + null)
            for (known in Known.entries + null)
                for (style in Style.entries + null)
                    for (interest in Interest.entries + null) {
                        val approaches = methods(purpose, known, Profile(style, interest)).map { it.approach }
                        val input = "$purpose $known $style $interest"
                        assertEquals(input, 3, approaches.size)
                        assertEquals(input, 3, approaches.toSet().size)
                        assertEquals(input, approaches.sortedBy { it.ordinal }, approaches)
                    }
    }

    @Test
    fun proposalSceneGivesCheerAskListen() {
        assertEquals(
            listOf(Method.CheerFirst, Method.AskOneMore, Method.ListenBack),
            methods(Purpose.Celebrate, Known.GoodNews),
        )
    }

    @Test
    fun calmCompanionGetsListenFirstForAWorry() {
        assertTrue(Method.ListenFirst in methods(Purpose.Listen, profile = Profile(style = Style.Calm)))
        assertTrue(Method.ListenBack in methods(Purpose.Listen))
    }

    @Test
    fun knownInterestUnlocksAskInterest() {
        assertTrue(Method.AskInterest in methods(Purpose.Light, profile = Profile(interest = Interest.Music)))
        assertTrue(Method.AskEasy in methods(Purpose.Light))
    }

    @Test
    fun skippedPurposeGivesTheLightStartCards() {
        assertEquals(methods(Purpose.Light), methods(null))
    }

    @Test
    fun askOneMoreNeedsAKnownTopic() {
        assertEquals(listOf(Method.CheerFirst, Method.AskEasy, Method.ListenBack), methods(Purpose.Celebrate))
    }

    @Test
    fun reasonsNameOnlyWhatTheUserChose() {
        val chosen = recommend(Purpose.Celebrate, Known.GoodNews, Profile()).first { it.method == Method.AskOneMore }
        assertEquals(listOf("목적 · 좋은 일 함께 기뻐하기", "소재 · 좋은 소식"), chosen.reasons)

        val skipped = recommend(null, null, Profile(style = Style.Calm))
        assertTrue(skipped.flatMap { it.reasons }.none { it.startsWith("목적") })
        assertEquals(listOf("평소 모습 · 차분"), skipped.first { it.method == Method.ListenQuiet }.reasons)
    }

    @Test
    fun everyCardEndsWithRespect() {
        Method.entries.forEach { assertEquals(it.name, RESPECT, it.respond.last()) }
    }

    @Test
    fun childSpeaksPolitelyAndOtherRolesSpeakCasually() {
        Method.entries.forEach { method ->
            val polite = method.firstWords(Role.Child)
            sentences(polite).forEach { assertTrue("${method.name}: $it", it.endsWith("요")) }
            Role.entries.filter { it != Role.Child }.forEach { role ->
                val casual = method.firstWords(role)
                assertNotEquals(method.name, polite, casual)
                sentences(casual).forEach { assertFalse("${method.name}: $it", it.endsWith("요")) }
            }
        }
    }

    @Test
    fun cardTextFitsTwoLines() {
        Method.entries.forEach { method ->
            val texts = listOf(method.title, method.why, method.start) + method.respond +
                Role.entries.map { method.firstWords(it) }
            texts.forEach {
                assertTrue("${method.name}: blank", it.isNotBlank())
                assertTrue("${method.name}: $it", it.length <= 60)
            }
        }
    }

    @Test
    fun cardTextAvoidsPressure() {
        Method.entries.forEach { method ->
            val texts = listOf(method.title, method.why, method.start) + method.respond +
                Role.entries.map { method.firstWords(it) }
            texts.forEach { text ->
                listOf("잘해야", "마음을 열").forEach { assertFalse("${method.name}: $text", text.contains(it)) }
            }
        }
    }

    @Test
    fun profileOfIgnoresUnsureAndUnknownLabels() {
        assertEquals(
            Profile(Style.Calm, Interest.Music),
            profileOf(mapOf(StartField.CompanionStyle to "차분", StartField.CompanionInterest to "음악")),
        )
        assertEquals(Profile(), profileOf(mapOf(StartField.CompanionStyle to UNSURE, StartField.CompanionInterest to UNSURE)))
        assertEquals(Profile(), profileOf(mapOf(StartField.CompanionStyle to "", StartField.CompanionInterest to "독서")))
        assertEquals(Profile(), profileOf(emptyMap()))
    }
}
