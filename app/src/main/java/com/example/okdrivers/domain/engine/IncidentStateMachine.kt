package com.example.okdrivers.domain.engine

import com.example.okdrivers.data.repository.IncidentRepository
import com.example.okdrivers.data.repository.IncidentTimelineRepository
import com.example.okdrivers.domain.model.AnomalyEvent
import com.example.okdrivers.domain.model.AnomalySeverity
import com.example.okdrivers.domain.model.EmergencyState
import com.example.okdrivers.domain.model.Incident
import com.example.okdrivers.domain.model.IncidentStateTransition
import java.util.UUID
import javax.inject.Inject

class IncidentStateMachine @Inject constructor(
    private val incidentRepository: IncidentRepository,
    private val timelineRepository: IncidentTimelineRepository
) {

    suspend fun handleAnomalyEvent(
        event: AnomalyEvent,
        driverId: String?,
        vehicleId: String?,
        now: Long = System.currentTimeMillis()
    ): Incident? {
        // If event does not require verification (e.g. non-escalating 0.8G braking), do not trigger incident state machine
        if (!event.requiresVerification) {
            return null
        }

        val targetState = when {
            event.isEscalated || event.severity == AnomalySeverity.CRITICAL -> EmergencyState.AUTHORITY_ESCALATION
            event.severity == AnomalySeverity.HIGH -> EmergencyState.AI_VERIFICATION
            else -> EmergencyState.ANOMALY_DETECTION
        }

        val incidentId = UUID.randomUUID().toString()
        val incident = Incident(
            id = incidentId,
            driverId = driverId,
            vehicleId = vehicleId,
            createdAt = now,
            updatedAt = now,
            latitude = event.latitude,
            longitude = event.longitude,
            severity = event.severity,
            currentState = targetState,
            anomalyConfidence = event.confidence,
            primaryAnomalyId = event.id,
            driverCondition = null,
            communityMobilized = false,
            responderAccepted = false,
            authorityEscalated = targetState == EmergencyState.AUTHORITY_ESCALATION,
            resolvedAt = null
        )

        incidentRepository.saveIncident(incident)

        val transition = IncidentStateTransition(
            id = UUID.randomUUID().toString(),
            incidentId = incidentId,
            timestamp = now,
            fromState = EmergencyState.NORMAL_OPERATION,
            toState = targetState,
            reason = event.reason
        )

        timelineRepository.saveTransition(transition)

        return incident
    }
}
