package com.gomsoo.saigil.drive

import ai.pleos.playground.navi.data.RouteStateInfo
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.gomsoo.saigil.library.LibraryStore
import com.gomsoo.saigil.recommend.Trigger
import java.util.Locale

private const val TAG = "SaigilDrive"

/**
 * Turns navigation signals into spoken suggestions for the active ride. Everything runs on the main thread.
 * The screen is off-limits while driving, so this speaks instead of showing anything.
 */
@SuppressLint("MissingPermission") // NAVI_ROUTE and TTS_SERVICE are normal permissions declared in the manifest.
class RideCompanion(private val context: Context, private val store: LibraryStore, private val rides: Rides) {

    private val main = Handler(Looper.getMainLooper())
    private val spokenAt = mutableMapOf<Trigger, Long>()
    private var navi: NaviHelper? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pending: String? = null

    /** Connects to navigation and warms up speech. Safe to call again. */
    fun start() {
        if (navi != null) return
        val helper = NaviHelper(context)
        navi = helper
        helper.initialize(
            onConnected = {
                Log.i(TAG, "NaviHelper connected")
                helper.addListener(navigation)
                helper.getRouteStateInfo()
                helper.getTBTInfo()
            },
            onFailed = { Log.w(TAG, "NaviHelper unavailable, only the debug trigger will speak", it) },
        )
        startSpeech()
    }

    fun onTrigger(trigger: Trigger) = main.post {
        when (val decision = decide(trigger, rides.active.value, store.library.value, spokenAt, System.currentTimeMillis())) {
            is Decision.Speak -> {
                spokenAt[trigger] = System.currentTimeMillis()
                rides.said(decision.suggestion.key)
                Log.i(TAG, "trigger=$trigger key=${decision.suggestion.key} speak: ${decision.suggestion.sentence}")
                speak(decision.suggestion.sentence)
            }
            else -> Log.i(TAG, "trigger=$trigger quiet: $decision")
        }
    }

    private val navigation = object : NaviHelperEventListener {
        override fun onTBTInfo(tbtInfoList: List<TBTInfo>) {
            longDriveFrom(tbtInfoList)?.let(::onTrigger)
        }

        override fun onRouteStateInfo(routeStateInfo: RouteStateInfo) {
            routeStateInfo.trafficStatus?.let { jamFrom(it.congestionItemList) }?.let(::onTrigger)
        }
    }

    private fun startSpeech() {
        val speech = try {
            TextToSpeech(context, Mode.ON_DEVICE)
        } catch (e: RuntimeException) {
            Log.w(TAG, "TextToSpeech unavailable", e)
            return
        }
        tts = speech
        speech.addEventListener(object : EventListener {
            override fun onStart() = log("TTS start")
            override fun onDone() = log("TTS done")
            override fun onStop() = log("TTS stop")
            override fun onError(errorType: ErrorType) = log("TTS error: $errorType")
            override fun onReady() = Unit
            override fun onUpdatedRms(rms: Double) = Unit
        })
        speech.initialize(object : OnInitializeListener {
            override fun onSuccess() {
                main.post {
                    speech.setLocale(Locale.KOREA)
                    ttsReady = true
                    log("TTS ready")
                    pending?.let { pending = null; speech.speak(it, true) }
                }
            }

            override fun onFailure(errorType: ErrorType) = log("TTS initialize failed: $errorType")
        })
    }

    private fun speak(sentence: String) {
        val speech = tts ?: return log("TTS unavailable, not spoken")
        if (ttsReady) speech.speak(sentence, true) else pending = sentence
    }

    private fun log(message: String) {
        Log.i(TAG, message)
    }
}
