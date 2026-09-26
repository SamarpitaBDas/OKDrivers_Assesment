package com.example.okdrivers.domain.engine

import com.example.okdrivers.audio.VoiceVerificationService
import com.example.okdrivers.data.repository.AIConversationRepository
import com.example.okdrivers.domain.model.AIConversationSession
import com.example.okdrivers.domain.model.AnomalySeverity
import com.example.okdrivers.domain.model.EmergencyState
import com.example.okdrivers.domain.model.ResponseClassification
import com.example.okdrivers.domain.model.UrgencyLevel
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class VerificationOutcome {
    RESOLVED_RESPONSIVE,
    ESCALATED_COMMUNITY,
    ESCALATED_AUTHORITY
}

@Singleton
class EmergencyVerificationOrchestrator @Inject constructor(
    private val voiceVerificationService: VoiceVerificationService,
    private val aiConversationRepository: AIConversationRepository,
    private val incidentStateMachine: IncidentStateMachine
) {

    suspend fun runVerificationFlow(
        incidentId: String,
        driverId: String?,
        vehicleId: String?,
        severity: AnomalySeverity,
        now: Long = System.currentTimeMillis()
    ): VerificationOutcome {
        val attempts = listOf(
            Triple(UrgencyLevel.INITIAL, 1, 5000L),
            Triple(UrgencyLevel.URGENT, 2, 4000L),
            Triple(UrgencyLevel.FINAL, 3, 3000L)
        )

        for ((urgency, attemptCount, timeout) in attempts) {
            val startTime = System.currentTimeMillis()
            val sessionId = UUID.randomUUID().toString()
            val promptText = voiceVerificationService.getPromptText(urgency)

            // Insert initial in-flight session (Option A: completed = false while speaking/listening)
            val initialSession = AIConversationSession(
                id = sessionId,
                incidentId = incidentId,
                startedAt = startTime,
                endedAt = null,
                prompt = promptText,
                response = null,
                responseClassification = null,
                responseLatencyMs = null,
                attemptCount = attemptCount,
                completed = false
            )
            aiConversationRepository.saveSession(initialSession)

            val result = voiceVerificationService.verifyVoiceResponse(urgency, attemptCount, timeout)
            val endTime = System.currentTimeMillis()

            // Update session row upon completion
            val completedSession = initialSession.copy(
                endedAt = endTime,
                response = result.transcribedText,
                responseClassification = result.classification,
                responseLatencyMs = result.latencyMs,
                completed = true
            )
            aiConversationRepository.updateSession(completedSession)

            if (result.classification == ResponseClassification.RESPONSIVE) {
                // Short-circuit downgrade to RESOLVED
                incidentStateMachine.transitionTo(
                    targetState = EmergencyState.RESOLVED,
                    reason = "Driver responded clearly: \"${result.transcribedText}\"",
                    driverId = driverId,
                    vehicleId = vehicleId,
                    now = now
                )
                return VerificationOutcome.RESOLVED_RESPONSIVE
            }
        }

        // Exhausted all attempts without responsive driver -> Escalate based on severity
        val targetState = if (severity == AnomalySeverity.CRITICAL || severity == AnomalySeverity.HIGH) {
            EmergencyState.AUTHORITY_ESCALATION
        } else {
            EmergencyState.COMMUNITY_MOBILIZATION
        }

        val reason = "Driver unresponsive after 3 verification attempts. Escalating."
        incidentStateMachine.transitionTo(
            targetState = targetState,
            reason = reason,
            driverId = driverId,
            vehicleId = vehicleId,
            now = now
        )

        return if (targetState == EmergencyState.AUTHORITY_ESCALATION) {
            VerificationOutcome.ESCALATED_AUTHORITY
        } else {
            VerificationOutcome.ESCALATED_COMMUNITY
        }
    }
}
