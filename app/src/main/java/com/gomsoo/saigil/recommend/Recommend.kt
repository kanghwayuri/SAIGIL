package com.gomsoo.saigil.recommend

import com.gomsoo.saigil.library.Flow
import com.gomsoo.saigil.library.Library
import com.gomsoo.saigil.library.Riders
import com.gomsoo.saigil.library.Topic
import com.gomsoo.saigil.library.TopicId

data class Opener(val approach: Approach, val first: String, val followUp: String)

sealed interface Reason {
    data object MatchesContext : Reason
    data class SharesInterest(val interest: String) : Reason
}

data class TopicPick(val topic: Topic, val reasons: List<Reason>)

/** What the road is doing now, as far as navigation tells. Unknown numbers stay null. */
sealed interface Situation {
    data class Highway(val remainingKm: Int?) : Situation
    data class Jam(val lengthM: Int?, val avgSpeedKmh: Int?) : Situation
}

/** One proactive recommendation. [key] identifies it so a ride never hears it twice. */
sealed interface Suggestion {
    val key: String
    val sentence: String

    data class SavedTopic(val id: TopicId, val text: String) : Suggestion {
        override val key get() = "topic:${id.value}"
        override val sentence get() = "저장해 둔 화제 하나 꺼내 볼까요? $text"
    }

    data class Trend(val text: String) : Suggestion {
        override val key get() = "trend:$text"
        override val sentence get() = "요즘 이야깃거리 하나 어때요? $text"
    }

    data class BalanceGame(val a: String, val b: String) : Suggestion {
        override val key get() = "balance:$a|$b"
        override val sentence get() = "가볍게 밸런스 게임 해 볼까요? $a, $b 중에 고른다면요?"
    }
}

/** The words said before a suggestion, so the riders hear why it came up now. */
val Situation.lead: String
    get() = when (this) {
        is Situation.Highway -> "한동안 길게 달려요."
        is Situation.Jam -> "길이 막히네요."
    }

private const val RECENT_MS = 3L * 24 * 60 * 60 * 1000

/** Three different ways to open [topic], in the register the user uses with the companion. */
fun openers(topic: String, riders: Riders, library: Library): List<Opener> {
    val companion = library.person(riders.companion)
    val register = library.register(riders)
    val bond = library.bond(riders.pair)
    val order = when {
        bond.lastFlow == Flow.Awkward -> listOf(Approach.Ask, Approach.Celebrate, Approach.Listen, Approach.ShareMine)
        companion != null && companion.interests.any { shared(topic, it) > 0 } ->
            listOf(Approach.Listen, Approach.Ask, Approach.Celebrate, Approach.ShareMine)
        else -> listOf(Approach.Ask, Approach.ShareMine, Approach.Celebrate, Approach.Listen)
    }
    return order.take(3).map { approach ->
        val templates = openerTemplates.getValue(approach)
        val template = templates[bond.count % templates.size]
        Opener(approach, template.first(topic.trim(), register), template.followUp)
    }
}

/** The topics saved for this pair that fit [context] best, at most three. */
fun topicsFor(context: String, riders: Riders, library: Library, now: Long): List<TopicPick> =
    ranked(context, riders, library, now).take(3)

/**
 * The next suggestion for [situation], or null once everything has been said this ride.
 * A jam starts with a balance game because a stopped car suits a quick back-and-forth.
 */
fun proactive(situation: Situation, riders: Riders, library: Library, said: Set<String>, now: Long): Suggestion? {
    val saved = ranked("", riders, library, now).map { Suggestion.SavedTopic(it.topic.id, it.topic.text) }
    val trends = trending.map { Suggestion.Trend(it) }
    val games = balanceGames.map { (a, b) -> Suggestion.BalanceGame(a, b) }
    val order = when (situation) {
        is Situation.Highway -> saved + trends + games
        is Situation.Jam -> games + saved + trends
    }
    return order.firstOrNull { it.key !in said }
}

private fun ranked(context: String, riders: Riders, library: Library, now: Long): List<TopicPick> {
    val companion = library.person(riders.companion)
    val likes = companion?.let { it.interests + it.places }.orEmpty()
    return library.topicsOf(riders.pair)
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
