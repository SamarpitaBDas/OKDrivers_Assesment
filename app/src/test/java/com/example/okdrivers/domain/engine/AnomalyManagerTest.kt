package com.example.okdrivers.domain.engine

import com.example.okdrivers.data.repository.AnomalyRepository
import com.example.okdrivers.domain.model.AnomalyEvent
import com.example.okdrivers.sensors.MotionSensorSample
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AnomalyManagerTest {

    private class FakeAnomalyRepository : AnomalyRepository {
        val savedEvents = mutableListOf<AnomalyEvent>()
        override fun observeAnomalies(): Flow<List<AnomalyEvent>> = flowOf(savedEvents)
        override suspend fun saveAnomaly(anomaly: AnomalyEvent) {
            savedEvents.add(anomaly)
        }
    }

    private val anomalyConfidenceEngine = AnomalyConfidenceEngine()
    private val anomalyEngine = AnomalyEngine(anomalyConfidenceEngine)

    private val sampleDriverBaseline = BaselineStatistics(
        sampleCount = 50,
        averageSpeedKmh = 50f,
        minimumSpeedKmh = 0f,
        maximumSpeedKmh = 100f,
        speedStdDev = 5f,
        averageGForce = 1.0f,
        minimumGForce = 0.9f,
        maximumGForce = 1.2f,
        gForceStdDev = 0.05f,
        averageRpm = 2000f,
        minimumRpm = 800f,
        maximumRpm = 4000f,
        rpmStdDev = 200f,
        averageEngineLoad = 30f,
        minimumEngineLoad = 10f,
        maximumEngineLoad = 80f,
        engineLoadStdDev = 5f,
        updatedAt = System.currentTimeMillis()
    )

    @Test
    fun testPersistsEveryAnomalyEventIncludingNonEscalating08GBraking() = runBlocking {
        val fakeRepository = FakeAnomalyRepository()
        val anomalyManager = AnomalyManager(anomalyEngine, fakeRepository)

        val motion = MotionSensorSample(
            timestamp = System.currentTimeMillis(),
            accelerationX = 0f,
            accelerationY = -7.8f,
            accelerationZ = 5.0f,
            gForce = 0.8f, // 0.8G braking (non-escalating)
            gyroX = 0f,
            gyroY = 0f,
            gyroZ = 0f
        )

        val result = anomalyManager.evaluateAndPersist(
            driverId = "driver_1",
            vehicleId = "vehicle_1",
            vehicleTelemetry = null,
            driverState = null,
            motionSensor = motion,
            gpsLocation = null,
            driverBaseline = sampleDriverBaseline,
            vehicleBaseline = null
        )

        assertFalse(result.requiresVerification)
        assertFalse(result.isEscalated)
        assertTrue(result.events.isNotEmpty())

        // Verify that even though it did not require verification or escalation, it was saved to the repository
        assertEquals(1, fakeRepository.savedEvents.size)
        assertEquals(result.events.first().id, fakeRepository.savedEvents.first().id)
    }
}
