package com.gomsoo.saigil.vehicle

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.content.pm.PackageManager
import com.gomsoo.saigil.session.Restriction
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf

/** Emits once the car service answers. Collect to connect, cancel to release. */
fun vehicleRestrictions(context: Context): Flow<Restriction> {
    if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
        return flowOf(Restriction.Unknown)
    }
    return callbackFlow {
        var manager: CarUxRestrictionsManager? = null
        val car = Car.createCar(context, null, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { car, ready ->
            manager?.unregisterListener()
            manager = null
            val connected = if (ready) car.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager else null
            if (connected == null) {
                trySend(Restriction.Unknown)
            } else {
                connected.registerListener { trySend(it.toRestriction()) }
                manager = connected
                trySend(connected.currentCarUxRestrictions.toRestriction())
            }
        }
        awaitClose {
            manager?.unregisterListener()
            car.disconnect()
        }
    }.distinctUntilChanged()
}

private fun CarUxRestrictions?.toRestriction(): Restriction = when {
    this == null -> Restriction.Unknown
    isRequiresDistractionOptimization -> Restriction.Restricted
    else -> Restriction.Parked
}
