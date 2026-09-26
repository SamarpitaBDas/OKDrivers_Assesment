package com.example.okdrivers.ui.incident

import com.example.okdrivers.domain.model.AnomalySeverity
import com.example.okdrivers.domain.model.EmergencyState

data class EmergencyIncidentUiState(
    val hasActiveIncident: Boolean = false,
    val emergencyState: EmergencyState? = null,
    val severity: AnomalySeverity? = null,
    val confidence: Float? = null,
    val formattedConfidence: String = "—",
    val triggerReason: String? = null,
    val contextualStatusLine: String = "No active emergency — all systems normal",
    val formattedCreatedTime: String = "—",
    val formattedLocation: String = "—",
    val incidentId: String? = null,
    val canSelfResolve: Boolean = false
)
