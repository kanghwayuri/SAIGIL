package com.gomsoo.saigil.recommend

import com.gomsoo.saigil.library.Flow
import com.gomsoo.saigil.library.Library
import com.gomsoo.saigil.library.Register
import com.gomsoo.saigil.library.Riders
import com.gomsoo.saigil.library.Topic
import com.gomsoo.saigil.library.register

data class Opener(val approach: Approach, val first: String, val followUp: String)

sealed interface Reason {
    data object MatchesContext : Reason
    data class AboutPassenger(val name: String) : Reason
    data class SharesInterest(val interest: String) : Reason
}

data class TopicPick(val topic: Topic, val reasons: List<Reason>)

enum class Trigger { LongDrive, Jam }

/** One spoken recommendation. [key] identifies what was said so a ride never hears it twice. */
data class Suggestion(val key: String, val sentence: String)

private const val RECENT_MS = 3L * 24 * 60 * 60 * 1000

/** Three different ways to open [topic], in the register the user uses with the passenger. */
fun openers(topic: String, riders: Riders, library: Library): List<Opener> {
    val passenger = library.person(riders.passenger)
    val register = passenger?.relation?.register ?: Register.Casual
    val bond = library.bond(riders)
    val order = when {
        bond.lastFlow == Flow.Awkward -> listOf(Approach.Ask, Approach.Celebrate, Approach.Listen, Approach.ShareMine)
        passenger != null && passenger.interests.any { shared(topic, it) > 0 } ->
            listOf(Approach.Listen, Approach.Ask, Approach.Celebrate, Approach.ShareMine)
        else -> listOf(Approach.Ask, Approach.ShareMine, Approach.Celebrate, Approach.Listen)
    }
    return order.take(3).map { approach ->
        val templates = openerTemplates.getValue(approach)
        val template = templates[bond.count % templates.size]
        Opener(approach, template.first(topic.trim(), register), template.followUp)
    }
}

/** The saved topics that fit [context] best for this passenger, at most three. */
fun topicsFor(context: String, riders: Riders, library: Library, now: Long): List<TopicPick> =
    ranked(context, riders, library, now).take(3)

/** One sentence to say aloud, or null once everything has been said this ride. */
fun proactive(trigger: Trigger, riders: Riders, library: Library, said: Set<String>, now: Long): Suggestion? {
    val lead = when (trigger) {
        Trigger.LongDrive -> "한동안 길게 달려요."
        Trigger.Jam -> "길이 막히네요."
    }
    val saved = ranked("", riders, library, now)
        .filter { it.topic.about.isEmpty() || riders.passenger in it.topic.about }
        .map { Suggestion("topic:${it.topic.id.value}", "$lead 저장해 둔 화제 하나 꺼내 볼까요? ${it.topic.text}") }
    val trends = trending.mapIndexed { i, text -> Suggestion("trend:$i", "$lead 요즘 이야깃거리 하나 어때요? $text") }
    val games = balanceGames.mapIndexed { i, text -> Suggestion("balance:$i", "$lead 가볍게 밸런스 게임 해 볼까요? $text") }
    return (saved + trends + games).firstOrNull { it.key !in said }
}

private fun ranked(context: String, riders: Riders, library: Library, now: Long): List<TopicPick> {
    val passenger = library.person(riders.passenger)
    val likes = passenger?.let { it.interests + it.places }.orEmpty()
    return library.topics
        .map { topic ->
            val reasons = mutableListOf<Reason>()
            var score = 0
            val contextMatch = shared(context, topic.text)
            if (contextMatch > 0) {
                score += 3 * contextMatch
                reasons += Reason.MatchesContext
            }
            likes.firstOrNull { shared(topic.text, it) > 0 }?.let {
                score += 1
                reasons += Reason.SharesInterest(it)
            }
            if (passenger != null && passenger.id in topic.about) {
                score += 2
                reasons += Reason.AboutPassenger(passenger.name)
            } else if (topic.about.isNotEmpty()) {
                score -= 1
            }
            if (topic.usedAt != null && now - topic.usedAt < RECENT_MS) score -= 2
            Triple(topic, reasons.toList(), score)
        }
        .sortedWith(compareByDescending<Triple<Topic, List<Reason>, Int>> { it.third }.thenByDescending { it.first.addedAt })
        .map { TopicPick(it.first, it.second) }
}

/** Shared Hangul character bigrams. Works without morphology because Korean particles attach to the end of a word. */
internal fun shared(a: String, b: String): Int = (bigrams(a) intersect bigrams(b)).size

private fun bigrams(text: String): Set<String> = text.lowercase()
    .split(Regex("[^\\p{L}\\p{N}]+"))
    .filter { it.length >= 2 }
    .flatMap { word -> word.windowed(2) }
    .toSet()
