package com.example.okdrivers.domain.engine

import com.example.okdrivers.data.repository.AnomalyRepository
import com.example.okdrivers.data.repository.IncidentRepository
import com.example.okdrivers.data.repository.IncidentTimelineRepository
import com.example.okdrivers.domain.model.AnomalyEvent
import com.example.okdrivers.domain.model.Incident
import com.example.okdrivers.domain.model.IncidentStateTransition
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
        override suspend fun getEvent(anomalyId: String): AnomalyEvent? = savedEvents.find { it.id == anomalyId }
        override suspend fun saveAnomaly(anomaly: AnomalyEvent) {
            savedEvents.add(anomaly)
        }
    }

    private class FakeIncidentRepository : IncidentRepository {
        val savedIncidents = mutableListOf<Incident>()
        override fun observeIncidents(): Flow<List<Incident>> = flowOf(savedIncidents)
        override fun observeActiveIncident(): Flow<Incident?> = flowOf(null)
        override suspend fun getIncident(id: String): Incident? = savedIncidents.find { it.id == id }
        override suspend fun saveIncident(incident: Incident) {
            savedIncidents.add(incident)
        }
    }

    private class FakeTimelineRepository : IncidentTimelineRepository {
        val savedTransitions = mutableListOf<IncidentStateTransition>()
        override fun observeTimeline(incidentId: String): Flow<List<IncidentStateTransition>> = flowOf(savedTransitions)
        override suspend fun saveTransition(transition: IncidentStateTransition) {
            savedTransitions.add(transition)
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
    fun testPersistsEveryAnomalyEventIncludingNonEscalating08GBrakingWithoutTriggeringIncident() = runBlocking {
        val fakeAnomalyRepo = FakeAnomalyRepository()
        val fakeIncidentRepo = FakeIncidentRepository()
        val fakeTimelineRepo = FakeTimelineRepository()
        val stateMachine = IncidentStateMachine(fakeIncidentRepo, fakeTimelineRepo)
        val anomalyManager = AnomalyManager(anomalyEngine, fakeAnomalyRepo, stateMachine)

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

        // Saved to anomaly repo (full log)
        assertEquals(1, fakeAnomalyRepo.savedEvents.size)
        // NOT escalated to incident state machine because requiresVerification = false
        assertEquals(0, fakeIncidentRepo.savedIncidents.size)
        assertEquals(0, fakeTimelineRepo.savedTransitions.size)
    }

    @Test
    fun testCriticalAnomalyFeedsStateMachineWithReasonAndSeverity() = runBlocking {
        val fakeAnomalyRepo = FakeAnomalyRepository()
        val fakeIncidentRepo = FakeIncidentRepository()
        val fakeTimelineRepo = FakeTimelineRepository()
        val stateMachine = IncidentStateMachine(fakeIncidentRepo, fakeTimelineRepo)
        val anomalyManager = AnomalyManager(anomalyEngine, fakeAnomalyRepo, stateMachine)

        val telemetry = com.example.okdrivers.sensors.VehicleTelemetrySample(
            timestamp = System.currentTimeMillis(),
            speedKmh = 80f,
            rpm = 0f, // Engine stop anomaly (Critical)
            engineLoad = 0f,
            throttlePosition = 0f,
            engineTemperatureCelsius = 90f,
            batteryVoltage = 12.6f,
            diagnosticFault = null,
            airbagDeployed = false
        )

        val result = anomalyManager.evaluateAndPersist(
            driverId = "driver_1",
            vehicleId = "vehicle_1",
            vehicleTelemetry = telemetry,
            driverState = null,
            motionSensor = null,
            gpsLocation = null,
            driverBaseline = null,
            vehicleBaseline = sampleDriverBaseline
        )

        assertTrue(result.requiresVerification)
        assertTrue(result.events.isNotEmpty())

        // Saved to anomaly repo
        assertTrue(fakeAnomalyRepo.savedEvents.isNotEmpty())
        // Fed into incident state machine (transitions: ANOMALY_DETECTION -> AI_VERIFICATION -> AUTHORITY_ESCALATION)
        assertTrue(fakeIncidentRepo.savedIncidents.isNotEmpty())
        assertEquals(3, fakeTimelineRepo.savedTransitions.size)

        val incident = fakeIncidentRepo.savedIncidents.last()
        val transition = fakeTimelineRepo.savedTransitions.last()

        assertEquals(result.events.first().severity, incident.severity)
        assertEquals(result.events.first().reason, transition.reason)
    }
}
