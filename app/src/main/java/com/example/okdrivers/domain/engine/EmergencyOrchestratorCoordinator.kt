package com.example.okdrivers.domain.engine

import com.example.okdrivers.data.repository.AIConversationRepository
import com.example.okdrivers.data.repository.ResponderActionRepository
import com.example.okdrivers.data.repository.ResponderRepository
import com.example.okdrivers.domain.community.ResponderMobilizationManager
import com.example.okdrivers.domain.model.AnomalySeverity
import com.example.okdrivers.domain.model.EmergencyState
import com.example.okdrivers.domain.model.ResponderAction
import com.example.okdrivers.domain.model.ResponderActionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class EmergencyOrchestratorCoordinator @Inject constructor(
    private val incidentStateMachine: IncidentStateMachine,
    private val verificationOrchestrator: EmergencyVerificationOrchestrator,
    private val mobilizationManager: ResponderMobilizationManager,
    private val aiConversationRepository: AIConversationRepository,
    private val responderActionRepository: ResponderActionRepository,
    private val responderRepository: ResponderRepository
) {
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var activeJob: Job? = null

    open fun launchVerification(incidentId: String, driverId: String?, vehicleId: String?, severity: AnomalySeverity) {
        activeJob?.cancel()
        activeJob = externalScope.launch {
            verificationOrchestrator.runVerificationFlow(incidentId, driverId, vehicleId, severity)
        }
    }

    open fun launchMobilization(incidentId: String, lat: Double, lng: Double) {
        activeJob?.cancel()
        activeJob = externalScope.launch {
            val responders = responderRepository.observeActiveResponders().first()
            mobilizationManager.mobilize(incidentId, lat, lng, responders)
        }
    }

    open suspend fun cancelAndResolve(incidentId: String, reason: String = "Manually cancelled by driver") {
        activeJob?.cancel()
        activeJob = null
        cleanUpDanglingRows(incidentId)
        incidentStateMachine.transitionTo(EmergencyState.RESOLVED, reason)
    }

    private suspend fun cleanUpDanglingRows(incidentId: String) {
        val sessions = aiConversationRepository.getForIncident(incidentId)
        for (session in sessions) {
            if (!session.completed) {
                val updatedSession = session.copy(completed = true)
                aiConversationRepository.saveSession(updatedSession)
            }
        }

        val declineAction = ResponderAction(
            id = UUID.randomUUID().toString(),
            incidentId = incidentId,
            responderId = "system_cancellation",
            timestamp = System.currentTimeMillis(),
            action = ResponderActionType.DECLINED,
            latitude = null,
            longitude = null
        )
        responderActionRepository.saveAction(declineAction)
    }
}
