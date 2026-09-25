package com.example.okdrivers.data.repository

import com.example.okdrivers.data.local.dao.ResponderActionDao
import com.example.okdrivers.domain.model.ResponderAction
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ResponderActionRepositoryImpl @Inject constructor(
    private val dao: ResponderActionDao
) : ResponderActionRepository {

    override fun observeActions(
        incidentId: String
    ): Flow<List<ResponderAction>> {
        return dao.observeForIncident(incidentId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun saveAction(
        action: ResponderAction
    ) {
        dao.insert(action.toEntity())
    }
}