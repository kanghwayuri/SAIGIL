package com.gomsoo.saigil.gleo

import com.gomsoo.saigil.drive.ActiveRide
import com.gomsoo.saigil.library.Library
import com.gomsoo.saigil.library.Pair
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

    private val me = Person(PersonId("me"), "지수")
    private val mom = Person(PersonId("mom"), "엄마")
    private val friend = Person(PersonId("f"), "민호")
    private val withMom = Riders.of(me.id, mom.id)!!
    private val sea = Topic(TopicId("sea"), "바다 여행 가고 싶다던 얘기", withMom.pair, addedAt = 1)
    private val library = Library(
        people = listOf(me, mom, friend),
        topics = listOf(Topic(TopicId("x"), "바다 사진 정리", Pair.of(mom.id, friend.id)!!, addedAt = 2), sea),
    ).relate(withMom, Relation.Parent)
    private val ride = ActiveRide(withMom)
    private val stamp = Stamp(now = 1000, id = "new")

    private fun dataOf(answer: Answer) = Json.parseToJsonElement(answer.response(Function.RECOMMEND_SAVED_TOPIC)["data"]!!.jsonPrimitive.content).jsonArray

    @Test
    fun `saved topics for a context are the pair's own, best first`() {
        val answer = answer(Function.RECOMMEND_SAVED_TOPIC, mapOf("context" to "바다 보러 가자"), library, me.id, ride, stamp)

        assertEquals(Status.Success, answer.status)
        assertEquals(listOf(sea.text), dataOf(answer).map { it.jsonObject["topic"]!!.jsonPrimitive.content })
    }

    @Test
    fun `a named companion works without an active ride, and openers use the pair's register`() {
        val answer = answer(Function.RECOMMEND_OPENER, mapOf("topic" to "요가", "companion" to "엄마"), library, me.id, null, stamp)

        val lines = dataOf(answer).map { it.jsonObject["firstLine"]!!.jsonPrimitive.content }
        assertEquals(3, lines.size)
        assertTrue(lines.toString(), lines.all { "요가" in it && Regex("요[.?!]$").containsMatchIn(it) })
    }

    @Test
    fun `Gleo is asked to find out who is using SAIGIL and who is riding when either is unknown`() {
        assertEquals(Status.Asking, answer(Function.RECOMMEND_SAVED_TOPIC, mapOf("context" to "심심해", "companion" to "엄마"), library, null, ride, stamp).status)
        assertEquals(Status.Asking, answer(Function.RECOMMEND_SAVED_TOPIC, mapOf("context" to "심심해"), library, me.id, null, stamp).status)
        assertEquals(Status.Asking, answer(Function.RECOMMEND_SAVED_TOPIC, mapOf("context" to "심심해"), library, friend.id, ride, stamp).status)
    }

    @Test
    fun `an unknown companion fails and a missing required parameter asks`() {
        assertEquals(Status.Fail, answer(Function.RECOMMEND_OPENER, mapOf("topic" to "요가", "companion" to "철수"), library, me.id, ride, stamp).status)
        assertEquals(Status.Asking, answer(Function.SAVE_TOPIC, emptyMap(), library, me.id, ride, stamp).status)
    }

    @Test
    fun `a saved topic belongs to the user and the active ride's companion`() {
        val answer = answer(Function.SAVE_TOPIC, mapOf("topic" to "김장 계획"), library, me.id, ride, stamp)

        val saved = answer.write!!.applyTo(library).topics.last()
        assertEquals("김장 계획", saved.text)
        assertEquals(Pair.of(mom.id, me.id), saved.pair)
        assertEquals("success", answer.response(Function.SAVE_TOPIC)["status"]!!.jsonPrimitive.content)
    }
}
