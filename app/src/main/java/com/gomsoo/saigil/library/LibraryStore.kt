package com.gomsoo.saigil.library

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** The whole library in one JSON file. Every change is written before it becomes visible. */
class LibraryStore(private val file: File) {

    private val writes = Mutex()
    private val state = MutableStateFlow(read())
    val library: StateFlow<Library> = state.asStateFlow()

    suspend fun update(change: (Library) -> Library): Library = writes.withLock {
        val next = change(state.value)
        if (next != state.value) {
            withContext(Dispatchers.IO) { write(next) }
            state.value = next
        }
        next
    }

    private fun read(): Library {
        if (!file.exists()) return Library()
        return try {
            json.decodeFromString(Library.serializer(), file.readText())
        } catch (e: IllegalArgumentException) {
            // Covers SerializationException. Keep the unreadable file instead of overwriting it on the next save.
            val aside = File(file.parentFile, "${file.name}.unreadable")
            file.renameTo(aside)
            Log.w(TAG, "library.json is unreadable, moved to ${aside.name}", e)
            Library()
        }
    }

    private fun write(library: Library) {
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(json.encodeToString(Library.serializer(), library))
        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    private companion object {
        const val TAG = "SAIGIL"
        val json = Json { ignoreUnknownKeys = true }
    }
}
