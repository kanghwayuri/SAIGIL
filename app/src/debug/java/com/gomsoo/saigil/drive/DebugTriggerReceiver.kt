package com.gomsoo.saigil.drive

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.gomsoo.saigil.SaigilApplication
import com.gomsoo.saigil.recommend.Situation

/**
 * The emulator cannot simulate a route, so debug builds accept `--es trigger LongDrive|Jam`
 * with optional `--ei remaining_km`, `--ei jam_m` and `--ei avg_kmh` for the board.
 */
class DebugTriggerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        fun int(name: String) = if (intent.hasExtra(name)) intent.getIntExtra(name, 0) else null
        val situation = when (val name = intent.getStringExtra("trigger")) {
            "LongDrive", "Highway" -> Situation.Highway(int("remaining_km"))
            "Jam" -> Situation.Jam(int("jam_m"), int("avg_kmh"))
            else -> return Unit.also { Log.w("SaigilDrive", "debug trigger ignored, unknown trigger=$name") }
        }
        (context.applicationContext as SaigilApplication).companion.onSituation(situation)
    }
}
