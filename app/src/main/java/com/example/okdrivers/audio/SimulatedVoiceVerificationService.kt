package com.example.okdrivers.audio

import com.example.okdrivers.domain.model.ResponseClassification
import com.example.okdrivers.domain.model.UrgencyLevel
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SimulatedVoiceVerificationService @Inject constructor(
    private val ttsManager: EmergencyTtsManager
) : VoiceVerificationService {

    var simulatedResponse: String? = "I'm okay"

    override suspend fun verifyVoiceResponse(
        urgency: UrgencyLevel,
        attemptCount: Int,
        timeoutMillis: Long
    ): VoiceVerificationResult {
        val startTime = System.currentTimeMillis()

        ttsManager.speak(urgency)
        delay(minOf(timeoutMillis, 500L))

        val latency = System.currentTimeMillis() - startTime
        val transcript = simulatedResponse
        val classification = classifyTranscript(transcript)

        return VoiceVerificationResult(
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
