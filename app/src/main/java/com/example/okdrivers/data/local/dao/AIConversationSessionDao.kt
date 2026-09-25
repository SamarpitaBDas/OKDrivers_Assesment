package com.example.okdrivers.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.okdrivers.data.local.entity.AIConversationSessionEntity

@Dao
interface AIConversationSessionDao {
    @Insert
    suspend fun insert(session: AIConversationSessionEntity)
    @Query("""
        SELECT * FROM ai_conversation_sessions
        WHERE incidentId = :incidentId
        ORDER BY startedAt ASC
    """)
    suspend fun getForIncident(
        incidentId: String
    ): List<AIConversationSessionEntity>
}