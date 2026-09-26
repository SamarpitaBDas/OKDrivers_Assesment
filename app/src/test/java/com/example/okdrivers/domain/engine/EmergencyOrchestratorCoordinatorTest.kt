package com.example.okdrivers.domain.engine

import com.example.okdrivers.audio.VoiceVerificationResult
import com.example.okdrivers.audio.VoiceVerificationService
import com.example.okdrivers.data.repository.AIConversationRepository
import com.example.okdrivers.data.repository.IncidentRepository
import com.example.okdrivers.data.repository.IncidentTimelineRepository
import com.example.okdrivers.data.repository.ResponderActionRepository
import com.example.okdrivers.data.repository.ResponderRepository
import com.example.okdrivers.domain.community.ResponderMobilizationManager
import com.example.okdrivers.domain.community.ResponderSearchService
import com.example.okdrivers.domain.model.AIConversationSession
import com.example.okdrivers.domain.model.EmergencyState
import com.example.okdrivers.domain.model.Incident
import com.example.okdrivers.domain.model.IncidentStateTransition
import com.example.okdrivers.domain.model.Responder
import com.example.okdrivers.domain.model.ResponderAction
import com.example.okdrivers.domain.model.ResponderActionType
import com.example.okdrivers.domain.model.ResponseClassification
import com.example.okdrivers.domain.model.UrgencyLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EmergencyOrchestratorCoordinatorTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeIncidentRepository : IncidentRepository {
        val activeIncidentFlow = MutableStateFlow<Incident?>(null)
        override fun observeIncidents(): Flow<List<Incident>> = flowOf(emptyList())
        override fun observeActiveIncident(): Flow<Incident?> = activeIncidentFlow
        override suspend fun getIncident(id: String): Incident? = activeIncidentFlow.value
        override suspend fun saveIncident(incident: Incident) {
            activeIncidentFlow.value = incident
        }
    }

    private class FakeTimelineRepository : IncidentTimelineRepository {
        val transitions = mutableListOf<IncidentStateTransition>()
        override fun observeTimeline(incidentId: String): Flow<List<IncidentStateTransition>> = flowOf(transitions)
        override suspend fun saveTransition(transition: IncidentStateTransition) {
            transitions.add(transition)
        }
    }

    private class FakeAIConversationRepository : AIConversationRepository {
        val sessions = mutableListOf<AIConversationSession>()
        override suspend fun getForIncident(incidentId: String): List<AIConversationSession> = sessions.filter { it.incidentId == incidentId }
        override fun observeSessions(incidentId: String): Flow<List<AIConversationSession>> = flowOf(sessions.filter { it.incidentId == incidentId })
        override suspend fun saveSession(session: AIConversationSession) {
            val idx = sessions.indexOfFirst { it.id == session.id }
            if (idx >= 0) {
                sessions[idx] = session
            } else {
                sessions.add(session)
            }
        }
        override suspend fun updateSession(session: AIConversationSession) {
            saveSession(session)
        }
    }

    private class FakeResponderActionRepository : ResponderActionRepository {
        val actions = mutableListOf<ResponderAction>()
        override fun observeActions(incidentId: String): Flow<List<ResponderAction>> = flowOf(actions.filter { it.incidentId == incidentId })
        override suspend fun saveAction(action: ResponderAction) {
            actions.add(action)
        }
    }

    private class FakeVoiceVerificationService : VoiceVerificationService {
        override suspend fun verifyVoiceResponse(urgency: UrgencyLevel, attemptCount: Int, timeoutMillis: Long): VoiceVerificationResult {
            return VoiceVerificationResult(ResponseClassification.UNRESPONSIVE, "", 0L, attemptCount)
        }
        override fun getPromptText(urgency: UrgencyLevel): String = ""
    }

    private class FakeResponderRepository : ResponderRepository {
        override fun observeActiveResponders(): Flow<List<Responder>> = flowOf(emptyList())
        override suspend fun saveResponder(responder: Responder) {}
    }

    @Test
    fun testCancelAndResolveCleansUpDanglingRowsAndTransitionsState() = runTest {
        val incidentRepo = FakeIncidentRepository()
        val timelineRepo = FakeTimelineRepository()
        val stateMachine = IncidentStateMachine(incidentRepo, timelineRepo)
        val aiRepo = FakeAIConversationRepository()
        val responderActionRepo = FakeResponderActionRepository()
        val voiceService = FakeVoiceVerificationService()
        val verificationOrchestrator = EmergencyVerificationOrchestrator(voiceService, aiRepo, stateMachine)
        val searchService = ResponderSearchService()
        val respRepo = FakeResponderRepository()
        val mobilizationManager = ResponderMobilizationManager(searchService, respRepo, responderActionRepo, stateMachine)

        // Setup an in-flight incomplete AI session
        val session = AIConversationSession(
            id = "sess_1",
            incidentId = "inc_1",
            startedAt = System.currentTimeMillis(),
            endedAt = null,
            prompt = "Prompt",
            response = null,
            responseClassification = null,
            responseLatencyMs = null,
            attemptCount = 1,
            completed = false
        )
        aiRepo.saveSession(session)

        // Setup active incident state in state machine
        stateMachine.transitionTo(EmergencyState.ANOMALY_DETECTION, "Trigger", now = System.currentTimeMillis())
        stateMachine.transitionTo(EmergencyState.AI_VERIFICATION, "Verifying", now = System.currentTimeMillis())

        val coordinator = EmergencyOrchestratorCoordinator(
            stateMachine, verificationOrchestrator, mobilizationManager, aiRepo, responderActionRepo, respRepo
        )

        coordinator.cancelAndResolve("inc_1", "Manually cancelled by driver")
        testScheduler.advanceUntilIdle()

        // Verify state machine transitioned to RESOLVED
        assertEquals(EmergencyState.RESOLVED, stateMachine.currentState.value)

        // Verify transition reason logged in timeline
        val lastTransition = timelineRepo.transitions.last()
        assertEquals(EmergencyState.RESOLVED, lastTransition.toState)
        assertEquals("Manually cancelled by driver", lastTransition.reason)

        // Verify dangling session marked as completed
        val updatedSession = aiRepo.getForIncident("inc_1").first()
        assertTrue(updatedSession.completed)

        // Verify system cancellation action recorded
        val actions = responderActionRepo.actions.filter { it.incidentId == "inc_1" }
        assertTrue(actions.any { it.action == ResponderActionType.DECLINED && it.responderId == "system_cancellation" })
    }
}
