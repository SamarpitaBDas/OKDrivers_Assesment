package com.example.okdrivers.ui.analytics

import com.example.okdrivers.data.repository.BaselineRepository
import com.example.okdrivers.data.repository.CurrentProfileRepository
import com.example.okdrivers.data.repository.SensorRepository
import com.example.okdrivers.data.repository.TelemetryRepository
import com.example.okdrivers.domain.model.SafetyBaseline
import com.example.okdrivers.domain.model.VehicleTelemetry
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
class BaselineAnalyticsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeBaselineRepository : BaselineRepository {
        val baselines = mutableMapOf<String, SafetyBaseline>()
        val baselineFlow = MutableStateFlow<SafetyBaseline?>(null)

        override suspend fun getBaseline(driverId: String, vehicleId: String): SafetyBaseline? = baselines["${driverId}_$vehicleId"]
        override fun observeBaseline(driverId: String, vehicleId: String): Flow<SafetyBaseline?> = baselineFlow
        override suspend fun saveBaseline(baseline: SafetyBaseline) {
            baselines["${baseline.driverId}_${baseline.vehicleId}"] = baseline
            baselineFlow.value = baseline
        }
    }

    private class FakeCurrentProfileRepository : CurrentProfileRepository {
        val driverIdFlow = MutableStateFlow("driver_1")
        val vehicleIdFlow = MutableStateFlow("vehicle_1")

        override fun observeCurrentDriverId(): Flow<String> = driverIdFlow
        override fun observeCurrentVehicleId(): Flow<String> = vehicleIdFlow
        override suspend fun setCurrentDriverId(driverId: String) { driverIdFlow.value = driverId }
        override suspend fun setCurrentVehicleId(vehicleId: String) { vehicleIdFlow.value = vehicleId }
    }

    private class FakeSensorRepository : SensorRepository {
        val motionFlow = MutableStateFlow(
            MotionSensorSample(
                timestamp = System.currentTimeMillis(),
                accelerationX = 0f, accelerationY = 0f, accelerationZ = 9.8f,
                gForce = 0.22f,
                gyroX = 0f, gyroY = 0f, gyroZ = 0f
            )
        )
        override fun observeMotion(): Flow<MotionSensorSample> = motionFlow
        override fun observeGps(): Flow<GpsLocationSample> = flowOf()
        override fun observeBattery(): Flow<BatteryStatusSample> = flowOf()
        override fun observeNetwork(): Flow<NetworkStatusSample> = flowOf()
        override fun observeVehicleTelemetry(): Flow<VehicleTelemetrySample> = flowOf()
        override fun observeDms(): Flow<DmsSample> = flowOf()
        override fun observeSensorSnapshot(): Flow<SensorSnapshot> = flowOf()
    }

    private class FakeTelemetryRepository : TelemetryRepository {
        val telemetryFlow = MutableStateFlow(
            listOf(
                VehicleTelemetry(
                    timestamp = System.currentTimeMillis(),
                    speedKmh = 42.5f,
                    rpm = 2100,
                    engineLoad = 35.0f,
                    throttlePosition = 20.0f,
                    engineTemperature = 90.0f,
                    batteryVoltage = 13.8f,
                    diagnosticFault = null
                )
            )
        )
        override fun observeTelemetry(): Flow<List<VehicleTelemetry>> = telemetryFlow
        override suspend fun getRecentTelemetry(limit: Int): List<VehicleTelemetry> = telemetryFlow.value
        override suspend fun saveTelemetry(telemetry: VehicleTelemetry) {}
    }

    @Test
    fun testSeedingAndBaselineObservation() = runTest {
        val baselineRepo = FakeBaselineRepository()
        val profileRepo = FakeCurrentProfileRepository()
        val sensorRepo = FakeSensorRepository()
        val telemetryRepo = FakeTelemetryRepository()

        val viewModel = BaselineAnalyticsViewModel(baselineRepo, profileRepo, sensorRepo, telemetryRepo)
        backgroundScope.launch { viewModel.uiState.collect {} }
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.hasBaseline)
        assertEquals(28, state.sampleCount)
        assertEquals(42.5f, state.averageSpeedKmh ?: 0f, 0.1f)
        assertEquals(2100, state.averageRpm ?: 0)
        assertEquals(35.0f, state.averageEngineLoad ?: 0f, 0.1f)
        assertEquals(0.22f, state.averageBrakingG ?: 0f, 0.01f)
    }

    @Test
    fun testStillLearningStateWhenSampleCountIsLow() = runTest {
        val baselineRepo = FakeBaselineRepository()
        val profileRepo = FakeCurrentProfileRepository()
        val sensorRepo = FakeSensorRepository()
        val telemetryRepo = FakeTelemetryRepository()

        val lowSampleBaseline = SafetyBaseline(
            id = "baseline_low",
            driverId = "driver_1",
            vehicleId = "vehicle_1",
            averageSpeedKmh = 30f,
            averageRpm = 1500,
            averageEngineLoad = 20f,
            averageBrakingG = 0.15f,
            maximumNormalGForce = 0.30f,
            sampleCount = 4,
            confidence = 0.08f,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        baselineRepo.saveBaseline(lowSampleBaseline)

        val viewModel = BaselineAnalyticsViewModel(baselineRepo, profileRepo, sensorRepo, telemetryRepo)
        backgroundScope.launch { viewModel.uiState.collect {} }
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.hasBaseline)
        assertEquals(4, state.sampleCount)
    }

    @Test
    fun testLiveDeviationCalculationWhenMotionSampleDeviates() = runTest {
        val baselineRepo = FakeBaselineRepository()
        val profileRepo = FakeCurrentProfileRepository()
        val sensorRepo = FakeSensorRepository()
        val telemetryRepo = FakeTelemetryRepository()

        val viewModel = BaselineAnalyticsViewModel(baselineRepo, profileRepo, sensorRepo, telemetryRepo)
        backgroundScope.launch { viewModel.uiState.collect {} }
        testScheduler.advanceUntilIdle()

        // Nudge live gForce sample to an elevated value (0.80 G)
        sensorRepo.motionFlow.value = MotionSensorSample(
            timestamp = System.currentTimeMillis(),
            accelerationX = 0f, accelerationY = 0f, accelerationZ = 9.8f,
            gForce = 0.80f,
            gyroX = 0f, gyroY = 0f, gyroZ = 0f
        )
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.hasBaseline)
        assertTrue(state.isCurrentlyOutsideNormalRange)
        assertTrue((state.currentGForceDeviation ?: 0f) > 2.0f)
    }
}
