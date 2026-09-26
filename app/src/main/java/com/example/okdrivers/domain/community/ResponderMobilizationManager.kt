package com.example.okdrivers.domain.community

import com.example.okdrivers.data.repository.ResponderActionRepository
import com.example.okdrivers.data.repository.ResponderRepository
import com.example.okdrivers.domain.engine.IncidentStateMachine
import com.example.okdrivers.di.DefaultDispatcher
import com.example.okdrivers.domain.model.EmergencyState
import com.example.okdrivers.domain.model.ResponderAction
import com.example.okdrivers.domain.model.ResponderActionType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class MobilizationOutcome {
    data class Accepted(val responderId: String, val tierRadiusKm: Double) : MobilizationOutcome()
    object EscalatedToAuthority : MobilizationOutcome()
}

@Singleton
class ResponderMobilizationManager @Inject constructor(
    private val responderSearchService: ResponderSearchService,
    private val responderRepository: ResponderRepository,
    private val responderActionRepository: ResponderActionRepository,
    private val incidentStateMachine: IncidentStateMachine,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {

    private val tiers = listOf(5.0, 10.0, 20.0)
    private val tierTimeoutMillis = 10000L // 10 seconds per tier
    private val maxNotifyCountPerTier = 3

    suspend fun mobilize(
        incidentId: String,
        lat: Double,
        lng: Double,
        allResponders: List<com.example.okdrivers.domain.model.Responder>,
        now: Long = System.currentTimeMillis()
    ): MobilizationOutcome = withContext(dispatcher) {

        for (radius in tiers) {
            val eligible = responderSearchService.findEligible(allResponders, lat, lng, radius)
                .take(maxNotifyCountPerTier)

            if (eligible.isEmpty()) {
                continue
            }

            for (responder in eligible) {
                val action = ResponderAction(
                    id = UUID.randomUUID().toString(),
                    incidentId = incidentId,
                    responderId = responder.id,
                    timestamp = now,
                    action = ResponderActionType.NOTIFIED,
                    latitude = lat,
                    longitude = lng
                )
                responderActionRepository.saveAction(action)
            }

            val acceptedResponderId = withTimeoutOrNull(tierTimeoutMillis) {
                try {
                    responderActionRepository.observeActions(incidentId)
                        .map { actions ->
                            actions.find { act ->
                                act.action == ResponderActionType.ACCEPTED && eligible.any { it.id == act.responderId }
                            }?.responderId
                        }
                        .first { it != null }
                } catch (_: Exception) {
                    null
                }
            }

            if (acceptedResponderId != null) {
                for (responder in eligible) {
                    if (responder.id != acceptedResponderId) {
                        val declineAction = ResponderAction(
                            id = UUID.randomUUID().toString(),
                            incidentId = incidentId,
                            responderId = responder.id,
                            timestamp = System.currentTimeMillis(),
                            action = ResponderActionType.DECLINED,
                            latitude = lat,
                            longitude = lng
                        )
                        responderActionRepository.saveAction(declineAction)
                    }
                }

                incidentStateMachine.transitionTo(
                    EmergencyState.COMMUNITY_RESPONSE,
                    "Responder $acceptedResponderId accepted mobilization at ${radius}km radius",
                    now = System.currentTimeMillis()
                )

                // Simulate ENROUTE journey update for the winning responder (terminal simulated state)
                delay(200L)
                val enrouteAction = ResponderAction(
                    id = UUID.randomUUID().toString(),
                    incidentId = incidentId,
                    responderId = acceptedResponderId,
                    timestamp = System.currentTimeMillis(),
                    action = ResponderActionType.ENROUTE,
                    latitude = lat,
                    longitude = lng
                )
                responderActionRepository.saveAction(enrouteAction)

                return@withContext MobilizationOutcome.Accepted(acceptedResponderId, radius)
            }
        }

        incidentStateMachine.transitionTo(
            EmergencyState.AUTHORITY_ESCALATION,
            "Community mobilization exhausted across all radius tiers (up to 20km). Escalating to authorities.",
            now = System.currentTimeMillis()
        )

        return@withContext MobilizationOutcome.EscalatedToAuthority
    }
}
