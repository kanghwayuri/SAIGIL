package com.gomsoo.saigil.drive

import com.gomsoo.saigil.library.Riders
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** The pair riding now, and what has been said to them. Lives as long as the process. */
data class ActiveRide(val riders: Riders, val said: Set<String> = emptySet())

class Rides {
    private val state = MutableStateFlow<ActiveRide?>(null)
    val active: StateFlow<ActiveRide?> = state.asStateFlow()

    /** Starting the same pair again keeps the ride, so nothing said earlier is repeated. */
    fun begin(riders: Riders) = state.update { if (it?.riders == riders) it else ActiveRide(riders) }

    fun said(key: String) = state.update { it?.copy(said = it.said + key) }
}
