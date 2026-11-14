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
    private val me = Person(PersonId("me"), "지수", Relation.Me)
    private val mom = Person(PersonId("mom"), "엄마", Relation.Parent, interests = listOf("요가"))
    private val friend = Person(PersonId("f"), "민호", Relation.Friend)
    private val withMom = Riders(me.id, mom.id)
    private val withFriend = Riders(me.id, friend.id)
    private val people = Library(people = listOf(me, mom, friend))

    private fun topic(id: String, text: String, about: Set<PersonId> = emptySet(), addedAt: Long = 0, usedAt: Long? = null) =
        Topic(TopicId(id), text, about, addedAt, usedAt)

    private val formalEnding = Regex("요[.?!]$")

    @Test
    fun `openers give three different approaches that each name the topic`() {
        val cards = openers("바다 여행", withFriend, people)

        assertEquals(3, cards.size)
        assertEquals(3, cards.map { it.approach }.toSet().size)
        cards.forEach { assertTrue(it.first, "바다 여행" in it.first) }
    }

    @Test
    fun `a parent passenger is spoken to formally and a friend casually`() {
        openers("바다 여행", withMom, people).forEach { assertTrue(it.first, formalEnding.containsMatchIn(it.first)) }
        openers("바다 여행", withFriend, people).forEach { assertFalse(it.first, formalEnding.containsMatchIn(it.first)) }
    }

    @Test
    fun `after an awkward ride the first opener is a light question`() {
        val library = people.record(Ride(me.id, friend.id, at = 1, flow = Flow.Awkward))

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
    fun `a topic about the passenger beats an equal one, and a recently used one drops`() {
        val library = people.copy(
            topics = listOf(
                topic("plain", "가을 캠핑", addedAt = 3),
                topic("used", "캠핑 장비 고르기", addedAt = 2, usedAt = now - day),
                topic("mine", "캠핑 사진 정리", about = setOf(friend.id), addedAt = 1),
            ),
        )

        val ids = topicsFor("캠핑 가고 싶다", withFriend, library, now).map { it.topic.id.value }

        assertEquals(listOf("mine", "plain", "used"), ids)
    }

    @Test
    fun `a topic that matches a passenger interest says so`() {
        val library = people.copy(topics = listOf(topic("yoga", "요가 학원 등록한 얘기")))

        val pick = topicsFor("오늘 날씨 좋다", withMom, library, now).single()

        assertTrue(pick.reasons.toString(), Reason.SharesInterest("요가") in pick.reasons)
    }

    @Test
    fun `proactive goes saved topic, then trending, then balance game, never repeating in a ride`() {
        val library = people.copy(
            topics = listOf(
                topic("sea", "바다 여행 가고 싶다던 얘기", about = setOf(friend.id)),
                topic("other", "엄마 요가 학원", about = setOf(mom.id)),
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
        assertFalse("a topic about someone else is never spoken", spoken.any { it.key == "topic:other" })
        assertEquals(listOf("topic", "trend", "balance"), spoken.map { it.key.substringBefore(':') }.distinct())
        assertNull(proactive(Trigger.Jam, withFriend, library, said, now))
    }

    @Test
    fun `the trigger sets the first words`() {
        assertTrue(proactive(Trigger.LongDrive, withFriend, people, emptySet(), now)!!.sentence.startsWith("한동안 길게"))
        assertTrue(proactive(Trigger.Jam, withFriend, people, emptySet(), now)!!.sentence.startsWith("길이 막히네요"))
    }
}
