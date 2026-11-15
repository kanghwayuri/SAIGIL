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

    @Test
    fun `proactive goes saved topic, then trending, then balance game, never repeating in a ride`() {
        val library = people.copy(
            topics = listOf(
                topic("sea", "바다 여행 가고 싶다던 얘기"),
                topic("other", "엄마 요가 학원", withMom),
            ),
        )
        val said = mutableSetOf<String>()
        val spoken = mutableListOf<Suggestion>()
        while (true) {
            val next = proactive(Trigger.Jam, withFriend, library, said, now) ?: break
            assertFalse("repeated ${next.key}", next.key in said)
            said += next.key
            spoken += next
        }

        assertEquals("topic:sea", spoken.first().key)
        assertTrue("바다 여행" in spoken.first().sentence)
        assertFalse("another pair's topic is never spoken", spoken.any { it.key == "topic:other" })
        assertEquals(listOf("topic", "trend", "balance"), spoken.map { it.key.substringBefore(':') }.distinct())
        assertNull(proactive(Trigger.Jam, withFriend, library, said, now))
    }

    @Test
    fun `the trigger sets the first words`() {
        assertTrue(proactive(Trigger.LongDrive, withFriend, people, emptySet(), now)!!.sentence.startsWith("한동안 길게"))
        assertTrue(proactive(Trigger.Jam, withFriend, people, emptySet(), now)!!.sentence.startsWith("길이 막히네요"))
    }
}
