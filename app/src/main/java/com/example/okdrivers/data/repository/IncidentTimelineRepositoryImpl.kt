package com.example.okdrivers.data.repository

import com.example.okdrivers.data.local.dao.IncidentStateTransitionDao
import com.example.okdrivers.domain.model.IncidentStateTransition
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class IncidentTimelineRepositoryImpl @Inject constructor(
    private val dao: IncidentStateTransitionDao
) : IncidentTimelineRepository {

    override fun observeTimeline(
        incidentId: String
    ): Flow<List<IncidentStateTransition>> {
        return dao.observeForIncident(incidentId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun saveTransition(
        transition: IncidentStateTransition
    ) {
        dao.insert(transition.toEntity())
    }
}