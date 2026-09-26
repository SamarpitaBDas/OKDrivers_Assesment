package com.example.okdrivers.ui.incident

import com.example.okdrivers.audio.VoiceVerificationResult
import com.example.okdrivers.audio.VoiceVerificationService
import com.example.okdrivers.data.repository.AIConversationRepository
import com.example.okdrivers.data.repository.AnomalyRepository
import com.example.okdrivers.data.repository.IncidentRepository
import com.example.okdrivers.data.repository.IncidentTimelineRepository
import com.example.okdrivers.data.repository.ResponderActionRepository
import com.example.okdrivers.data.repository.ResponderRepository
import com.example.okdrivers.domain.community.ResponderMobilizationManager
import com.example.okdrivers.domain.community.ResponderSearchService
import com.example.okdrivers.domain.engine.EmergencyOrchestratorCoordinator
import com.example.okdrivers.domain.engine.EmergencyVerificationOrchestrator
import com.example.okdrivers.domain.engine.IncidentStateMachine
import com.example.okdrivers.domain.model.AIConversationSession
import com.example.okdrivers.domain.model.AnomalyEvent
import com.example.okdrivers.domain.model.AnomalySeverity
import com.example.okdrivers.domain.model.AnomalyType
import com.example.okdrivers.domain.model.EmergencyState
import com.example.okdrivers.domain.model.Incident
import com.example.okdrivers.domain.model.IncidentStateTransition
import com.example.okdrivers.domain.model.Responder
import com.example.okdrivers.domain.model.ResponderAction
import com.example.okdrivers.domain.model.ResponseClassification
import com.example.okdrivers.domain.model.UrgencyLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
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
class EmergencyIncidentViewModelTest {

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

    private class FakeAnomalyRepository : AnomalyRepository {
        val anomalies = mutableMapOf<String, AnomalyEvent>()
        override fun observeAnomalies(): Flow<List<AnomalyEvent>> = flowOf(emptyList())
        override suspend fun getEvent(anomalyId: String): AnomalyEvent? = anomalies[anomalyId]
        override suspend fun saveAnomaly(anomaly: AnomalyEvent) {
            anomalies[anomaly.id] = anomaly
        }
    }

    private class FakeAIConversationRepository : AIConversationRepository {
        val sessions = mutableListOf<AIConversationSession>()
        override suspend fun getForIncident(incidentId: String): List<AIConversationSession> = sessions.filter { it.incidentId == incidentId }
        override fun observeSessions(incidentId: String): Flow<List<AIConversationSession>> = flowOf(sessions.filter { it.incidentId == incidentId })
        override suspend fun saveSession(session: AIConversationSession) {
            sessions.add(session)
        }
    }

    private class FakeResponderActionRepository : ResponderActionRepository {
        val actions = mutableListOf<ResponderAction>()
        override fun observeActions(incidentId: String): Flow<List<ResponderAction>> = flowOf(actions.filter { it.incidentId == incidentId })
        override suspend fun saveAction(action: ResponderAction) {
            actions.add(action)
        }
    }

    private class FakeEmergencyOrchestratorCoordinator(
        stateMachine: IncidentStateMachine,
        verificationOrchestrator: EmergencyVerificationOrchestrator,
        mobilizationManager: ResponderMobilizationManager,
        aiRepo: AIConversationRepository,
        responderActionRepo: ResponderActionRepository,
        responderRepo: ResponderRepository
    ) : EmergencyOrchestratorCoordinator(
        stateMachine,
        verificationOrchestrator,
        mobilizationManager,
        aiRepo,
        responderActionRepo,
        responderRepo
    ) {
        var cancelAndResolveCalled = false
        var lastCancelReason: String? = null

        override suspend fun cancelAndResolve(incidentId: String, reason: String) {
            cancelAndResolveCalled = true
            lastCancelReason = reason
        }
    }

