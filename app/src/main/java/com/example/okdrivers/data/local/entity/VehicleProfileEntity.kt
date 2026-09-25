package com.example.okdrivers.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicle_profiles")
data class VehicleProfileEntity(
    @PrimaryKey
    val id: String,
    val registrationNumber: String,
    val make: String,
    val model: String,
    val year: Int?,
    val driverId: String?,
    val createdAt: Long,
    val updatedAt: Long
)