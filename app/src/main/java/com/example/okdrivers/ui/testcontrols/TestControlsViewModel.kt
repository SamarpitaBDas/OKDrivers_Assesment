package com.example.okdrivers.ui.testcontrols

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.okdrivers.audio.DemoVoiceResponseController
import com.example.okdrivers.data.repository.IncidentRepository
import com.example.okdrivers.data.repository.ResponderActionRepository
import com.example.okdrivers.domain.engine.IncidentStateMachine
import com.example.okdrivers.domain.model.ResponderAction
import com.example.okdrivers.domain.model.ResponderActionType
import com.example.okdrivers.sensors.DmsSimulator
import com.example.okdrivers.sensors.MotionSensorManager
import com.example.okdrivers.sensors.VehicleTelemetrySimulator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TestControlsViewModel @Inject constructor(
    private val motionSensorManager: MotionSensorManager,
    private val vehicleTelemetrySimulator: VehicleTelemetrySimulator,
    private val dmsSimulator: DmsSimulator,
    private val demoController: DemoVoiceResponseController,
    private val responderActionRepository: ResponderActionRepository,
    private val incidentStateMachine: IncidentStateMachine,
    private val incidentRepository: IncidentRepository
) : ViewModel() {

    private val activeScenarioNameFlow = MutableStateFlow("Normal Driving")
    private val statusMessageFlow = MutableStateFlow("System operating normally — no active triggers")

    val uiState: StateFlow<TestControlsUiState> = combine(
        incidentStateMachine.currentState,
        incidentRepository.observeActiveIncident(),
        activeScenarioNameFlow,
        statusMessageFlow
    ) { state, activeIncident, scenarioName, statusMsg ->
        TestControlsUiState(
            activeScenarioName = scenarioName,
            currentEmergencyState = state,
            activeIncidentId = activeIncident?.id,
            simulatedDriverResponseText = if (demoController.isForceSimulatedMode()) "Controlled (STT simulated)" else "Real STT active",
            isForceSimulatedMode = demoController.isForceSimulatedMode(),
            statusMessage = statusMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TestControlsUiState()
    )

    fun triggerNormalDriving() {
        vehicleTelemetrySimulator.clearAnomaly()
        dmsSimulator.clearAnomaly()
        motionSensorManager.clearSimulatedGForce()
        viewModelScope.launch {
            incidentStateMachine.forceResetToNormal()
        }
        activeScenarioNameFlow.value = "1. Normal Driving"
        statusMessageFlow.value = "Scenario 1: Normal Driving active — all anomaly triggers cleared"
    }

    fun triggerHardBraking() {
        vehicleTelemetrySimulator.injectAnomaly(VehicleTelemetrySimulator.AnomalyType.HARD_BRAKING)
        motionSensorManager.injectSimulatedGForce(0.85f)
        activeScenarioNameFlow.value = "2. 0.8G Hard Brake"
        statusMessageFlow.value = "Scenario 2: 0.8G Hard Brake injected (verify no escalation)"
    }

    fun triggerSuspectedAccident() {
        vehicleTelemetrySimulator.injectAnomaly(VehicleTelemetrySimulator.AnomalyType.ENGINE_STOP)
        motionSensorManager.injectSimulatedGForce(2.2f)
        dmsSimulator.injectAnomaly(DmsSimulator.AnomalyType.UNRESPONSIVE)
        activeScenarioNameFlow.value = "3. Suspected Accident"
        statusMessageFlow.value = "Scenario 3: Suspected Accident injected (speed drop + RPM 0 + unresponsive DMS)"
    }

    fun triggerDriverResponds() {
        demoController.setForceSimulatedMode(true)
        demoController.setNextSimulatedResponse("I'm okay")
        activeScenarioNameFlow.value = "4. Driver Responds (\"I'm okay\")"
        statusMessageFlow.value = "Scenario 4: Set next simulated voice response to \"I'm okay\""
    }

    fun triggerDriverSilent() {
        demoController.setForceSimulatedMode(true)
        demoController.setNextSimulatedResponse(null)
        activeScenarioNameFlow.value = "5. Driver Silent / Unresponsive"
        statusMessageFlow.value = "Scenario 5: Set next simulated voice response to Silence (letting 3 attempts exhaust)"
    }

    fun triggerResponderAccepts() {
        val incidentId = uiState.value.activeIncidentId ?: UUID.randomUUID().toString()
        viewModelScope.launch {
            val action = ResponderAction(
                id = UUID.randomUUID().toString(),
                incidentId = incidentId,
                responderId = "responder_fixture_0",
                timestamp = System.currentTimeMillis(),
                action = ResponderActionType.ACCEPTED,
                latitude = 37.7749,
                longitude = -122.4194
            )
            responderActionRepository.saveAction(action)
        }
        activeScenarioNameFlow.value = "6. Responder Accepts"
        statusMessageFlow.value = "Scenario 6: Injected ACCEPTED ResponderAction for active incident"
    }

    fun triggerNoCommunityResponse() {
        activeScenarioNameFlow.value = "7. No Community Response"
        statusMessageFlow.value = "Scenario 7: No community response — letting 5->10->20km tiers exhaust to AUTHORITY_ESCALATION"
    }

    fun triggerAirbagEvent() {
        vehicleTelemetrySimulator.injectAnomaly(VehicleTelemetrySimulator.AnomalyType.AIRBAG_DEPLOYED)
        motionSensorManager.injectSimulatedGForce(3.5f)
        activeScenarioNameFlow.value = "8. Critical / Airbag Event"
        statusMessageFlow.value = "Scenario 8: Critical Airbag Event injected — immediate escalation"
    }
}
