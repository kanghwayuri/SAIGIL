package com.gomsoo.saigil.drive

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.gomsoo.saigil.SaigilApplication
import com.gomsoo.saigil.recommend.Trigger

/** The emulator cannot simulate a route, so debug builds accept `--es trigger LongDrive|Jam`. */
class DebugTriggerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("trigger")
        val trigger = Trigger.entries.firstOrNull { it.name == name }
            ?: return Unit.also { Log.w("SaigilDrive", "debug trigger ignored, unknown trigger=$name") }
        (context.applicationContext as SaigilApplication).companion.onTrigger(trigger)
    }
}
