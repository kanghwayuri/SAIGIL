package com.gomsoo.saigil

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gomsoo.saigil.session.Event
import com.gomsoo.saigil.session.Model
import com.gomsoo.saigil.session.reduce
import com.gomsoo.saigil.vehicle.vehicleRestrictions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SessionViewModel(application: Application) : AndroidViewModel(application) {

    // Memory only, never SavedStateHandle: F05 wants prepared content gone when the process dies,
    // which overrides the platform guide's advice to restore user data after process recreation.
    private val state = MutableStateFlow(Model())
    val model: StateFlow<Model> = state.asStateFlow()

    init {
        viewModelScope.launch {
            vehicleRestrictions(application).collect { send(Event.VehicleChanged(it)) }
        }
    }

    fun send(event: Event) = state.update { reduce(it, event) }
}
