package com.gomsoo.saigil.gleo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.gomsoo.saigil.SaigilApplication
import com.gomsoo.saigil.screen.Stamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.util.UUID

private const val TAG = "SaigilGleo"
private const val RESPONSE_ACTION = "ai.pleos.playground.intent.action.APP_ACTION_RESPONSE"

/** Gleo calls a schema.json function as a broadcast and cancels it after 7 seconds, so this answers within 6. */
class GleoReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val function = Function.entries.firstOrNull { it.name == intent.action } ?: return
        val raw = intent.getStringExtra("ACTION_PARAM") ?: "{}"
        Log.i(TAG, "request $function $raw")
        val app = context.applicationContext as SaigilApplication
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val answer = withTimeoutOrNull(6_000) {
                    val result = answer(function, params(raw), app.store.library.value, app.me.value, app.rides.active.value, Stamp(System.currentTimeMillis(), UUID.randomUUID().toString()))
                    result.write?.let { write -> app.store.update(write::applyTo) }
                    result
                } ?: Answer(Status.Fail, "SAIGIL could not answer in time.")
                val response = answer.response(function).toString()
                Log.i(TAG, "response $response")
                context.sendBroadcast(
                    Intent(RESPONSE_ACTION).putExtra("packageName", context.packageName).putExtra("response", response),
                )
            } finally {
                pending.finish()
            }
        }
    }

    /** ACTION_PARAM is JSON from another app. Anything that is not a string-valued object is ignored. */
    private fun params(raw: String): Map<String, String> {
        val json = try {
            Json.parseToJsonElement(raw).jsonObject
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "ACTION_PARAM is not a JSON object: $raw")
            JsonObject(emptyMap())
        }
        return json.mapNotNull { (key, value) -> (value as? JsonPrimitive)?.takeIf { it.isString && it.content.isNotBlank() }?.let { key to it.content.trim() } }
            .toMap()
    }
}
