package com.gomsoo.saigil

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.constant.area.DoorValue
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var vehicle: Vehicle
    private lateinit var resultView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resultView = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 24f
            text = "Loading door position..."
        }
        setContentView(resultView)

        vehicle = Vehicle(this)
        // Calls made before the service binds are queued by the SDK and sent once connected.
        vehicle.initialize()
        requestDriverDoorPosition()
    }

    private fun requestDriverDoorPosition() {
        vehicle.getDoor().getCurrentDoorPosition(
            area = DoorArea(DoorValue.ROW_1_LEFT),
            onComplete = { position ->
                Log.d(TAG, "Door position (ROW_1_LEFT): $position")
                runOnUiThread { resultView.text = "ROW_1_LEFT door position: $position" }
            },
            onFailed = { e ->
                Log.e(TAG, "Failed to get door position", e)
                runOnUiThread { resultView.text = "Failed: ${e.message}" }
            },
        )
    }

    override fun onDestroy() {
        // Release here, not inside the callback: releasing early unbinds the service and drops queued calls.
        vehicle.release()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "SAIGIL"
    }
}
