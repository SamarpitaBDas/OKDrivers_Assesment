package com.example.okdrivers.domain.model

data class VehicleProfile(
    val id: String,
    val registrationNumber: String,
    val make: String,
    val model: String,
    val year: Int?,
    val driverId: String?,
    val createdAt: Long,
    val updatedAt: Long
)