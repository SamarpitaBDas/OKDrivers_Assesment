package com.example.okdrivers.domain.community

import com.example.okdrivers.data.repository.ResponderRepository
import com.example.okdrivers.domain.engine.IncidentStateMachine
import com.example.okdrivers.domain.model.EmergencyState
import com.example.okdrivers.domain.model.Responder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ResponderSimulationTest {

    private class FakeResponderRepository : ResponderRepository {
        val savedResponders = mutableListOf<Responder>()
        override fun observeActiveResponders(): Flow<List<Responder>> = flowOf(savedResponders)
        override suspend fun saveResponder(responder: Responder) {
            savedResponders.add(responder)
        }
    }

    private class FakeResponderActionRepository : com.example.okdrivers.data.repository.ResponderActionRepository {
        val actions = mutableListOf<com.example.okdrivers.domain.model.ResponderAction>()
        private val _actionsFlow = MutableStateFlow<List<com.example.okdrivers.domain.model.ResponderAction>>(emptyList())
        override fun observeActions(incidentId: String): Flow<List<com.example.okdrivers.domain.model.ResponderAction>> = _actionsFlow
        override suspend fun saveAction(action: com.example.okdrivers.domain.model.ResponderAction) {
            actions.add(action)
            _actionsFlow.value = actions.toList()
        }
    }

    private class FakeIncidentRepository : com.example.okdrivers.data.repository.IncidentRepository {
        val incidents = mutableListOf<com.example.okdrivers.domain.model.Incident>()
        override fun observeIncidents(): Flow<List<com.example.okdrivers.domain.model.Incident>> = flowOf(incidents)
        override suspend fun getIncident(id: String): com.example.okdrivers.domain.model.Incident? = incidents.find { it.id == id }
        override suspend fun saveIncident(incident: com.example.okdrivers.domain.model.Incident) {
            incidents.add(incident)
        }
    }

    private class FakeTimelineRepository : com.example.okdrivers.data.repository.IncidentTimelineRepository {
        val transitions = mutableListOf<com.example.okdrivers.domain.model.IncidentStateTransition>()
        override fun observeTimeline(incidentId: String): Flow<List<com.example.okdrivers.domain.model.IncidentStateTransition>> = flowOf(transitions)
        override suspend fun saveTransition(transition: com.example.okdrivers.domain.model.IncidentStateTransition) {
            transitions.add(transition)
        }
    }

    @Test
    fun testHaversineDistanceCalculation() {
        val p1 = Pair(37.7749, -122.4194) // San Francisco
        val p2 = Pair(34.0522, -118.2437) // Los Angeles
        val dist = ResponderDistanceCalculator.distanceKm(p1, p2)
        assertTrue(dist > 500.0 && dist < 600.0) // Approx 559 km
    }

    @Test
    fun testResponderSimulatorGeneration() {
        val responders = ResponderSimulator.generateAround(37.7749, -122.4194, count = 6)
        assertEquals(6, responders.size)
        assertTrue(responders.any { it.name == "Dr. Sarah Miller" })
        assertTrue(responders.any { it.name == "Medic John Doe" })
        for (r in responders) {
            assertTrue(r.reputationScore!! in 3.5f..5.0f)
            assertTrue(r.isActive)
        }
    }

    @Test
    fun testFilterAndSortByDistance() {
        val center = Pair(37.7749, -122.4194)
        val responders = ResponderSimulator.generateAround(center.first, center.second, count = 6)
        val searchService = ResponderSearchService()
        val filtered = searchService.findEligible(responders, center.first, center.second, radiusKm = 10.0)
        assertFalse(filtered.isEmpty())
        assertTrue(filtered.size <= 6)
    }

    @Test
    fun testMobilizationManagerExhaustionEscalatesToAuthority() = runBlocking {
        val responderRepo = FakeResponderRepository()
        val actionRepo = FakeResponderActionRepository()
        val incidentRepo = FakeIncidentRepository()
        val timelineRepo = FakeTimelineRepository()
        val stateMachine = IncidentStateMachine(incidentRepo, timelineRepo)
        val searchService = ResponderSearchService()

        val mobilizationManager = ResponderMobilizationManager(
            responderSearchService = searchService,
            responderRepository = responderRepo,
            responderActionRepository = actionRepo,
            incidentStateMachine = stateMachine,
            dispatcher = Dispatchers.Unconfined
        )

        val responders = ResponderSimulator.generateAround(37.7749, -122.4194, count = 6)

        // Set state machine into COMMUNITY_MOBILIZATION
        stateMachine.transitionTo(EmergencyState.ANOMALY_DETECTION, "Trigger")
        stateMachine.transitionTo(EmergencyState.AI_VERIFICATION, "Verifying")
        stateMachine.transitionTo(EmergencyState.COMMUNITY_MOBILIZATION, "Mobilizing")

        // Run mobilization with no acceptance (exhaustion across all tiers)
        val outcome = mobilizationManager.mobilize(
            incidentId = "incident_exhaust",
            lat = 37.7749,
            lng = -122.4194,
            allResponders = responders
        )

        assertEquals(MobilizationOutcome.EscalatedToAuthority, outcome)
        assertEquals(EmergencyState.AUTHORITY_ESCALATION, stateMachine.currentState.value)
    }

    @Test
    fun testResponderAcceptanceAndEnrouteJourney() = runBlocking {
        val responderRepo = FakeResponderRepository()
        val actionRepo = FakeResponderActionRepository()
        val incidentRepo = FakeIncidentRepository()
        val timelineRepo = FakeTimelineRepository()
        val stateMachine = IncidentStateMachine(incidentRepo, timelineRepo)
        val searchService = ResponderSearchService()

        val mobilizationManager = ResponderMobilizationManager(
            responderSearchService = searchService,
            responderRepository = responderRepo,
            responderActionRepository = actionRepo,
            incidentStateMachine = stateMachine,
            dispatcher = Dispatchers.Unconfined
        )

        val responders = ResponderSimulator.generateAround(37.7749, -122.4194, count = 6)
        val eligibleTier1 = searchService.findEligible(responders, 37.7749, -122.4194, 5.0)
        val firstResponderId = eligibleTier1.first().id

        stateMachine.transitionTo(EmergencyState.ANOMALY_DETECTION, "Trigger")
        stateMachine.transitionTo(EmergencyState.AI_VERIFICATION, "Verifying")
        stateMachine.transitionTo(EmergencyState.COMMUNITY_MOBILIZATION, "Mobilizing")

        // Pre-simulate acceptance action for an eligible tier 1 responder
        actionRepo.saveAction(
            com.example.okdrivers.domain.model.ResponderAction(
                id = java.util.UUID.randomUUID().toString(),
                incidentId = "incident_accept",
                responderId = firstResponderId,
                timestamp = System.currentTimeMillis(),
                action = com.example.okdrivers.domain.model.ResponderActionType.ACCEPTED,
                latitude = 37.7749,
                longitude = -122.4194
            )
        )

        val outcome = mobilizationManager.mobilize(
            incidentId = "incident_accept",
            lat = 37.7749,
            lng = -122.4194,
            allResponders = responders
        )

        assertTrue(outcome is MobilizationOutcome.Accepted)
        assertEquals(EmergencyState.COMMUNITY_RESPONSE, stateMachine.currentState.value)

        // Verify NOTIFIED, ACCEPTED, and ENROUTE actions were recorded
        val actions = actionRepo.actions
        assertTrue(actions.any { it.action == com.example.okdrivers.domain.model.ResponderActionType.NOTIFIED })
        assertTrue(actions.any { it.action == com.example.okdrivers.domain.model.ResponderActionType.ACCEPTED })
        assertTrue(actions.any { it.action == com.example.okdrivers.domain.model.ResponderActionType.ENROUTE })
    }
}
