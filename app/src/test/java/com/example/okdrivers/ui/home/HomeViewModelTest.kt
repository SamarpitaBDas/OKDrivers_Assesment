package com.example.okdrivers.ui.home

import com.example.okdrivers.data.repository.IncidentRepository
import com.example.okdrivers.data.repository.IncidentTimelineRepository
import com.example.okdrivers.data.repository.ServiceStateRepository
import com.example.okdrivers.domain.engine.IncidentStateMachine
import com.example.okdrivers.domain.engine.LiveAnomalyStatusHolder
import com.example.okdrivers.domain.model.EmergencyState
import com.example.okdrivers.domain.model.Incident
import com.example.okdrivers.domain.model.IncidentStateTransition
import com.example.okdrivers.sensors.BatteryStatusManager
import com.example.okdrivers.sensors.BatteryStatusSample
import com.example.okdrivers.sensors.NetworkStatusManager
import com.example.okdrivers.sensors.NetworkStatusSample
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

    private class FakeNetworkStatusManager : NetworkStatusManager(FakeContext()) {
        val networkFlow = MutableStateFlow(NetworkStatusSample(System.currentTimeMillis(), isOnline = true))
        override fun observeNetwork(): Flow<NetworkStatusSample> = networkFlow
    }

    private class FakeBatteryStatusManager : BatteryStatusManager(FakeContext()) {
        val batteryFlow = MutableStateFlow(BatteryStatusSample(System.currentTimeMillis(), batteryPercentage = 85, isCharging = false))
        override fun observeBattery(): Flow<BatteryStatusSample> = batteryFlow
    }

    private class FakeServiceStateRepository : ServiceStateRepository {
        val serviceRunningFlow = MutableStateFlow(true)
        override val isServiceRunningFlow: Flow<Boolean> = serviceRunningFlow
        override suspend fun setServiceRunning(running: Boolean) {
            serviceRunningFlow.value = running
        }
    }

    private class FakeContext : android.content.ContextWrapper(null)

    @Test
    fun testLiveAnomalyConfidenceAndServiceRunningMapping() = runTest {
        val incidentRepo = FakeIncidentRepository()
        val timelineRepo = FakeTimelineRepository()
        val stateMachine = IncidentStateMachine(incidentRepo, timelineRepo)
        val networkManager = FakeNetworkStatusManager()
        val batteryManager = FakeBatteryStatusManager()
        val statusHolder = LiveAnomalyStatusHolder()
        val serviceRepo = FakeServiceStateRepository()

        val viewModel = HomeViewModel(
            stateMachine,
            networkManager,
            batteryManager,
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
