package com.example.okdrivers.data.repository

import com.example.okdrivers.domain.model.AIConversationSession
import kotlinx.coroutines.flow.Flow

interface AIConversationRepository {
    suspend fun getForIncident(
        incidentId: String
    ): List<AIConversationSession>
    fun observeSessions(
        incidentId: String
    ): Flow<List<AIConversationSession>>
    suspend fun saveSession(
        session: AIConversationSession
    )
}