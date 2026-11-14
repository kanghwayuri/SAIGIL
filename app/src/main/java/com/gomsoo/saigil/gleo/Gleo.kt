package com.gomsoo.saigil.gleo

import com.gomsoo.saigil.drive.ActiveRide
import com.gomsoo.saigil.library.Library
import com.gomsoo.saigil.library.Person
import com.gomsoo.saigil.library.Relation
import com.gomsoo.saigil.library.Riders
import com.gomsoo.saigil.library.Topic
import com.gomsoo.saigil.library.TopicId
import com.gomsoo.saigil.recommend.Reason
import com.gomsoo.saigil.recommend.openers
import com.gomsoo.saigil.recommend.topicsFor
import com.gomsoo.saigil.screen.Effect
import com.gomsoo.saigil.screen.Stamp
import com.gomsoo.saigil.screen.TEXT_LIMIT
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/** The schema.json function names. They arrive as the intent action. */
enum class Function { RECOMMEND_OPENER, RECOMMEND_SAVED_TOPIC, SAVE_TOPIC }

enum class Status(val wire: String) { Success("success"), Asking("asking"), Fail("fail") }

/** What the app tells Gleo. Gleo words the final answer from [data]; [write] is applied before answering. */
data class Answer(val status: Status, val message: String, val data: JsonArray? = null, val write: Effect.Write? = null)

fun answer(function: Function, params: Map<String, String>, library: Library, ride: ActiveRide?, stamp: Stamp): Answer {
    val companionName = params["companion"]
    val named = companionName?.let { library.personNamed(it) ?: return Answer(Status.Fail, "There is no SAIGIL profile named $it.") }
    val passenger = named ?: ride?.riders?.passenger?.let(library::person)
    return when (function) {
        Function.RECOMMEND_OPENER -> {
            val topic = params["topic"] ?: return Answer(Status.Asking, "What topic would you like to talk about?")
            val riders = riders(passenger, ride, library) ?: return askCompanion()
            val cards = openers(topic, riders, library)
            Answer(
                Status.Success,
                "Here are three ways to start talking about the topic. Read the first line of each in Korean.",
                buildJsonArray {
                    cards.forEach { add(buildJsonObject { put("approach", it.approach.name); put("firstLine", it.first); put("followUp", it.followUp) }) }
                },
            )
        }
        Function.RECOMMEND_SAVED_TOPIC -> {
            val context = params["context"] ?: return Answer(Status.Asking, "What is happening right now?")
            val riders = riders(passenger, ride, library) ?: return askCompanion()
            val picks = topicsFor(context, riders, library, stamp.now)
            if (picks.isEmpty()) return Answer(Status.Success, "There are no saved topics yet.", JsonArray(emptyList()))
            Answer(
                Status.Success,
                "Here are the saved topics that fit the situation, best first.",
                buildJsonArray {
                    picks.forEach { pick ->
                        add(buildJsonObject {
                            put("topic", pick.topic.text)
                            putJsonArray("reasons") { pick.reasons.forEach { add(JsonPrimitive(it.wire())) } }
                        })
                    }
                },
            )
        }
        Function.SAVE_TOPIC -> {
            val topic = params["topic"]?.take(TEXT_LIMIT) ?: return Answer(Status.Asking, "What topic should I save?")
            val about = setOfNotNull(passenger?.id)
            Answer(
                Status.Success,
                "The topic was saved.",
                buildJsonArray { add(buildJsonObject { put("topic", topic); put("about", passenger?.name.orEmpty()) }) },
                Effect.AddTopic(Topic(TopicId(stamp.id), topic, about, addedAt = stamp.now)),
            )
        }
    }
}

private fun riders(passenger: Person?, ride: ActiveRide?, library: Library): Riders? {
    passenger ?: return null
    val driver = ride?.riders?.driver ?: library.people.firstOrNull { it.relation == Relation.Me }?.id ?: return null
    return Riders(driver, passenger.id)
}

private fun askCompanion() = Answer(Status.Asking, "Who is riding with you? Say the name saved in SAIGIL.")

private fun Reason.wire(): String = when (this) {
    Reason.MatchesContext -> "matches what is happening now"
    is Reason.AboutPassenger -> "about $name"
    is Reason.SharesInterest -> "close to their interest $interest"
}

fun Answer.response(function: Function): JsonObject = buildJsonObject {
    put("functionName", function.name)
    put("status", status.wire)
    put("message", message)
    data?.let { put("data", it.toString()) }
}
