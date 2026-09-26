package com.example.okdrivers.data.repository

import com.example.okdrivers.domain.model.VehicleProfile
import kotlinx.coroutines.flow.Flow

interface VehicleRepository {
    fun observeVehicles(): Flow<List<VehicleProfile>>
    fun observeVehicle(id: String): Flow<VehicleProfile?>
    suspend fun getVehicle(id: String): VehicleProfile?
    suspend fun saveVehicle(vehicle: VehicleProfile)
}