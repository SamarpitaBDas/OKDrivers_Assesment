package com.example.okdrivers.audio

import com.example.okdrivers.domain.model.UrgencyLevel
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
open class SimulatedVoiceVerificationService @Inject constructor(
    private val ttsManager: EmergencyTtsManager
) : VoiceVerificationService {

    var simulatedResponse: String? = "I'm okay"

    override fun getPromptText(urgency: UrgencyLevel): String {
        return ttsManager.getPromptText(urgency)
    }

    override suspend fun verifyVoiceResponse(
        urgency: UrgencyLevel,
        attemptCount: Int,
        timeoutMillis: Long
    ): VoiceVerificationResult {
        val startTime = System.currentTimeMillis()

        ttsManager.speak(urgency)
        delay(minOf(timeoutMillis, 500L).milliseconds)

        val latency = System.currentTimeMillis() - startTime
        val transcript = simulatedResponse
        val classification = ResponseClassifier.classify(transcript)

        return VoiceVerificationResult(
            classification = classification,
            transcribedText = transcript,
            latencyMs = latency,
            attemptCount = attemptCount
        )
    }
}
