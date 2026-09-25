package com.example.okdrivers.data.repository

import com.example.okdrivers.data.local.dao.IncidentDao
import com.example.okdrivers.domain.model.Incident
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class IncidentRepositoryImpl @Inject constructor(
    private val dao: IncidentDao
) : IncidentRepository {
    override fun observeIncidents(): Flow<List<Incident>> {
        return dao.observeAll().map { list ->
            list.map { it.toDomain() }
        }
    }
    override suspend fun getIncident(
        id: String
    ): Incident? {
        return dao.getById(id)?.toDomain()
    }
    override suspend fun saveIncident(
        incident: Incident
    ) {
        dao.insert(incident.toEntity())
    }
}