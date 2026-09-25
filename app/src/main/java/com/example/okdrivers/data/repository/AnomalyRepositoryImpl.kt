package com.example.okdrivers.data.repository

import com.example.okdrivers.data.local.dao.AnomalyEventDao
import com.example.okdrivers.domain.model.AnomalyEvent
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AnomalyRepositoryImpl @Inject constructor(
    private val dao: AnomalyEventDao
) : AnomalyRepository {

    override fun observeAnomalies(): Flow<List<AnomalyEvent>> {
        return dao.observeAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun saveAnomaly(
        anomaly: AnomalyEvent
    ) {
        dao.insert(anomaly.toEntity())
    }
}