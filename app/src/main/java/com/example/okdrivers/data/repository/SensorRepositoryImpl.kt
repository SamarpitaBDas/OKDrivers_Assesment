package com.example.okdrivers.data.repository

import com.example.okdrivers.data.local.dao.SensorSampleDao
import com.example.okdrivers.domain.model.SensorSample
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SensorRepositoryImpl @Inject constructor(
    private val dao: SensorSampleDao
) : SensorRepository {
    override fun observeSamples(): Flow<List<SensorSample>> {
        return dao.observeAll().map { list ->
            list.map { it.toDomain() }
        }
    }
    override suspend fun getRecentSamples(limit: Int): List<SensorSample> {
        return dao.getRecent(limit).map { it.toDomain() }
    }
    override suspend fun saveSample(sample: SensorSample) {
        dao.insert(sample.toEntity())
    }
    override suspend fun clearSamples() {
        dao.deleteAll()
    }
}