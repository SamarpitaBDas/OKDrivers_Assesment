package com.example.okdrivers.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "responder_actions")
data class ResponderActionEntity(
    @PrimaryKey
    val id: String,
    val incidentId: String,
    val responderId: String,
    val timestamp: Long,
    val action: String,
    val latitude: Double?,
    val longitude: Double?
)