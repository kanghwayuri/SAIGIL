package com.gomsoo.saigil

import android.app.Application
import com.gomsoo.saigil.drive.Rides
import com.gomsoo.saigil.library.LibraryStore
import java.io.File

/** One library and one active ride per process, shared by the screen, the drive companion and Gleo. */
class SaigilApplication : Application() {
    val store by lazy { LibraryStore(File(filesDir, "library.json")) }
    val rides = Rides()
}
