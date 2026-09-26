package com.example.okdrivers.data.repository

import com.example.okdrivers.data.local.dao.DriverProfileDao
import com.example.okdrivers.domain.model.DriverProfile
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DriverRepositoryImpl @Inject constructor(
    private val dao: DriverProfileDao
) : DriverRepository {
    override fun observeDrivers(): Flow<List<DriverProfile>> {
        return dao.getAll().map { list ->
            list.map { it.toDomain() }
        }
    }
    override fun observeDriver(id: String): Flow<DriverProfile?> {
        return dao.observeById(id).map { it?.toDomain() }
    }
    override suspend fun getDriver(id: String): DriverProfile? {
        return dao.getById(id)?.toDomain()
    }
    override suspend fun saveDriver(driver: DriverProfile) {
        dao.insert(driver.toEntity())
    }
}