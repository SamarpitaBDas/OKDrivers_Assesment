package com.example.okdrivers.data.repository

import com.example.okdrivers.data.local.dao.ResponderDao
import com.example.okdrivers.domain.model.Responder
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ResponderRepositoryImpl @Inject constructor(
    private val dao: ResponderDao
) : ResponderRepository {

    override fun observeActiveResponders(): Flow<List<Responder>> {
        return dao.observeActiveResponders().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun saveResponder(
        responder: Responder
    ) {
        dao.insert(responder.toEntity())
    }
}