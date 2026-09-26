package com.example.okdrivers.ui.testcontrols

import com.example.okdrivers.domain.model.EmergencyState

data class TestControlsUiState(
    val activeScenarioName: String = "Normal Driving",
    val currentEmergencyState: EmergencyState = EmergencyState.NORMAL_OPERATION,
    val activeIncidentId: String? = null,
    val simulatedDriverResponseText: String? = "I'm okay",
    val isForceSimulatedMode: Boolean = false,
    val statusMessage: String = "System operating normally — no active triggers"
)
