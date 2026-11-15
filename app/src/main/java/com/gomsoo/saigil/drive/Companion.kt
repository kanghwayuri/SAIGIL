package com.gomsoo.saigil.drive

import ai.pleos.playground.navi.constants.TrafficCongestionType
import ai.pleos.playground.navi.constants.TurnType
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.navi.data.TrafficCongestionInfo
import com.gomsoo.saigil.library.Library
import com.gomsoo.saigil.recommend.Situation
import com.gomsoo.saigil.recommend.Suggestion
import com.gomsoo.saigil.recommend.lead
import com.gomsoo.saigil.recommend.proactive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt
import kotlin.reflect.KClass

const val COOLDOWN_MS = 10L * 60 * 1000
private const val HIGHWAY_ENTRY_M = 500.0
private const val JAM_AHEAD_M = 300.0

private val highwayEntries = setOf(TurnType.TURN_ENTER_IC_R, TurnType.TURN_ENTER_IC_L, TurnType.TURN_ENTER_IC_C)

/** NaviHelper has no "on a highway" field, so an imminent interchange entry stands in for a long drive ahead. */
fun highwayFrom(tbt: List<TBTInfo>, remainingKm: Int?): Situation.Highway? =
    tbt.firstOrNull()?.takeIf { it.type in highwayEntries && it.distance <= HIGHWAY_ENTRY_M }?.let { Situation.Highway(remainingKm) }

fun jamFrom(congestion: List<TrafficCongestionInfo>): Situation.Jam? =
    congestion.firstOrNull {
        it.offset <= JAM_AHEAD_M && it.trafficCongestionType in setOf(TrafficCongestionType.SLOW, TrafficCongestionType.HEAVY)
    }?.let { Situation.Jam(it.distance.roundToInt(), it.avrSpeed.roundToInt()) }

/** What the driver sees while the car moves: the road now and the last thing said aloud. */
data class DriveBoard(val situation: Situation? = null, val latest: Suggestion? = null)

sealed interface Decision {
    data class Speak(val suggestion: Suggestion, val line: String) : Decision
    data object NoRide : Decision
    data object CoolingDown : Decision
    data object NothingLeft : Decision
}

/** Decides what to say for each situation and publishes the board. Each kind of situation speaks at most once per [COOLDOWN_MS]. */
class Drive(private val rides: Rides, private val library: () -> Library) {
    private val state = MutableStateFlow(DriveBoard())
    val board: StateFlow<DriveBoard> = state.asStateFlow()
    private val spokenAt = mutableMapOf<KClass<out Situation>, Long>()

    fun on(situation: Situation, now: Long): Decision {
        state.update { it.copy(situation = situation) }
        val ride = rides.active.value ?: return Decision.NoRide
        val last = spokenAt[situation::class]
        if (last != null && now - last < COOLDOWN_MS) return Decision.CoolingDown
        val suggestion = proactive(situation, ride.riders, library(), ride.said, now) ?: return Decision.NothingLeft
        spokenAt[situation::class] = now
        rides.said(suggestion.key)
        state.update { it.copy(latest = suggestion) }
        return Decision.Speak(suggestion, "${situation.lead} ${suggestion.sentence}")
    }
}
