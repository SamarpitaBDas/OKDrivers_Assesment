package com.example.okdrivers.data.repository

import com.example.okdrivers.domain.model.AIConversationSession

interface AIConversationRepository {
    suspend fun getForIncident(
        incidentId: String
    ): List<AIConversationSession>
    suspend fun saveSession(
        session: AIConversationSession
    )
}