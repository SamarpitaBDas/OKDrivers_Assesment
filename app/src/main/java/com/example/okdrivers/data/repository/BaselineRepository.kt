package com.example.okdrivers.data.repository

import com.example.okdrivers.domain.model.SafetyBaseline

interface BaselineRepository {

    suspend fun getBaseline(
        driverId: String,
        vehicleId: String
    ): SafetyBaseline?

    suspend fun saveBaseline(baseline: SafetyBaseline)
}