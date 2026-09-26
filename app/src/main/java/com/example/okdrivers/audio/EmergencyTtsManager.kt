package com.example.okdrivers.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.okdrivers.domain.model.UrgencyLevel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class EmergencyTtsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                isInitialized = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            } else {
                isInitialized = false
            }
        }
    }

    fun getPromptText(urgency: UrgencyLevel): String {
        return when (urgency) {
            UrgencyLevel.INITIAL -> "Emergency detected. Are you okay? Please respond."
            UrgencyLevel.URGENT -> "We still haven't heard from you. Please respond now."
            UrgencyLevel.FINAL -> "No response detected. Emergency services will be notified."
        }
    }

    suspend fun speak(urgency: UrgencyLevel): Result<Unit> {
        val prompt = getPromptText(urgency)
        if (!isInitialized || tts == null) {
            return Result.failure(IllegalStateException("TextToSpeech engine not initialized or unavailable"))
        }

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
            .setAudioAttributes(audioAttributes)
            .setAcceptsDelayedFocusGain(false)
            .build()

        val focusResult = audioManager?.requestAudioFocus(focusRequest) ?: AudioManager.AUDIOFOCUS_REQUEST_FAILED
        if (focusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return Result.failure(Exception("Failed to acquire audio focus for emergency prompt"))
        }

        return try {
            suspendCancellableCoroutine { continuation ->
                val utteranceId = UUID.randomUUID().toString()

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}

                    override fun onDone(id: String?) {
                        if (id == utteranceId && continuation.isActive) {
                            continuation.resume(Result.success(Unit))
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(id: String?) {
                        if (id == utteranceId && continuation.isActive) {
                            continuation.resume(Result.failure(Exception("TTS synthesis error")))
                        }
                    }

                    override fun onError(id: String?, errorCode: Int) {
                        super.onError(id, errorCode)
                        if (id == utteranceId && continuation.isActive) {
                            continuation.resume(Result.failure(Exception("TTS synthesis error code: $errorCode")))
                        }
                    }
                })

                val speakResult = tts?.speak(prompt, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                if (speakResult != TextToSpeech.SUCCESS) {
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(Exception("Failed to queue TTS utterance")))
                    }
                }

                continuation.invokeOnCancellation {
                    tts?.stop()
                }
            }
        } finally {
            audioManager?.abandonAudioFocusRequest(focusRequest)
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