    private class FakeTimelineRepository : IncidentTimelineRepository {
        override fun observeTimeline(incidentId: String): Flow<List<IncidentStateTransition>> = flowOf(emptyList())
        override suspend fun saveTransition(transition: IncidentStateTransition) {}
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
    fun testIdleStateWithoutActiveIncident() = runTest {
        val incidentRepo = FakeIncidentRepository(null)
        val anomalyRepo = FakeAnomalyRepository()
        val aiRepo = FakeAIConversationRepository()
        val responderRepo = FakeResponderActionRepository()
        val timelineRepo = FakeTimelineRepository()
        val stateMachine = IncidentStateMachine(incidentRepo, timelineRepo)
        val voiceService = FakeVoiceVerificationService()
        val verificationOrchestrator = EmergencyVerificationOrchestrator(voiceService, aiRepo, stateMachine)
        val searchService = ResponderSearchService()
        val respRepo = FakeResponderRepository()
        val mobilizationManager = ResponderMobilizationManager(searchService, respRepo, responderRepo, stateMachine)

        val coordinator = FakeEmergencyOrchestratorCoordinator(
            stateMachine, verificationOrchestrator, mobilizationManager, aiRepo, responderRepo, respRepo
        )

        val viewModel = EmergencyIncidentViewModel(incidentRepo, anomalyRepo, aiRepo, responderRepo, coordinator)
        val state = viewModel.uiState.value

        assertFalse(state.hasActiveIncident)
        assertFalse(state.canSelfResolve)
        assertEquals("No active emergency — all systems normal", state.contextualStatusLine)
    }

    @Test
    fun testActiveIncidentInAnomalyDetectionState() = runTest {
        val incident = Incident(
            id = "inc_1",
            driverId = "d_1",
            vehicleId = "v_1",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            latitude = 37.7749,
            longitude = -122.4194,
            severity = AnomalySeverity.HIGH,
            currentState = EmergencyState.ANOMALY_DETECTION,
            anomalyConfidence = 0.82f,
            primaryAnomalyId = "anom_1",
            driverCondition = null,
            communityMobilized = false,
            responderAccepted = false,
            authorityEscalated = false,
            resolvedAt = null
        )

        val incidentRepo = FakeIncidentRepository(incident)
        val anomalyRepo = FakeAnomalyRepository().apply {
            anomalies["anom_1"] = AnomalyEvent(
                id = "anom_1",
                timestamp = System.currentTimeMillis(),
                type = AnomalyType.HARD_BRAKING,
                severity = AnomalySeverity.HIGH,
                confidence = 0.82f,
                reason = "Sudden braking 0.95G",
                latitude = 37.7749,
                longitude = -122.4194,
                driverId = "d_1",
                vehicleId = "v_1",
                sensorSampleTimestamp = System.currentTimeMillis(),
                telemetryTimestamp = System.currentTimeMillis(),
                requiresVerification = true,
                isEscalated = false
            )
        }
        val aiRepo = FakeAIConversationRepository()
        val responderRepo = FakeResponderActionRepository()
        val timelineRepo = FakeTimelineRepository()
        val stateMachine = IncidentStateMachine(incidentRepo, timelineRepo)
        val voiceService = FakeVoiceVerificationService()
        val verificationOrchestrator = EmergencyVerificationOrchestrator(voiceService, aiRepo, stateMachine)
        val searchService = ResponderSearchService()
        val respRepo = FakeResponderRepository()
        val mobilizationManager = ResponderMobilizationManager(searchService, respRepo, responderRepo, stateMachine)

        val coordinator = FakeEmergencyOrchestratorCoordinator(
            stateMachine, verificationOrchestrator, mobilizationManager, aiRepo, responderRepo, respRepo
        )

        val viewModel = EmergencyIncidentViewModel(incidentRepo, anomalyRepo, aiRepo, responderRepo, coordinator)
        backgroundScope.launch { viewModel.uiState.collect {} }
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value

        assertTrue(state.hasActiveIncident)
        assertTrue(state.canSelfResolve)
        assertEquals(EmergencyState.ANOMALY_DETECTION, state.emergencyState)
        assertEquals(AnomalySeverity.HIGH, state.severity)
        assertEquals("82%", state.formattedConfidence)
        assertEquals("Sudden braking 0.95G", state.triggerReason)
        assertEquals("Anomaly detected — evaluating severity", state.contextualStatusLine)
    }

