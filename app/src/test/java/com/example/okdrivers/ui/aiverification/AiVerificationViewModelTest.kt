package com.example.okdrivers.ui.aiverification

import com.example.okdrivers.audio.DemoVoiceResponseController
import com.example.okdrivers.audio.VoiceVerificationResult
import com.example.okdrivers.audio.VoiceVerificationService
import com.example.okdrivers.data.repository.AIConversationRepository
import com.example.okdrivers.data.repository.IncidentRepository
import com.example.okdrivers.domain.model.AIConversationSession
import com.example.okdrivers.domain.model.AnomalySeverity
import com.example.okdrivers.domain.model.EmergencyState
import com.example.okdrivers.domain.model.Incident
import com.example.okdrivers.domain.model.ResponseClassification
import com.example.okdrivers.domain.model.UrgencyLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiVerificationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeIncidentRepository(
        activeIncident: Incident? = null
    ) : IncidentRepository {
        val activeIncidentFlow = MutableStateFlow(activeIncident)
        override fun observeIncidents(): Flow<List<Incident>> = flowOf(emptyList())
        override fun observeActiveIncident(): Flow<Incident?> = activeIncidentFlow
        override suspend fun getIncident(id: String): Incident? = activeIncidentFlow.value
        override suspend fun saveIncident(incident: Incident) {
            activeIncidentFlow.value = incident
        }
    }

    private class FakeAIConversationRepository : AIConversationRepository {
        val sessions = mutableListOf<AIConversationSession>()
        val sessionsFlow = MutableStateFlow<List<AIConversationSession>>(emptyList())

        override suspend fun getForIncident(incidentId: String): List<AIConversationSession> = sessions.filter { it.incidentId == incidentId }
        override fun observeSessions(incidentId: String): Flow<List<AIConversationSession>> = sessionsFlow
        override suspend fun saveSession(session: AIConversationSession) {
            sessions.add(session)
            sessionsFlow.value = sessions.toList()
        }
        override suspend fun updateSession(session: AIConversationSession) {
            val idx = sessions.indexOfFirst { it.id == session.id }
            if (idx >= 0) {
                sessions[idx] = session
            } else {
                sessions.add(session)
            }
            sessionsFlow.value = sessions.toList()
        }
    }

    private class FakeDemoVoiceResponseController : DemoVoiceResponseController {
        var nextResponse: String? = null
        var forceSimulated: Boolean = false

        override fun setNextSimulatedResponse(transcript: String?) {
            nextResponse = transcript
        }

        override fun setForceSimulatedMode(enabled: Boolean) {
            forceSimulated = enabled
        }

        override fun isForceSimulatedMode(): Boolean = forceSimulated
    }

    private class TestVoiceVerificationRouter(
        private val demoController: FakeDemoVoiceResponseController
    ) : com.example.okdrivers.audio.VoiceVerificationRouter(
        context = FakeContext(),
        realService = FakeRealService(),
        simulatedService = FakeSimulatedService()
    ) {
        override fun shouldUseRealService(): Boolean {
            return !demoController.isForceSimulatedMode()
        }

        override fun getPromptText(urgency: UrgencyLevel): String {
            return when (urgency) {
                UrgencyLevel.INITIAL -> "Emergency detected. Are you okay? Please respond."
                UrgencyLevel.URGENT -> "We still haven't heard from you. Please respond now."
                UrgencyLevel.FINAL -> "No response detected. Emergency services will be notified."
            }
        }
    }

    private class FakeContext : android.content.ContextWrapper(null)
    private class FakeRealService : com.example.okdrivers.audio.RealVoiceVerificationService(
        FakeContext(),
        com.example.okdrivers.audio.EmergencyTtsManager(FakeContext()),
        Dispatchers.Main
    )
    private class FakeSimulatedService : com.example.okdrivers.audio.SimulatedVoiceVerificationService(
        com.example.okdrivers.audio.EmergencyTtsManager(FakeContext())
    )

    @Test
    fun testIdleStateWithoutActiveIncident() = runTest {
        val incidentRepo = FakeIncidentRepository(null)
        val aiRepo = FakeAIConversationRepository()
        val demoController = FakeDemoVoiceResponseController()
        val router = TestVoiceVerificationRouter(demoController)

        val viewModel = AiVerificationViewModel(incidentRepo, aiRepo, router, demoController)
        backgroundScope.launch { viewModel.uiState.collect {} }
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.hasActiveIncident)
        assertEquals("No active AI verification session — system operating normally", state.verificationStatusText)
    }

    @Test
    fun testLiveInProgressAndCompletedSessionState() = runTest {
        val incident = Incident(
            id = "inc_1",
            driverId = "d_1",
            vehicleId = "v_1",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            latitude = 37.7749,
            longitude = -122.4194,
            severity = AnomalySeverity.HIGH,
            currentState = EmergencyState.AI_VERIFICATION,
            anomalyConfidence = 0.85f,
            primaryAnomalyId = null,
            driverCondition = null,
            communityMobilized = false,
            responderAccepted = false,
            authorityEscalated = false,
            resolvedAt = null
        )

        val incidentRepo = FakeIncidentRepository(incident)
        val aiRepo = FakeAIConversationRepository()
        val demoController = FakeDemoVoiceResponseController()
        val router = TestVoiceVerificationRouter(demoController)

        // 1. In-flight session (Option A: completed = false)
        val inFlightSession = AIConversationSession(
            id = "sess_1",
            incidentId = "inc_1",
            startedAt = System.currentTimeMillis(),
            endedAt = null,
            prompt = "Emergency detected. Are you okay? Please respond.",
            response = null,
            responseClassification = null,
            responseLatencyMs = null,
            attemptCount = 1,
            completed = false
        )
        aiRepo.saveSession(inFlightSession)

        val viewModel = AiVerificationViewModel(incidentRepo, aiRepo, router, demoController)
        backgroundScope.launch { viewModel.uiState.collect {} }
        testScheduler.advanceUntilIdle()

        var state = viewModel.uiState.value
        assertTrue(state.hasActiveIncident)
        assertTrue(state.isSessionInProgress)
        assertEquals(UrgencyLevel.INITIAL, state.currentUrgencyLevel)
        assertEquals("Emergency detected. Are you okay? Please respond.", state.activePromptText)
        assertTrue(state.verificationStatusText.contains("Attempt 1 in progress"))

        // 2. Completed session
        val completedSession = inFlightSession.copy(
            endedAt = System.currentTimeMillis(),
            response = "I'm okay",
            responseClassification = ResponseClassification.RESPONSIVE,
            responseLatencyMs = 280L,
            completed = true
        )
        aiRepo.updateSession(completedSession)
        testScheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertFalse(state.isSessionInProgress)
        assertEquals("I'm okay", state.latestTranscript)
        assertEquals(ResponseClassification.RESPONSIVE, state.latestClassification)
        assertEquals(280L, state.latestLatencyMs)
        assertTrue(state.verificationStatusText.contains("Attempt 1 completed"))
    }

    @Test
    fun testDemoControls() = runTest {
        val incidentRepo = FakeIncidentRepository(null)
        val aiRepo = FakeAIConversationRepository()
        val demoController = FakeDemoVoiceResponseController()
        val router = TestVoiceVerificationRouter(demoController)

        val viewModel = AiVerificationViewModel(incidentRepo, aiRepo, router, demoController)
        backgroundScope.launch { viewModel.uiState.collect {} }
        testScheduler.advanceUntilIdle()

        // Toggle force simulated mode
        viewModel.toggleForceSimulatedMode(true)
        testScheduler.advanceUntilIdle()

        assertTrue(demoController.isForceSimulatedMode())
        assertTrue(viewModel.uiState.value.isForceSimulatedMode)

        // Set next simulated response
        viewModel.setSimulatedResponse("I need help")
        assertEquals("I need help", demoController.nextResponse)
    }
}
