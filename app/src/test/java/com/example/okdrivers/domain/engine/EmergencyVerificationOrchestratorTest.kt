package com.example.okdrivers.domain.engine

import com.example.okdrivers.audio.VoiceVerificationResult
import com.example.okdrivers.audio.VoiceVerificationService
import com.example.okdrivers.data.repository.AIConversationRepository
import com.example.okdrivers.data.repository.IncidentRepository
import com.example.okdrivers.data.repository.IncidentTimelineRepository
import com.example.okdrivers.domain.model.AIConversationSession
import com.example.okdrivers.domain.model.AnomalySeverity
import com.example.okdrivers.domain.model.EmergencyState
import com.example.okdrivers.domain.model.Incident
import com.example.okdrivers.domain.model.IncidentStateTransition
import com.example.okdrivers.domain.model.ResponseClassification
import com.example.okdrivers.domain.model.UrgencyLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class EmergencyVerificationOrchestratorTest {

    private class FakeVoiceVerificationService(
        private val classifications: List<ResponseClassification>
    ) : VoiceVerificationService {
        private var callIndex = 0
        override suspend fun verifyVoiceResponse(
            urgency: UrgencyLevel,
            attemptCount: Int,
            timeoutMillis: Long
        ): VoiceVerificationResult {
            val classification = classifications.getOrElse(callIndex) { ResponseClassification.UNRESPONSIVE }
            callIndex++
            return VoiceVerificationResult(
                classification = classification,
                transcribedText = if (classification == ResponseClassification.RESPONSIVE) "I'm okay" else null,
                latencyMs = 200L,
                attemptCount = attemptCount
            )
        }

        override fun getPromptText(urgency: UrgencyLevel): String {
            return when (urgency) {
                UrgencyLevel.INITIAL -> "Emergency detected. Are you okay? Please respond."
                UrgencyLevel.URGENT -> "We still haven't heard from you. Please respond now."
                UrgencyLevel.FINAL -> "No response detected. Emergency services will be notified."
            }
        }
    }

    private class FakeAIConversationRepository : AIConversationRepository {
        val sessions = mutableListOf<AIConversationSession>()
        override suspend fun getForIncident(incidentId: String): List<AIConversationSession> = sessions.filter { it.incidentId == incidentId }
        override fun observeSessions(incidentId: String): Flow<List<AIConversationSession>> = flowOf(sessions.filter { it.incidentId == incidentId })
        override suspend fun saveSession(session: AIConversationSession) {
            sessions.add(session)
        }
        override suspend fun updateSession(session: AIConversationSession) {
            val idx = sessions.indexOfFirst { it.id == session.id }
            if (idx >= 0) {
                sessions[idx] = session
            } else {
                sessions.add(session)
            }
        }
    }

    private class FakeIncidentRepository : IncidentRepository {
        val incidents = mutableListOf<Incident>()
        override fun observeIncidents(): Flow<List<Incident>> = flowOf(incidents)
        override fun observeActiveIncident(): Flow<Incident?> = flowOf(null)
        override suspend fun getIncident(id: String): Incident? = incidents.find { it.id == id }
        override suspend fun saveIncident(incident: Incident) {
            incidents.add(incident)
        }
    }

    private class FakeTimelineRepository : IncidentTimelineRepository {
        val transitions = mutableListOf<IncidentStateTransition>()
        override fun observeTimeline(incidentId: String): Flow<List<IncidentStateTransition>> = flowOf(transitions)
        override suspend fun saveTransition(transition: IncidentStateTransition) {
            transitions.add(transition)
        }
    }

    @Test
    fun testShortCircuitDowngradeOnResponsiveAttempt1() = runBlocking {
        val voiceService = FakeVoiceVerificationService(listOf(ResponseClassification.RESPONSIVE))
        val aiRepo = FakeAIConversationRepository()
        val incidentRepo = FakeIncidentRepository()
        val timelineRepo = FakeTimelineRepository()
        val stateMachine = IncidentStateMachine(incidentRepo, timelineRepo)

        // Set state machine to AI_VERIFICATION first
        stateMachine.transitionTo(EmergencyState.ANOMALY_DETECTION, "Trigger")
        stateMachine.transitionTo(EmergencyState.AI_VERIFICATION, "Verifying")

        val orchestrator = EmergencyVerificationOrchestrator(voiceService, aiRepo, stateMachine)

        val outcome = orchestrator.runVerificationFlow("incident_1", "driver_1", "vehicle_1", AnomalySeverity.MEDIUM)

        assertEquals(VerificationOutcome.RESOLVED_RESPONSIVE, outcome)
        assertEquals(1, aiRepo.sessions.size)
        assertEquals(EmergencyState.RESOLVED, stateMachine.currentState.value)
    }

    @Test
    fun testExhaustionEscalatesToCommunityOrAuthority() = runBlocking {
        val voiceService = FakeVoiceVerificationService(listOf(
            ResponseClassification.UNRESPONSIVE,
            ResponseClassification.IMPAIRED,
            ResponseClassification.UNRESPONSIVE
        ))
        val aiRepo = FakeAIConversationRepository()
        val incidentRepo = FakeIncidentRepository()
        val timelineRepo = FakeTimelineRepository()
        val stateMachine = IncidentStateMachine(incidentRepo, timelineRepo)

        stateMachine.transitionTo(EmergencyState.ANOMALY_DETECTION, "Trigger")
        stateMachine.transitionTo(EmergencyState.AI_VERIFICATION, "Verifying")

        val orchestrator = EmergencyVerificationOrchestrator(voiceService, aiRepo, stateMachine)

        // Critical severity should escalate to Authority
        val outcome = orchestrator.runVerificationFlow("incident_2", "driver_1", "vehicle_1", AnomalySeverity.CRITICAL)

        assertEquals(VerificationOutcome.ESCALATED_AUTHORITY, outcome)
        assertEquals(3, aiRepo.sessions.size) // All 3 attempts persisted
        assertEquals(EmergencyState.AUTHORITY_ESCALATION, stateMachine.currentState.value)
    }
}
