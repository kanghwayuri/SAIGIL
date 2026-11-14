package com.gomsoo.saigil.drive

import ai.pleos.playground.navi.constants.TrafficCongestionType
import ai.pleos.playground.navi.constants.TurnType
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.navi.data.TrafficCongestionInfo
import com.gomsoo.saigil.library.Library
import com.gomsoo.saigil.recommend.Suggestion
import com.gomsoo.saigil.recommend.Trigger
import com.gomsoo.saigil.recommend.proactive

const val COOLDOWN_MS = 10L * 60 * 1000
private const val HIGHWAY_ENTRY_M = 500.0
private const val JAM_AHEAD_M = 300.0

private val highwayEntries = setOf(TurnType.TURN_ENTER_IC_R, TurnType.TURN_ENTER_IC_L, TurnType.TURN_ENTER_IC_C)

/** NaviHelper has no "on a highway" field, so an imminent interchange entry stands in for a long drive ahead. */
fun longDriveFrom(tbt: List<TBTInfo>): Trigger? =
    tbt.firstOrNull()?.takeIf { it.type in highwayEntries && it.distance <= HIGHWAY_ENTRY_M }?.let { Trigger.LongDrive }

fun jamFrom(congestion: List<TrafficCongestionInfo>): Trigger? =
    congestion.firstOrNull {
        it.offset <= JAM_AHEAD_M && it.trafficCongestionType in setOf(TrafficCongestionType.SLOW, TrafficCongestionType.HEAVY)
    }?.let { Trigger.Jam }

sealed interface Decision {
    data class Speak(val suggestion: Suggestion) : Decision
    data object NoRide : Decision
    data object CoolingDown : Decision
    data object NothingLeft : Decision
}

/** Whether to speak for [trigger] now. Each kind of trigger speaks at most once per [COOLDOWN_MS]. */
fun decide(trigger: Trigger, ride: ActiveRide?, library: Library, spokenAt: Map<Trigger, Long>, now: Long): Decision {
    if (ride == null) return Decision.NoRide
    val last = spokenAt[trigger]
    if (last != null && now - last < COOLDOWN_MS) return Decision.CoolingDown
    return proactive(trigger, ride.riders, library, ride.said, now)?.let { Decision.Speak(it) } ?: Decision.NothingLeft
}
