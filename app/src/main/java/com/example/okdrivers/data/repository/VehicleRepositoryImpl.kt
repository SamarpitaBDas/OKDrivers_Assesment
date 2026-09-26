package com.example.okdrivers.data.repository

import com.example.okdrivers.data.local.dao.VehicleProfileDao
import com.example.okdrivers.domain.model.VehicleProfile
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VehicleRepositoryImpl @Inject constructor(
    private val dao: VehicleProfileDao
) : VehicleRepository {
    override fun observeVehicles(): Flow<List<VehicleProfile>> {
        return dao.getAll().map { list ->
            list.map { it.toDomain() }
        }
    }
    override fun observeVehicle(id: String): Flow<VehicleProfile?> {
        return dao.observeById(id).map { it?.toDomain() }
    }
    override suspend fun getVehicle(id: String): VehicleProfile? {
        return dao.getById(id)?.toDomain()
    }
    override suspend fun saveVehicle(vehicle: VehicleProfile) {
        dao.insert(vehicle.toEntity())
    }
}