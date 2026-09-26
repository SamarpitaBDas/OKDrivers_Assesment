package com.example.okdrivers.domain.community

import com.example.okdrivers.data.repository.ResponderRepository
import com.example.okdrivers.domain.engine.IncidentStateMachine
import com.example.okdrivers.domain.model.EmergencyState
import com.example.okdrivers.domain.model.Responder
import kotlinx.coroutines.flow.Flow
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
        val filtered = ResponderDistanceCalculator.filterAndSortByDistance(responders, center.first, center.second, radiusKm = 10.0)
        assertFalse(filtered.isEmpty())
        for (i in 0 until filtered.size - 1) {
            assertTrue(filtered[i].second <= filtered[i + 1].second)
        }
    }

    @Test
    fun testCommunityMobilizationOrchestratorSeedingAndIdempotency() = runBlocking {
        val responderRepo = FakeResponderRepository()
        val incidentRepo = FakeIncidentRepository()
        val timelineRepo = FakeTimelineRepository()
        val stateMachine = IncidentStateMachine(incidentRepo, timelineRepo)

        val orchestrator = CommunityMobilizationOrchestrator(stateMachine, responderRepo)

        // Trigger mobilization
        stateMachine.transitionTo(EmergencyState.ANOMALY_DETECTION, "Trigger")
        stateMachine.transitionTo(EmergencyState.AI_VERIFICATION, "Verifying")
        stateMachine.transitionTo(EmergencyState.COMMUNITY_MOBILIZATION, "Mobilizing")

        orchestrator.mobilizationTriggered()

        assertEquals(6, responderRepo.savedResponders.size)

        // Call again to test idempotency guard (should not add more responders)
        orchestrator.mobilizationTriggered()
        assertEquals(6, responderRepo.savedResponders.size)
    }
}
