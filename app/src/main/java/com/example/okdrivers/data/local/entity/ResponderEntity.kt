package com.example.okdrivers.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "responders")
data class ResponderEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double,
    val isActive: Boolean,
    val reputationScore: Float?,
    val status: String
)