package com.example.okdrivers.domain.engine

import com.example.okdrivers.data.repository.*
import com.example.okdrivers.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class IncidentPayloadTest {

    private class FakeIncidentRepository : IncidentRepository {
        var incident: Incident? = null
        override fun observeIncidents(): Flow<List<Incident>> = flowOf(listOfNotNull(incident))
        override suspend fun getIncident(id: String): Incident? = incident
        override suspend fun saveIncident(incident: Incident) {
            this.incident = incident
        }
    }

    private class FakeTelemetryRepository : TelemetryRepository {
        var telemetry = listOf(VehicleTelemetry(System.currentTimeMillis(), 60f, 2500, 40f, 30f, 90f, 13.8f, null))
        override fun observeTelemetry(): Flow<List<VehicleTelemetry>> = flowOf(telemetry)
        override suspend fun getRecentTelemetry(limit: Int): List<VehicleTelemetry> = telemetry
        override suspend fun saveTelemetry(telemetry: VehicleTelemetry) {}
    }

    private class FakeDriverStateRepository : DriverStateRepository {
        var state: DriverState? = null
        override fun observeStates(): Flow<List<DriverState>> = flowOf(listOfNotNull(state))
        override suspend fun getLatestState(): DriverState? = state
        override suspend fun saveState(state: DriverState) {}
    }

    private class FakeAIConversationRepository : AIConversationRepository {
        val sessions = mutableListOf<AIConversationSession>()
        override suspend fun getForIncident(incidentId: String): List<AIConversationSession> = sessions.filter { it.incidentId == incidentId }
        override suspend fun saveSession(session: AIConversationSession) {
            sessions.add(session)
        }
    }

    private class FakeResponderActionRepository : ResponderActionRepository {
        val actions = mutableListOf<ResponderAction>()
        override fun observeActions(incidentId: String): Flow<List<ResponderAction>> = flowOf(actions)
        override suspend fun saveAction(action: ResponderAction) {
            actions.add(action)
        }
    }

    private class FakeResponderRepository : ResponderRepository {
        val responders = mutableListOf<Responder>()
        override fun observeActiveResponders(): Flow<List<Responder>> = flowOf(responders)
        override suspend fun saveResponder(responder: Responder) {
            responders.add(responder)
        }
    }

    private class FakeNotificationRepository : NotificationRepository {
        val notifications = mutableListOf<NotificationEvent>()
        override fun observeNotifications(): Flow<List<NotificationEvent>> = flowOf(notifications)
        override suspend fun saveNotification(notification: NotificationEvent) {
            notifications.add(notification)
        }
    }

    @Test
    fun testIncidentPayloadAssemblyAndGracefulDegradation() = runBlocking {
        val incidentRepo = FakeIncidentRepository()
        val telemetryRepo = FakeTelemetryRepository()
        val driverStateRepo = FakeDriverStateRepository()
        val aiRepo = FakeAIConversationRepository()
        val actionRepo = FakeResponderActionRepository()
        val responderRepo = FakeResponderRepository()

        incidentRepo.incident = Incident(
            id = "incident_123",
            driverId = "driver_1",
            vehicleId = "vehicle_1",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            latitude = 37.7749,
            longitude = -122.4194,
            severity = AnomalySeverity.CRITICAL,
            currentState = EmergencyState.AUTHORITY_ESCALATION,
            anomalyConfidence = 0.95f,
            primaryAnomalyId = "anomaly_1",
            driverCondition = DriverCondition.UNRESPONSIVE,
            communityMobilized = true,
            responderAccepted = true,
            authorityEscalated = true,
            resolvedAt = null
        )

        aiRepo.sessions.add(
            AIConversationSession(
                id = "session_1",
                incidentId = "incident_123",
                startedAt = System.currentTimeMillis(),
                endedAt = System.currentTimeMillis(),
                prompt = "Emergency prompt",
                response = null,
                responseClassification = ResponseClassification.UNRESPONSIVE,
                responseLatencyMs = 300L,
                attemptCount = 3,
                completed = false
            )
        )

        val builder = IncidentPayloadBuilder(
            incidentRepo,
            telemetryRepo,
            driverStateRepo,
            aiRepo,
            actionRepo,
            responderRepo
        )

        val payload = builder.build("incident_123")

        assertEquals("incident_123", payload.incidentId)
        assertEquals(AnomalySeverity.CRITICAL, payload.severity)
        assertNotNull(payload.latestTelemetry)
        assertEquals(ResponseClassification.UNRESPONSIVE, payload.verificationOutcome)
        assertTrue(payload.summaryText.contains("CRITICAL"))
    }

    @Test
    fun testEmergencyApiServiceSendsAndPersistsNotification() = runBlocking {
        val notifRepo = FakeNotificationRepository()
        val apiService = EmergencyApiService(notifRepo)

        val payload = IncidentPayload(
            incidentId = "incident_999",
            timestamp = System.currentTimeMillis(),
            severity = AnomalySeverity.CRITICAL,
            location = Pair(37.7749, -122.4194),
            vehicleId = "veh_1",
            driverId = "drv_1",
            latestTelemetry = null,
            latestDmsSnapshot = null,
            verificationOutcome = ResponseClassification.UNRESPONSIVE,
            communityResponseStatus = CommunityResponseStatus(false, 0, null, false, null),
            summaryText = "CRITICAL EMERGENCY TEST"
        )

        val result = apiService.sendPayload(payload)
        assertTrue(result.isSuccess)
        assertEquals(1, notifRepo.notifications.size)
        assertEquals("CRITICAL EMERGENCY TEST", notifRepo.notifications.first().message)
        assertEquals(NotificationRecipient.EMERGENCY_AUTHORITY, notifRepo.notifications.first().recipientType)
    }

    @Test
    fun testFamilyContactNotificationService() = runBlocking {
        val notifRepo = FakeNotificationRepository()
        val familyService = FamilyContactNotificationService(notifRepo)

        val result = familyService.notifyFamilyContact("incident_777", "CRITICAL", "San Francisco")
        assertTrue(result.isSuccess)
        assertEquals(1, notifRepo.notifications.size)
        assertEquals(NotificationRecipient.FAMILY_CONTACT, notifRepo.notifications.first().recipientType)
        assertTrue(notifRepo.notifications.first().message.contains("CRITICAL"))
    }
}