    @Test
    fun testSelfResolveGatingTable() {
        runTest {
            val canResolveMap = mapOf(
                EmergencyState.ANOMALY_DETECTION to true,
                EmergencyState.AI_VERIFICATION to true,
                EmergencyState.COMMUNITY_MOBILIZATION to true,
                EmergencyState.COMMUNITY_RESPONSE to true,
                EmergencyState.AUTHORITY_ESCALATION to false,
                EmergencyState.RESOLVED to false,
                EmergencyState.NORMAL_OPERATION to false
            )

            val aiRepo = FakeAIConversationRepository()
            val responderRepo = FakeResponderActionRepository()
            val timelineRepo = FakeTimelineRepository()
            val searchService = ResponderSearchService()
            val respRepo = FakeResponderRepository()
            val voiceService = FakeVoiceVerificationService()

            for ((state, expectedCanResolve) in canResolveMap) {
                val incident = Incident(
                    id = "inc_test",
                    driverId = "d_1",
                    vehicleId = "v_1",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    latitude = null,
                    longitude = null,
                    severity = AnomalySeverity.MEDIUM,
                    currentState = state,
                    anomalyConfidence = 0.5f,
                    primaryAnomalyId = null,
                    driverCondition = null,
                    communityMobilized = false,
                    responderAccepted = false,
                    authorityEscalated = false,
                    resolvedAt = null
                )
                val incidentRepo = FakeIncidentRepository(incident)
                val stateMachine = IncidentStateMachine(incidentRepo, timelineRepo)
                val verificationOrchestrator = EmergencyVerificationOrchestrator(voiceService, aiRepo, stateMachine)
                val mobilizationManager = ResponderMobilizationManager(searchService, respRepo, responderRepo, stateMachine)
                val coordinator = FakeEmergencyOrchestratorCoordinator(
                    stateMachine, verificationOrchestrator, mobilizationManager, aiRepo, responderRepo, respRepo
                )

                val viewModel = EmergencyIncidentViewModel(
                    incidentRepo,
                    FakeAnomalyRepository(),
                    aiRepo,
                    responderRepo,
                    coordinator
                )
                backgroundScope.launch { viewModel.uiState.collect {} }
                testScheduler.advanceUntilIdle()
                val uiState = viewModel.uiState.value
                if (state == EmergencyState.NORMAL_OPERATION || state == EmergencyState.RESOLVED) {
                    assertFalse("Expected canSelfResolve = false for state $state", uiState.canSelfResolve)
                } else {
                    assertEquals("Expected canSelfResolve = $expectedCanResolve for state $state", expectedCanResolve, uiState.canSelfResolve)
                }
            }
        }
    }

    @Test
    fun testSelfResolveActionWhenAllowed() = runTest {
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
        val responderRepo = FakeResponderActionRepository()
        val timelineRepo = FakeTimelineRepository()
        val stateMachine = IncidentStateMachine(incidentRepo, timelineRepo)
        val voiceService = FakeVoiceVerificationService()
        val verificationOrchestrator = EmergencyVerificationOrchestrator(voiceService, aiRepo, stateMachine)
        val searchService = ResponderSearchService()
        val respRepo = FakeResponderRepository()
        val mobilizationManager = ResponderMobilizationManager(searchService, respRepo, responderRepo, stateMachine)

        val coordinator = FakeEmergencyOrchestratorCoordinator(
            stateMachine, verificationOrchestrator, mobilizationManager, aiRepo, responderRepo, respRepo
        )

        val viewModel = EmergencyIncidentViewModel(
            incidentRepo,
            FakeAnomalyRepository(),
            aiRepo,
            responderRepo,
            coordinator
        )
        backgroundScope.launch { viewModel.uiState.collect {} }
        testScheduler.advanceUntilIdle()

        viewModel.selfResolve()
        testScheduler.advanceUntilIdle()

        assertTrue(coordinator.cancelAndResolveCalled)
        assertEquals("Manually cancelled by driver", coordinator.lastCancelReason)
    }
}
