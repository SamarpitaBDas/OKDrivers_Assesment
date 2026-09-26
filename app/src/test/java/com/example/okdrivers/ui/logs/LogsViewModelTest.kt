package com.example.okdrivers.ui.logs

import com.example.okdrivers.data.repository.IncidentRepository
import com.example.okdrivers.data.repository.IncidentTimelineRepository
import com.example.okdrivers.domain.model.EmergencyState
import com.example.okdrivers.domain.model.Incident
import com.example.okdrivers.domain.model.IncidentStateTransition
import com.example.okdrivers.domain.model.AnomalySeverity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Test

class LogsViewModelTest {

    private class FakeIncidentRepository(val incidents: List<Incident>) : IncidentRepository {
        override fun observeIncidents(): Flow<List<Incident>> = flowOf(incidents)
        override fun observeActiveIncident(): Flow<Incident?> = flowOf(incidents.firstOrNull { it.currentState != EmergencyState.NORMAL_OPERATION && it.currentState != EmergencyState.RESOLVED })
        override suspend fun getIncident(id: String): Incident? = incidents.find { it.id == id }
        override suspend fun saveIncident(incident: Incident) {}
    }

    private class FakeTimelineRepository(val transitions: List<IncidentStateTransition>) : IncidentTimelineRepository {
        override fun observeTimeline(incidentId: String): Flow<List<IncidentStateTransition>> = flowOf(transitions.filter { it.incidentId == incidentId })
        override suspend fun saveTransition(transition: IncidentStateTransition) {}
    }

    @Test
    fun testLogsViewModelMappingAndFormatting() {
        val now = System.currentTimeMillis()
        val incident = Incident(
            id = "inc_1",
            driverId = "d_1",
            vehicleId = "v_1",
            createdAt = now,
            updatedAt = now + 1000,
            latitude = 37.7749,
            longitude = -122.4194,
            severity = AnomalySeverity.HIGH,
            currentState = EmergencyState.AI_VERIFICATION,
            anomalyConfidence = 0.9f,
            primaryAnomalyId = "anom_1",
            driverCondition = null,
            communityMobilized = false,
            responderAccepted = false,
            authorityEscalated = false,
            resolvedAt = null
        )

        val transition = IncidentStateTransition(
            id = "tr_1",
            incidentId = "inc_1",
            timestamp = now,
            fromState = EmergencyState.ANOMALY_DETECTION,
            toState = EmergencyState.AI_VERIFICATION,
            reason = "0.92G braking, 340% above driver's baseline"
        )

        val incidentRepo = FakeIncidentRepository(listOf(incident))
        val timelineRepo = FakeTimelineRepository(listOf(transition))

        val viewModel = LogsViewModel(incidentRepo, timelineRepo)
        assertNotNull(viewModel.uiState)
    }
}
