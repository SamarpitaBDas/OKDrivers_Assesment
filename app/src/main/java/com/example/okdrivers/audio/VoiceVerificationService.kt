package com.example.okdrivers.audio

import com.example.okdrivers.domain.model.ResponseClassification
import com.example.okdrivers.domain.model.UrgencyLevel

data class VoiceVerificationResult(
    val classification: ResponseClassification,
    val transcribedText: String?,
    val latencyMs: Long,
    val attemptCount: Int
)

interface VoiceVerificationService {
    suspend fun verifyVoiceResponse(
        urgency: UrgencyLevel,
        attemptCount: Int,
        timeoutMillis: Long
    ): VoiceVerificationResult
}
