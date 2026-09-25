package com.example.okdrivers.data.repository

import com.example.okdrivers.data.local.dao.AIConversationSessionDao
import com.example.okdrivers.domain.model.AIConversationSession
import javax.inject.Inject

class AIConversationRepositoryImpl @Inject constructor(
    private val dao: AIConversationSessionDao
) : AIConversationRepository {
    override suspend fun getForIncident(
        incidentId: String
    ): List<AIConversationSession> {
        return dao.getForIncident(incidentId)
            .map { it.toDomain() }
    }
    override suspend fun saveSession(
        session: AIConversationSession
    ) {
        dao.insert(session.toEntity())
    }
}