package com.example.okdrivers.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.okdrivers.domain.model.ResponseClassification
import com.example.okdrivers.domain.model.UrgencyLevel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume

class RealVoiceVerificationService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ttsManager: EmergencyTtsManager,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : VoiceVerificationService {

    override suspend fun verifyVoiceResponse(
        urgency: UrgencyLevel,
        attemptCount: Int,
        timeoutMillis: Long
    ): VoiceVerificationResult = withContext(dispatcher) {
        val startTime = System.currentTimeMillis()

        // 1. Speak prompt via TTS
        val ttsResult = ttsManager.speak(urgency)
        if (ttsResult.isFailure) {
            val latency = System.currentTimeMillis() - startTime
            return@withContext VoiceVerificationResult(
                classification = ResponseClassification.UNRESPONSIVE,
                transcribedText = null,
                latencyMs = latency,
                attemptCount = attemptCount
            )
        }

        // 2. Listen via SpeechRecognizer
        val transcript = suspendCancellableCoroutine<String?> { continuation ->
            var speechRecognizer: SpeechRecognizer? = null
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    if (continuation.isActive) continuation.resume(null)
                    return@suspendCancellableCoroutine
                }

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()
                        if (continuation.isActive) {
                            continuation.resume(text)
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                speechRecognizer?.startListening(intent)

                continuation.invokeOnCancellation {
                    try {
                        speechRecognizer?.stopListening()
                        speechRecognizer?.destroy()
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        }

        val latency = System.currentTimeMillis() - startTime
        val classification = classifyTranscript(transcript)

        VoiceVerificationResult(
            classification = classification,
            transcribedText = transcript,
            latencyMs = latency,
            attemptCount = attemptCount
        )
    }

    private fun classifyTranscript(transcript: String?): ResponseClassification {
        if (transcript.isNullOrBlank()) {
            return ResponseClassification.UNRESPONSIVE
        }
        val affirmativeKeywords = listOf("ok", "okay", "yes", "help", "fine", "yeah", "sure", "i'm okay", "i am okay")
        val cleaned = transcript.trim().lowercase()
        val isAffirmative = affirmativeKeywords.any { cleaned.contains(it) }
        return if (isAffirmative) {
            ResponseClassification.RESPONSIVE
        } else {
            ResponseClassification.IMPAIRED
        }
    }
}
