package com.gomsoo.saigil.recommend

import com.gomsoo.saigil.library.Flow
import com.gomsoo.saigil.library.Library
import com.gomsoo.saigil.library.Person
import com.gomsoo.saigil.library.PersonId
import com.gomsoo.saigil.library.Relation
import com.gomsoo.saigil.library.Ride
import com.gomsoo.saigil.library.Riders
import com.gomsoo.saigil.library.Topic
import com.gomsoo.saigil.library.TopicId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendTest {

    private val day = 24L * 60 * 60 * 1000
    private val now = 100 * day
    private val me = Person(PersonId("me"), "지수")
    private val mom = Person(PersonId("mom"), "엄마", interests = listOf("요가"))
    private val friend = Person(PersonId("f"), "민호")
    private val withMom = Riders.of(me.id, mom.id)!!
    private val withFriend = Riders.of(me.id, friend.id)!!
    private val people = Library(people = listOf(me, mom, friend))
        .relate(withMom, Relation.Parent)
        .relate(withFriend, Relation.Friend)

    private fun topic(id: String, text: String, riders: Riders = withFriend, addedAt: Long = 0, usedAt: Long? = null) =
        Topic(TopicId(id), text, riders.pair, addedAt, usedAt)

    private val formalEnding = Regex("요[.?!]$")

    @Test
    fun `openers give three different approaches that each name the topic`() {
        val cards = openers("바다 여행", withFriend, people)

        assertEquals(3, cards.size)
        assertEquals(3, cards.map { it.approach }.toSet().size)
        cards.forEach { assertTrue(it.first, "바다 여행" in it.first) }
    }

    @Test
    fun `a parent is spoken to formally, and that parent speaks back to their child casually`() {
        openers("바다 여행", withMom, people).forEach { assertTrue(it.first, formalEnding.containsMatchIn(it.first)) }
        openers("바다 여행", Riders.of(mom.id, me.id)!!, people).forEach { assertFalse(it.first, formalEnding.containsMatchIn(it.first)) }
        openers("바다 여행", withFriend, people).forEach { assertFalse(it.first, formalEnding.containsMatchIn(it.first)) }
    }

    @Test
    fun `after an awkward ride the first opener is a light question`() {
        val library = people.record(Ride(friend.id, me.id, at = 1, flow = Flow.Awkward))

        assertEquals(Approach.Ask, openers("주말 계획", withFriend, library).first().approach)
    }

    @Test
    fun `a context about the sea ranks the saved sea trip first`() {
        val library = people.copy(
            topics = listOf(
                topic("shoes", "새로 산 운동화", addedAt = 3),
                topic("sea", "바다 여행 가고 싶다던 얘기", addedAt = 1),
                topic("move", "회사 이사 소식", addedAt = 2),
            ),
        )

        val picks = topicsFor("주말에 바다 보러 갈까 했어", withFriend, library, now)

        assertEquals("sea", picks.first().topic.id.value)
        assertTrue(Reason.MatchesContext in picks.first().reasons)
        assertEquals(3, picks.size)
    }

    @Test
    fun `a topic saved by one person is found when the other is the user, and another pair's never is`() {
        val library = people.copy(
            topics = listOf(
                topic("ours", "가을 캠핑 계획", Riders.of(friend.id, me.id)!!),
                topic("theirs", "캠핑 장비 고르기", Riders.of(mom.id, friend.id)!!),
                topic("mine-with-mom", "캠핑 사진 정리", withMom),
            ),
        )

        assertEquals(listOf("ours"), topicsFor("캠핑 가고 싶다", withFriend, library, now).map { it.topic.id.value })
        assertEquals(listOf("mine-with-mom"), topicsFor("캠핑 가고 싶다", withMom, library, now).map { it.topic.id.value })
    }

    @Test
    fun `a recently used topic drops below an unused one`() {
        val library = people.copy(
            topics = listOf(
                topic("used", "캠핑 장비 고르기", addedAt = 2, usedAt = now - day),
                topic("plain", "가을 캠핑", addedAt = 1),
            ),
        )

        assertEquals(listOf("plain", "used"), topicsFor("캠핑 가고 싶다", withFriend, library, now).map { it.topic.id.value })
    }

    @Test
    fun `a topic that matches the companion's interest says so`() {
        val library = people.copy(topics = listOf(topic("yoga", "요가 학원 등록한 얘기", withMom)))

        val pick = topicsFor("오늘 날씨 좋다", withMom, library, now).single()

        assertTrue(pick.reasons.toString(), Reason.SharesInterest("요가") in pick.reasons)
    }

    private fun everything(situation: Situation, library: Library): List<Suggestion> {
        val said = mutableSetOf<String>()
        val spoken = mutableListOf<Suggestion>()
        while (true) {
            val next = proactive(situation, withFriend, library, said, now) ?: break
            assertFalse("repeated ${next.key}", next.key in said)
            said += next.key
            spoken += next
        }
        assertNull(proactive(situation, withFriend, library, said, now))
        return spoken
    }

    private val twoPairs = people.copy(
        topics = listOf(topic("sea", "바다 여행 가고 싶다던 얘기"), topic("other", "엄마 요가 학원", withMom)),
    )

    @Test
    fun `on a highway proactive goes saved topic, then trend, then balance game, never repeating in a ride`() {
        val spoken = everything(Situation.Highway(42), twoPairs)

        assertEquals(Suggestion.SavedTopic(TopicId("sea"), "바다 여행 가고 싶다던 얘기"), spoken.first())
        assertFalse("another pair's topic is never spoken", spoken.any { it is Suggestion.SavedTopic && it.id == TopicId("other") })
        assertEquals(
            listOf(Suggestion.SavedTopic::class, Suggestion.Trend::class, Suggestion.BalanceGame::class),
            spoken.map { it::class }.distinct(),
        )
    }

    @Test
    fun `in a jam proactive starts with a balance game, then saved topics, then trends`() {
        val spoken = everything(Situation.Jam(2300, 12), twoPairs)

        assertTrue(spoken.first() is Suggestion.BalanceGame)
        assertEquals(
            listOf(Suggestion.BalanceGame::class, Suggestion.SavedTopic::class, Suggestion.Trend::class),
            spoken.map { it::class }.distinct(),
        )
    }

    @Test
    fun `each kind of suggestion says what it suggests`() {
        val topic = Suggestion.SavedTopic(TopicId("sea"), "바다 여행 가고 싶다던 얘기").sentence
        val trend = Suggestion.Trend("요즘 자주 듣는 노래를 하나씩 말해 봐요.").sentence
        val game = Suggestion.BalanceGame("짜장면", "짬뽕").sentence

        assertTrue(topic, "저장해 둔 화제" in topic && "바다 여행 가고 싶다던 얘기" in topic)
        assertTrue(trend, "요즘 자주 듣는 노래를 하나씩 말해 봐요." in trend)
        assertTrue(game, "밸런스 게임" in game && "짜장면" in game && "짬뽕" in game)
    }
}
