package com.example.okdrivers.domain.engine

import com.example.okdrivers.data.repository.IncidentRepository
import com.example.okdrivers.data.repository.IncidentTimelineRepository
import com.example.okdrivers.domain.model.EmergencyState
import com.example.okdrivers.domain.model.Incident
import com.example.okdrivers.domain.model.IncidentStateTransition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class IncidentStateMachineTest {

    private class FakeIncidentRepository : IncidentRepository {
        val incidents = mutableListOf<Incident>()
        override fun observeIncidents(): Flow<List<Incident>> = flowOf(incidents)
        override suspend fun getIncident(id: String): Incident? = incidents.find { it.id == id }
        override suspend fun saveIncident(incident: Incident) {
            incidents.add(incident)
        }
    }

    private class FakeTimelineRepository : IncidentTimelineRepository {
        val transitions = mutableListOf<IncidentStateTransition>()
        override fun observeTimeline(incidentId: String): Flow<List<IncidentStateTransition>> = flowOf(transitions)
        override suspend fun saveTransition(transition: IncidentStateTransition) {
            transitions.add(transition)
        }
    }

    @Test
    fun testValidStateTransitionsAndDowngradePath() = runBlocking {
        val incidentRepo = FakeIncidentRepository()
        val timelineRepo = FakeTimelineRepository()
        val stateMachine = IncidentStateMachine(incidentRepo, timelineRepo)

        assertEquals(EmergencyState.NORMAL_OPERATION, stateMachine.currentState.value)

        // 1. Transition to Anomaly Detection
        stateMachine.transitionTo(EmergencyState.ANOMALY_DETECTION, "Sudden deceleration detected")
        assertEquals(EmergencyState.ANOMALY_DETECTION, stateMachine.currentState.value)

        // 2. Transition to AI Verification
        stateMachine.transitionTo(EmergencyState.AI_VERIFICATION, "Verifying driver response")
        assertEquals(EmergencyState.AI_VERIFICATION, stateMachine.currentState.value)

        // 3. Downgrade path: AI Verification -> Normal Operation (Driver responded clearly)
        stateMachine.transitionTo(EmergencyState.NORMAL_OPERATION, "Driver responded clearly, false alarm")
        assertEquals(EmergencyState.NORMAL_OPERATION, stateMachine.currentState.value)
    }

    @Test
    fun testIllegalTransitionRejection() {
        val incidentRepo = FakeIncidentRepository()
        val timelineRepo = FakeTimelineRepository()
        val stateMachine = IncidentStateMachine(incidentRepo, timelineRepo)

        assertFalse(stateMachine.isValidTransition(EmergencyState.NORMAL_OPERATION, EmergencyState.COMMUNITY_MOBILIZATION))

        try {
            runBlocking {
                stateMachine.transitionTo(EmergencyState.COMMUNITY_MOBILIZATION, "Illegal jump")
            }
            fail("Expected IllegalArgumentException for illegal state transition")
        } catch (_: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun testFullEscalationAndResolutionTerminalState() = runBlocking {
        val incidentRepo = FakeIncidentRepository()
        val timelineRepo = FakeTimelineRepository()
        val stateMachine = IncidentStateMachine(incidentRepo, timelineRepo)

        stateMachine.transitionTo(EmergencyState.ANOMALY_DETECTION, "Trigger")
        stateMachine.transitionTo(EmergencyState.AI_VERIFICATION, "Verifying")
        stateMachine.transitionTo(EmergencyState.COMMUNITY_MOBILIZATION, "No response, mobilizing community")
        assertEquals(EmergencyState.COMMUNITY_MOBILIZATION, stateMachine.currentState.value)

        stateMachine.transitionTo(EmergencyState.COMMUNITY_RESPONSE, "Responder accepted")
        assertEquals(EmergencyState.COMMUNITY_RESPONSE, stateMachine.currentState.value)

        stateMachine.transitionTo(EmergencyState.RESOLVED, "Incident resolved")
        assertEquals(EmergencyState.RESOLVED, stateMachine.currentState.value)
    }
}
