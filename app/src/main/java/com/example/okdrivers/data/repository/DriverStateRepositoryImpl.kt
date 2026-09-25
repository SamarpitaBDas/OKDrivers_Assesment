package com.example.okdrivers.data.repository

import com.example.okdrivers.data.local.dao.DriverStateDao
import com.example.okdrivers.domain.model.DriverState
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DriverStateRepositoryImpl @Inject constructor(
    private val dao: DriverStateDao
) : DriverStateRepository {
    override fun observeStates(): Flow<List<DriverState>> {
        return dao.observeAll().map { list ->
            list.map { it.toDomain() }
        }
    }
    override suspend fun getLatestState(): DriverState? {
        return dao.getLatest()?.toDomain()
    }
    override suspend fun saveState(state: DriverState) {
        dao.insert(state.toEntity())
    }
}