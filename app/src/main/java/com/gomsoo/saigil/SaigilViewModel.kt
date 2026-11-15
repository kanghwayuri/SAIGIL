package com.gomsoo.saigil

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gomsoo.saigil.drive.ActiveRide
import com.gomsoo.saigil.library.Library
import com.gomsoo.saigil.library.PersonId
import com.gomsoo.saigil.screen.Effect
import com.gomsoo.saigil.screen.Event
import com.gomsoo.saigil.screen.Model
import com.gomsoo.saigil.screen.Stamp
import com.gomsoo.saigil.screen.reduce
import com.gomsoo.saigil.speech.PleosSpeechInput
import com.gomsoo.saigil.speech.SpeechInput
import com.gomsoo.saigil.vehicle.vehicleRestrictions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class Ui(val model: Model, val library: Library, val ride: ActiveRide?, val me: PersonId?)

class SaigilViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SaigilApplication
    private val store = app.store
    private val speech: SpeechInput = PleosSpeechInput(application)
    private val model = MutableStateFlow(Model())

    val ui: StateFlow<Ui> = combine(model, store.library, app.rides.active, app.me, ::Ui)
        .stateIn(viewModelScope, SharingStarted.Eagerly, Ui(model.value, store.library.value, app.rides.active.value, null))

    init {
        // A new screen model starts at "who am I", so the user is unknown again. A configuration change keeps both.
        app.me.value = null
        viewModelScope.launch {
            vehicleRestrictions(application).collect { send(Event.VehicleChanged(it)) }
        }
    }

    /** Main thread only. */
    fun send(event: Event) {
        val next = reduce(model.value, event, store.library.value, app.me.value, Stamp(System.currentTimeMillis(), UUID.randomUUID().toString()))
        model.value = next.model
        next.effects.forEach(::run)
    }

    private fun run(effect: Effect) {
        when (effect) {
            is Effect.Write -> viewModelScope.launch { store.update(effect::applyTo) }
            is Effect.BecomeMe -> app.me.value = effect.me
            is Effect.BeginRide -> app.rides.begin(effect.riders)
            Effect.StartListening -> speech.start { send(Event.Speech(it)) }
            Effect.StopListening -> speech.stop()
        }
    }

    override fun onCleared() {
        speech.release()
    }
}
