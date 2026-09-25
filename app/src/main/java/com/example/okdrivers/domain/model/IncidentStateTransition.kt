package com.example.okdrivers.domain.model

data class IncidentStateTransition(
    val id: String,
    val incidentId: String,
    val timestamp: Long,
    val fromState: EmergencyState,
    val toState: EmergencyState,
    val reason: String
)