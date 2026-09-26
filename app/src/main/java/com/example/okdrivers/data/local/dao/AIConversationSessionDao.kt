package com.example.okdrivers.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.okdrivers.data.local.entity.AIConversationSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AIConversationSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: AIConversationSessionEntity)

    @Update
    suspend fun update(session: AIConversationSessionEntity)

    @Query("""
        SELECT * FROM ai_conversation_sessions
        WHERE incidentId = :incidentId
        ORDER BY startedAt ASC
    """)
    suspend fun getForIncident(
        incidentId: String
    ): List<AIConversationSessionEntity>

    @Query("""
        SELECT * FROM ai_conversation_sessions
        WHERE incidentId = :incidentId
        ORDER BY startedAt ASC
    """)
    fun observeForIncident(
        incidentId: String
    ): Flow<List<AIConversationSessionEntity>>
}