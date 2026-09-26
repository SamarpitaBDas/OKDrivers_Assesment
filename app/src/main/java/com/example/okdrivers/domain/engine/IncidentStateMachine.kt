package com.example.okdrivers.domain.engine

import com.example.okdrivers.data.repository.IncidentRepository
import com.example.okdrivers.data.repository.IncidentTimelineRepository
import com.example.okdrivers.domain.model.AnomalyEvent
import com.example.okdrivers.domain.model.AnomalySeverity
import com.example.okdrivers.domain.model.EmergencyState
import com.example.okdrivers.domain.model.Incident
import com.example.okdrivers.domain.model.IncidentStateTransition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncidentStateMachine @Inject constructor(
    private val incidentRepository: IncidentRepository,
    private val timelineRepository: IncidentTimelineRepository
) {

    private val _currentState = MutableStateFlow(EmergencyState.NORMAL_OPERATION)
    val currentState: StateFlow<EmergencyState> = _currentState.asStateFlow()

    private var activeIncident: Incident? = null

    fun isValidTransition(from: EmergencyState, to: EmergencyState): Boolean {
        if (from == to) return true
        return when (from) {
            EmergencyState.NORMAL_OPERATION -> to == EmergencyState.ANOMALY_DETECTION
            EmergencyState.ANOMALY_DETECTION -> to == EmergencyState.NORMAL_OPERATION || to == EmergencyState.AI_VERIFICATION
            EmergencyState.AI_VERIFICATION -> to == EmergencyState.NORMAL_OPERATION || to == EmergencyState.RESOLVED || to == EmergencyState.COMMUNITY_MOBILIZATION || to == EmergencyState.AUTHORITY_ESCALATION
            EmergencyState.COMMUNITY_MOBILIZATION -> to == EmergencyState.COMMUNITY_RESPONSE || to == EmergencyState.AUTHORITY_ESCALATION || to == EmergencyState.RESOLVED
            EmergencyState.COMMUNITY_RESPONSE -> to == EmergencyState.RESOLVED
            EmergencyState.AUTHORITY_ESCALATION -> to == EmergencyState.RESOLVED
            EmergencyState.RESOLVED -> to == EmergencyState.NORMAL_OPERATION
        }
    }

    suspend fun evaluateAndTransition(
        result: AnomalyDetectionResult,
        driverId: String?,
        vehicleId: String?,
        now: Long = System.currentTimeMillis()
    ): Incident? {
        val primaryEvent = result.events.firstOrNull() ?: return null

        // 1. Non-escalation path: ANOMALY_DETECTION -> NORMAL_OPERATION (or stay NORMAL)
        if (!result.requiresVerification) {
            if (_currentState.value == EmergencyState.ANOMALY_DETECTION) {
                transitionTo(EmergencyState.NORMAL_OPERATION, primaryEvent.reason, driverId, vehicleId, primaryEvent, now)
            }
            return null
        }

        // 2. Escalation path: NORMAL_OPERATION -> ANOMALY_DETECTION -> AI_VERIFICATION
        if (_currentState.value == EmergencyState.NORMAL_OPERATION) {
            transitionTo(EmergencyState.ANOMALY_DETECTION, primaryEvent.reason, driverId, vehicleId, primaryEvent, now)
        }

        if (_currentState.value == EmergencyState.ANOMALY_DETECTION) {
            transitionTo(EmergencyState.AI_VERIFICATION, primaryEvent.reason, driverId, vehicleId, primaryEvent, now)
        }

        // 3. Further escalation from AI_VERIFICATION if critical
        val targetState = when {
            result.isEscalated || result.overallSeverity == AnomalySeverity.CRITICAL -> EmergencyState.AUTHORITY_ESCALATION
            result.overallSeverity == AnomalySeverity.HIGH -> EmergencyState.COMMUNITY_MOBILIZATION
            else -> EmergencyState.AI_VERIFICATION
        }

        if (targetState != _currentState.value) {
            return transitionTo(targetState, primaryEvent.reason, driverId, vehicleId, primaryEvent, now)
        }

        return activeIncident
    }

    suspend fun transitionTo(
        targetState: EmergencyState,
        reason: String,
        driverId: String? = null,
        vehicleId: String? = null,
        event: AnomalyEvent? = null,
        now: Long = System.currentTimeMillis()
    ): Incident? {
        val fromState = _currentState.value
        require(isValidTransition(fromState, targetState)) {
            "Illegal state transition from $fromState to $targetState"
        }

        if (fromState == targetState) {
            return activeIncident
        }

        _currentState.value = targetState

        val incidentId = activeIncident?.id ?: UUID.randomUUID().toString()
        val isResolved = targetState == EmergencyState.RESOLVED || targetState == EmergencyState.NORMAL_OPERATION

        val incident = Incident(
            id = incidentId,
            driverId = driverId ?: activeIncident?.driverId,
            vehicleId = vehicleId ?: activeIncident?.vehicleId,
            createdAt = activeIncident?.createdAt ?: now,
            updatedAt = now,
            latitude = event?.latitude ?: activeIncident?.latitude,
            longitude = event?.longitude ?: activeIncident?.longitude,
            severity = event?.severity ?: activeIncident?.severity ?: AnomalySeverity.LOW,
            currentState = targetState,
            anomalyConfidence = event?.confidence ?: activeIncident?.anomalyConfidence ?: 0f,
            primaryAnomalyId = event?.id ?: activeIncident?.primaryAnomalyId,
            driverCondition = null,
            communityMobilized = targetState == EmergencyState.COMMUNITY_MOBILIZATION || (activeIncident?.communityMobilized == true),
            responderAccepted = targetState == EmergencyState.COMMUNITY_RESPONSE || (activeIncident?.responderAccepted == true),
            authorityEscalated = targetState == EmergencyState.AUTHORITY_ESCALATION || (activeIncident?.authorityEscalated == true),
            resolvedAt = if (isResolved) now else null
        )

        activeIncident = if (isResolved) null else incident

        if (targetState != EmergencyState.NORMAL_OPERATION || incidentId.isNotEmpty()) {
            incidentRepository.saveIncident(incident)

            val transition = IncidentStateTransition(
                id = UUID.randomUUID().toString(),
                incidentId = incidentId,
                timestamp = now,
                fromState = fromState,
                toState = targetState,
                reason = reason
            )
            timelineRepository.saveTransition(transition)
        }

        return incident
    }

    fun getActiveIncident(): Incident? = activeIncident

    suspend fun forceResetToNormal(reason: String = "Test control reset to NORMAL_OPERATION") {
        if (_currentState.value != EmergencyState.NORMAL_OPERATION) {
            _currentState.value = EmergencyState.NORMAL_OPERATION
            activeIncident = null
        }
    }
}
