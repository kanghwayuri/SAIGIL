package com.gomsoo.saigil.speech

import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.ErrorType
import ai.pleos.playground.stt.constant.Mode
import ai.pleos.playground.stt.listener.OnInitializeListener
import ai.pleos.playground.stt.listener.ResultListener
import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.gomsoo.saigil.screen.Heard
import com.gomsoo.saigil.screen.SttFailure
import java.util.Locale

/** Speech to text behind a seam so the screen reducer never touches the SDK. Callbacks arrive on the main thread. */
interface SpeechInput {
    fun start(onHeard: (Heard) -> Unit)
    fun stop()
    fun release()
}

/** Pleos SpeechToText in on-device mode, which needs no app registration. A recognizer is never reused after release. */
@SuppressLint("MissingPermission") // pleos.car.permission.STT_SERVICE is a normal permission declared in the manifest.
class PleosSpeechInput(private val context: Context) : SpeechInput {

    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechToText? = null

    override fun start(onHeard: (Heard) -> Unit) {
        release()
        val emit = { heard: Heard -> main.post { onHeard(heard) }; Unit }
        val stt = try {
            SpeechToText(context, Mode.ON_DEVICE)
        } catch (e: RuntimeException) {
            Log.w(TAG, "SpeechToText unavailable", e)
            emit(Heard.Failed(SttFailure.Unavailable))
            return
        }
        recognizer = stt
        stt.initialize(object : OnInitializeListener {
            override fun onSuccess() {
                stt.setLocale(Locale.KOREA)
                stt.addListener(Results(emit))
                stt.request()
            }

            override fun onFailure(errorType: ErrorType) {
                Log.w(TAG, "STT initialize failed: $errorType")
                emit(Heard.Failed(errorType.toFailure()))
            }
        })
    }

    override fun stop() {
        recognizer?.stop()
    }

    override fun release() {
        recognizer?.release()
        recognizer = null
    }

    private class Results(private val emit: (Heard) -> Unit) : ResultListener {
        override fun onUpdated(stt: String, completed: Boolean) {
            Log.d(TAG, "STT ${if (completed) "final" else "partial"}: $stt")
            emit(if (completed) Heard.Final(stt) else Heard.Partial(stt))
        }

        override fun onError(errorType: ErrorType) {
            Log.w(TAG, "STT error: $errorType")
            emit(Heard.Failed(errorType.toFailure()))
        }

        override fun onEndedRecognition() = emit(Heard.Ended)
        override fun onReady() = Unit
        override fun onStartedRecognition() = Unit
        override fun onUpdatedEpdData(on: Long, off: Long) = Unit
        override fun onUpdatedRms(rms: Double) = Unit
    }

    private companion object {
        const val TAG = "SAIGIL"
    }
}

private fun ErrorType.toFailure(): SttFailure = if (this == ErrorType.TIMEOUT) SttFailure.NoSpeech else SttFailure.Unavailable
