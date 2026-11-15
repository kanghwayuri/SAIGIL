package com.gomsoo.saigil.gleo

import com.gomsoo.saigil.drive.ActiveRide
import com.gomsoo.saigil.library.Library
import com.gomsoo.saigil.library.PersonId
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

/**
 * [me] is the user picked on SAIGIL's screen in this process. The companion is the named profile,
 * or else the active ride's companion when that ride is the same user's.
 */
fun answer(function: Function, params: Map<String, String>, library: Library, me: PersonId?, ride: ActiveRide?, stamp: Stamp): Answer {
    me ?: return Answer(Status.Asking, "SAIGIL does not know who is using it yet. Open SAIGIL and choose your profile first.")
    val named = params["companion"]?.let { library.personNamed(it) ?: return Answer(Status.Fail, "There is no SAIGIL profile named $it.") }
    val companion = named?.id ?: ride?.riders?.takeIf { it.me == me }?.companion
    val riders = companion?.let { Riders.of(me, it) }
        ?: return Answer(Status.Asking, "Who is riding with you? Say the name saved in SAIGIL.")
    return when (function) {
        Function.RECOMMEND_OPENER -> {
            val topic = params["topic"] ?: return Answer(Status.Asking, "What topic would you like to talk about?")
            val cards = openers(topic, riders, library)
            Answer(
                Status.Success,
                "Here are three ways to start talking about the topic.",
                buildJsonArray {
                    cards.forEach { add(buildJsonObject { put("approach", it.approach.name); put("firstLine", it.first); put("followUp", it.followUp) }) }
                },
            )
        }
        Function.RECOMMEND_SAVED_TOPIC -> {
            val context = params["context"] ?: return Answer(Status.Asking, "What is happening right now?")
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
            Answer(
                Status.Success,
                "The topic was saved.",
                buildJsonArray { add(buildJsonObject { put("topic", topic); put("companion", library.person(riders.companion)?.name.orEmpty()) }) },
                Effect.AddTopic(Topic(TopicId(stamp.id), topic, riders.pair, addedAt = stamp.now)),
            )
        }
    }
}

private fun Reason.wire(): String = when (this) {
    Reason.MatchesContext -> "matches what is happening now"
    is Reason.SharesInterest -> "close to their interest $interest"
}

fun Answer.response(function: Function): JsonObject = buildJsonObject {
    put("functionName", function.name)
    put("status", status.wire)
    put("message", message)
    data?.let { put("data", it.toString()) }
}
