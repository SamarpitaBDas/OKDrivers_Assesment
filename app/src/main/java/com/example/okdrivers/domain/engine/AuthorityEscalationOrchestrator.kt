package com.example.okdrivers.domain.engine

import com.example.okdrivers.domain.model.EmergencyState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthorityEscalationOrchestrator @Inject constructor(
    private val incidentStateMachine: IncidentStateMachine,
    private val incidentPayloadBuilder: IncidentPayloadBuilder,
    private val emergencyApiService: EmergencyApiService,
    private val familyContactNotificationService: FamilyContactNotificationService
) {
    private val escalatedIncidentIds = mutableSetOf<String>()

    init {
        CoroutineScope(Dispatchers.Default).launch {
            incidentStateMachine.currentState.collectLatest { state ->
                if (state == EmergencyState.AUTHORITY_ESCALATION) {
                    processEscalation()
                }
            }
        }
    }

    suspend fun processEscalation(): IncidentPayload? {
        val activeIncident = incidentStateMachine.getActiveIncident() ?: return null
        val incidentId = activeIncident.id

        if (escalatedIncidentIds.contains(incidentId)) {
            return null
        }
        escalatedIncidentIds.add(incidentId)

        val payload = incidentPayloadBuilder.build(incidentId)
        emergencyApiService.sendPayload(payload)
        familyContactNotificationService.notifyFamilyContact(
            incidentId = incidentId,
            severity = payload.severity.name,
            locationSummary = payload.summaryText
        )
        return payload
    }
}
