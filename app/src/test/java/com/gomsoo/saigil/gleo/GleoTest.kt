package com.gomsoo.saigil.gleo

import com.gomsoo.saigil.drive.ActiveRide
import com.gomsoo.saigil.library.Library
import com.gomsoo.saigil.library.Person
import com.gomsoo.saigil.library.PersonId
import com.gomsoo.saigil.library.Relation
import com.gomsoo.saigil.library.Riders
import com.gomsoo.saigil.library.Topic
import com.gomsoo.saigil.library.TopicId
import com.gomsoo.saigil.screen.Stamp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GleoTest {

    private val me = Person(PersonId("me"), "지수", Relation.Me)
    private val mom = Person(PersonId("mom"), "엄마", Relation.Parent)
    private val sea = Topic(TopicId("sea"), "바다 여행 가고 싶다던 얘기", setOf(mom.id), addedAt = 1)
    private val library = Library(people = listOf(me, mom), topics = listOf(Topic(TopicId("x"), "새로 산 운동화", addedAt = 2), sea))
    private val ride = ActiveRide(Riders(me.id, mom.id))
    private val stamp = Stamp(now = 1000, id = "new")

    private fun dataOf(answer: Answer) = Json.parseToJsonElement(answer.response(Function.RECOMMEND_SAVED_TOPIC)["data"]!!.jsonPrimitive.content).jsonArray

    @Test
    fun `saved topics for a context come back as a data array, best first`() {
        val answer = answer(Function.RECOMMEND_SAVED_TOPIC, mapOf("context" to "바다 보러 가자"), library, ride, stamp)

        assertEquals(Status.Success, answer.status)
        assertEquals(sea.text, dataOf(answer).first().jsonObject["topic"]!!.jsonPrimitive.content)
    }

    @Test
    fun `a named companion works without an active ride, and openers use their register`() {
        val answer = answer(Function.RECOMMEND_OPENER, mapOf("topic" to "요가", "companion" to "엄마"), library, null, stamp)

        val lines = dataOf(answer).map { it.jsonObject["firstLine"]!!.jsonPrimitive.content }
        assertEquals(3, lines.size)
        assertTrue(lines.toString(), lines.all { "요가" in it && Regex("요[.?!]$").containsMatchIn(it) })
    }

    @Test
    fun `without a companion or an active ride Gleo is asked to find out who is riding`() {
        assertEquals(Status.Asking, answer(Function.RECOMMEND_SAVED_TOPIC, mapOf("context" to "심심해"), library, null, stamp).status)
    }

    @Test
    fun `an unknown companion fails and a missing required parameter asks`() {
        assertEquals(Status.Fail, answer(Function.RECOMMEND_OPENER, mapOf("topic" to "요가", "companion" to "철수"), library, ride, stamp).status)
        assertEquals(Status.Asking, answer(Function.SAVE_TOPIC, emptyMap(), library, ride, stamp).status)
    }

    @Test
    fun `a saved topic is about the active ride's passenger`() {
        val answer = answer(Function.SAVE_TOPIC, mapOf("topic" to "김장 계획"), library, ride, stamp)

        val saved = answer.write!!.applyTo(library).topics.last()
        assertEquals("김장 계획", saved.text)
        assertEquals(setOf(mom.id), saved.about)
        assertEquals("success", answer.response(Function.SAVE_TOPIC)["status"]!!.jsonPrimitive.content)
    }
}
