package com.example.okdrivers.domain.engine

import com.example.okdrivers.data.repository.*
import com.example.okdrivers.domain.model.AnomalySeverity
import com.example.okdrivers.domain.model.ResponderActionType
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncidentPayloadBuilder @Inject constructor(
    private val incidentRepository: IncidentRepository,
    private val telemetryRepository: TelemetryRepository,
    private val driverStateRepository: DriverStateRepository,
    private val aiConversationRepository: AIConversationRepository,
    private val responderActionRepository: ResponderActionRepository,
    private val responderRepository: ResponderRepository
) {

    suspend fun build(incidentId: String, now: Long = System.currentTimeMillis()): IncidentPayload {
        val incident = incidentRepository.getIncident(incidentId)
        val severity = incident?.severity ?: AnomalySeverity.HIGH
        val vehicleId = incident?.vehicleId
        val driverId = incident?.driverId
        val location = if (incident?.latitude != null && incident.longitude != null) {
            Pair(incident.latitude, incident.longitude)
        } else {
            null
        }

        // Fresh telemetry at build time
        val latestTelemetry = runCatching {
            telemetryRepository.getRecentTelemetry(1).firstOrNull()
        }.getOrNull()

        // Latest raw DMS snapshot
        val latestDms = runCatching {
            driverStateRepository.getLatestState()
        }.getOrNull()

        // AI Verification outcome from conversation sessions
        val sessions = runCatching {
            aiConversationRepository.getForIncident(incidentId)
        }.getOrDefault(emptyList())
        val latestSession = sessions.maxByOrNull { it.startedAt }
        val verificationOutcome = latestSession?.responseClassification

        // Community response status from responder actions
        val actions = runCatching {
            responderActionRepository.observeActions(incidentId).firstOrNull()
        }.getOrDefault(emptyList()) ?: emptyList()

        val notifiedCount = actions.count { it.action == ResponderActionType.NOTIFIED }
        val acceptedAction = actions.find { it.action == ResponderActionType.ACCEPTED }
        val accepted = acceptedAction != null
        val acceptedResponderId = acceptedAction?.responderId

        val allResponders = runCatching {
            responderRepository.observeActiveResponders().firstOrNull()
        }.getOrDefault(emptyList()) ?: emptyList()
        val acceptedResponderName = allResponders.find { it.id == acceptedResponderId }?.name

        val communityStatus = CommunityResponseStatus(
            mobilized = notifiedCount > 0,
            respondersNotifiedCount = notifiedCount,
            radiusReachedKm = if (notifiedCount > 0) 20.0 else null,
            accepted = accepted,
            acceptedResponderName = acceptedResponderName
        )

        val summaryText = IncidentPayload.generateSummary(
            incidentId = incidentId,
            severity = severity,
            location = location,
            vehicleId = vehicleId,
            verificationOutcome = verificationOutcome,
            communityStatus = communityStatus
        )

        return IncidentPayload(
            incidentId = incidentId,
            timestamp = incident?.createdAt ?: now,
            severity = severity,
            location = location,
            vehicleId = vehicleId,
            driverId = driverId,
            latestTelemetry = latestTelemetry,
            latestDmsSnapshot = latestDms,
            verificationOutcome = verificationOutcome,
            communityResponseStatus = communityStatus,
            summaryText = summaryText
        )
    }
}
