package com.example.okdrivers.ui.home

import com.example.okdrivers.data.repository.IncidentRepository
import com.example.okdrivers.data.repository.IncidentTimelineRepository
import com.example.okdrivers.data.repository.SensorRepository
import com.example.okdrivers.data.repository.ServiceStateRepository
import com.example.okdrivers.domain.engine.DriverStateEngine
import com.example.okdrivers.domain.engine.IncidentStateMachine
import com.example.okdrivers.domain.engine.LiveAnomalyStatusHolder
import com.example.okdrivers.domain.model.DriverCondition
import com.example.okdrivers.domain.model.DriverState
import com.example.okdrivers.domain.model.EmergencyState
import com.example.okdrivers.domain.model.Incident
import com.example.okdrivers.domain.model.IncidentStateTransition
import com.example.okdrivers.sensors.BatteryStatusSample
import com.example.okdrivers.sensors.DmsSample
import com.example.okdrivers.sensors.GpsLocationSample
import com.example.okdrivers.sensors.MotionSensorSample
import com.example.okdrivers.sensors.NetworkStatusSample
import com.example.okdrivers.sensors.SensorSnapshot
import com.example.okdrivers.sensors.VehicleTelemetrySample
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
class HomeViewModelTest {

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
        override suspend fun saveIncident(incident: Incident) { activeIncidentFlow.value = incident }
    }

    private class FakeTimelineRepository : IncidentTimelineRepository {
        override fun observeTimeline(incidentId: String): Flow<List<IncidentStateTransition>> = flowOf(emptyList())
        override suspend fun saveTransition(transition: IncidentStateTransition) {}
    }

    private class FakeServiceStateRepository : ServiceStateRepository {
        val serviceRunningFlow = MutableStateFlow(true)
        override val isServiceRunningFlow: Flow<Boolean> = serviceRunningFlow
        override suspend fun setServiceRunning(running: Boolean) {
            serviceRunningFlow.value = running
        }
    }

    private class FakeSensorRepository : SensorRepository {
        override fun observeMotion(): Flow<MotionSensorSample> = flowOf()
        override fun observeGps(): Flow<GpsLocationSample> = flowOf()
        override fun observeBattery(): Flow<BatteryStatusSample> = flowOf(BatteryStatusSample(System.currentTimeMillis(), 85, false))
        override fun observeNetwork(): Flow<NetworkStatusSample> = flowOf(NetworkStatusSample(System.currentTimeMillis(), true))
        override fun observeVehicleTelemetry(): Flow<VehicleTelemetrySample> = flowOf()
        override fun observeDms(): Flow<DmsSample> = flowOf()
        override fun observeSensorSnapshot(): Flow<SensorSnapshot> = flowOf(
            SensorSnapshot(
                battery = BatteryStatusSample(System.currentTimeMillis(), 85, false),
                network = NetworkStatusSample(System.currentTimeMillis(), true)
            )
        )
    }

    private class FakeDriverStateEngine : DriverStateEngine {
        override fun process(sample: DmsSample): DriverState {
            return DriverState(
                timestamp = System.currentTimeMillis(),
                attentionScore = 0.95f,
                perclos = 0.05f,
                blinkRate = 15f,
                yawnDetected = false,
                gazeAwayDurationMs = 0L,
                headPitch = 0f, headYaw = 0f, headRoll = 0f,
                gazeDirection = "FORWARD",
                isResponsive = true,
                condition = DriverCondition.ALERT
            )
        }
    }

    @Test
    fun testLiveAnomalyConfidenceAndServiceRunningMapping() = runTest {
        val incidentRepo = FakeIncidentRepository()
        val timelineRepo = FakeTimelineRepository()
        val stateMachine = IncidentStateMachine(incidentRepo, timelineRepo)
        val statusHolder = LiveAnomalyStatusHolder()
        val serviceRepo = FakeServiceStateRepository()
        val sensorRepo = FakeSensorRepository()
        val driverStateEngine = FakeDriverStateEngine()

        val viewModel = HomeViewModel(
            stateMachine,
            sensorRepo,
            driverStateEngine,
            statusHolder,
            serviceRepo
        )

        backgroundScope.launch { viewModel.uiState.collect {} }
        testScheduler.advanceUntilIdle()

        val initialState = viewModel.uiState.value
        assertEquals(0f, initialState.anomalyConfidence, 0.001f)
        assertTrue(initialState.serviceRunning)
        assertEquals(EmergencyState.NORMAL_OPERATION, initialState.emergencyState)

        // Update confidence in holder
        statusHolder.updateConfidence(0.82f)
        testScheduler.advanceUntilIdle()

        assertEquals(0.82f, viewModel.uiState.value.anomalyConfidence, 0.001f)

        // Toggle service running state
        viewModel.setServiceRunning(false)
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.serviceRunning)
    }
}
