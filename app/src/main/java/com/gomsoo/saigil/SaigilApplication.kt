package com.gomsoo.saigil

import android.app.Application
import com.gomsoo.saigil.drive.RideCompanion
import com.gomsoo.saigil.drive.Rides
import com.gomsoo.saigil.library.LibraryStore
import com.gomsoo.saigil.library.PersonId
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

/** One library, one current user and one active ride per process, shared by the screen, the drive companion and Gleo. */
class SaigilApplication : Application() {
    val store by lazy { LibraryStore(File(filesDir, "library.json")) }
    /** Memory only, so every entry into the app asks again. */
    val me = MutableStateFlow<PersonId?>(null)
    val rides = Rides()
    val companion by lazy { RideCompanion(this, store, rides) }

    override fun onCreate() {
        super.onCreate()
        // Navigation and speech connect only once a ride has begun.
        MainScope().launch {
            rides.active.filterNotNull().first()
            companion.start()
        }
    }
}
