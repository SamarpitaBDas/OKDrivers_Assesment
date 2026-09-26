package com.example.okdrivers.ui.testcontrols

import com.example.okdrivers.audio.DemoVoiceResponseController
import com.example.okdrivers.data.repository.IncidentRepository
import com.example.okdrivers.data.repository.IncidentTimelineRepository
import com.example.okdrivers.data.repository.ResponderActionRepository
import com.example.okdrivers.domain.engine.IncidentStateMachine
import com.example.okdrivers.domain.model.EmergencyState
import com.example.okdrivers.domain.model.Incident
import com.example.okdrivers.domain.model.IncidentStateTransition
import com.example.okdrivers.domain.model.ResponderAction
import com.example.okdrivers.domain.model.ResponderActionType
import com.example.okdrivers.sensors.DmsSimulator
import com.example.okdrivers.sensors.MotionSensorManager
import com.example.okdrivers.sensors.MotionSensorSample
import com.example.okdrivers.sensors.VehicleTelemetrySimulator
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TestControlsViewModelTest {

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
        override fun observeTimeline(incidentId: String): Flow<List<IncidentStateTransition>> = flowOf(emptyList())
        override suspend fun saveTransition(transition: IncidentStateTransition) {}
    }

    private class FakeResponderActionRepository : ResponderActionRepository {
        val actions = mutableListOf<ResponderAction>()
        override fun observeActions(incidentId: String): Flow<List<ResponderAction>> = flowOf(actions.filter { it.incidentId == incidentId })
        override suspend fun saveAction(action: ResponderAction) {
            actions.add(action)
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

    private class FakeContext : android.content.ContextWrapper(null)

    private class FakeMotionSensorManager : MotionSensorManager(FakeContext()) {
        override fun observeMotion(samplingPeriodUs: Int): Flow<MotionSensorSample> = flowOf()
    }

    @Test
    fun testAllScenarioTriggers() = runTest {
        val motionSensorManager = FakeMotionSensorManager()
        val vehicleTelemetrySimulator = VehicleTelemetrySimulator()
        val dmsSimulator = DmsSimulator()
        val demoController = FakeDemoVoiceResponseController()
        val responderActionRepo = FakeResponderActionRepository()
        val incidentRepo = FakeIncidentRepository()
        val timelineRepo = FakeTimelineRepository()
        val stateMachine = IncidentStateMachine(incidentRepo, timelineRepo)

        val viewModel = TestControlsViewModel(
            motionSensorManager,
            vehicleTelemetrySimulator,
            dmsSimulator,
            demoController,
            responderActionRepo,
            stateMachine,
            incidentRepo
        )

        backgroundScope.launch { viewModel.uiState.collect {} }
        testScheduler.advanceUntilIdle()

        // 1. Hard braking trigger
        viewModel.triggerHardBraking()
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.activeScenarioName.contains("Hard Brake"))

        // 2. Suspected accident trigger
        viewModel.triggerSuspectedAccident()
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.activeScenarioName.contains("Suspected Accident"))

        // 3. Driver responds trigger
        viewModel.triggerDriverResponds()
        testScheduler.advanceUntilIdle()
        assertEquals("I'm okay", demoController.nextResponse)
        assertTrue(demoController.isForceSimulatedMode())

        // 4. Driver silent trigger
        viewModel.triggerDriverSilent()
        testScheduler.advanceUntilIdle()
        assertNull(demoController.nextResponse)

        // 5. Responder accepts trigger
        viewModel.triggerResponderAccepts()
        testScheduler.advanceUntilIdle()
        assertTrue(responderActionRepo.actions.any { it.action == ResponderActionType.ACCEPTED })

        // 6. Airbag event trigger
        viewModel.triggerAirbagEvent()
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.activeScenarioName.contains("Airbag"))

        // 7. Normal driving reset
        viewModel.triggerNormalDriving()
        testScheduler.advanceUntilIdle()
        assertEquals(EmergencyState.NORMAL_OPERATION, viewModel.uiState.value.currentEmergencyState)
    }
}
