package com.example.okdrivers.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incident_state_transitions")
data class IncidentStateTransitionEntity(
    @PrimaryKey
    val id: String,
    val incidentId: String,
    val timestamp: Long,
    val fromState: String,
    val toState: String,
    val reason: String
)