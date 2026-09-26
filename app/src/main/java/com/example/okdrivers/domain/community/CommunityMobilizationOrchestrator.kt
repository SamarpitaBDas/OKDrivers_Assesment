package com.example.okdrivers.domain.community

import com.example.okdrivers.data.repository.ResponderRepository
import com.example.okdrivers.domain.engine.IncidentStateMachine
import com.example.okdrivers.domain.model.EmergencyState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityMobilizationOrchestrator @Inject constructor(
    private val incidentStateMachine: IncidentStateMachine,
    private val responderRepository: ResponderRepository
) {
    private val seededIncidentIds = mutableSetOf<String>()
    private val defaultCenterLat = 37.7749
    private val defaultCenterLng = -122.4194

    init {
        CoroutineScope(Dispatchers.Default).launch {
            incidentStateMachine.currentState.collectLatest { state ->
                if (state == EmergencyState.COMMUNITY_MOBILIZATION) {
                    mobilizationTriggered()
                }
            }
        }
    }

    suspend fun mobilizationTriggered() {
        val activeIncident = incidentStateMachine.getActiveIncident()
        val incidentId = activeIncident?.id ?: "default_incident"

        if (seededIncidentIds.contains(incidentId)) {
            return
        }
        seededIncidentIds.add(incidentId)

        val centerLat = activeIncident?.latitude ?: defaultCenterLat
        val centerLng = activeIncident?.longitude ?: defaultCenterLng

        val responders = ResponderSimulator.generateAround(centerLat, centerLng, count = 6)
        for (responder in responders) {
            responderRepository.saveResponder(responder)
        }
    }
}
