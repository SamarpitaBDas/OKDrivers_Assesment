package com.example.okdrivers.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_conversation_sessions")
data class AIConversationSessionEntity(
    @PrimaryKey
    val id: String,
    val incidentId: String,
    val startedAt: Long,
    val endedAt: Long?,
    val prompt: String,
    val response: String?,
    val responseClassification: String?,
    val responseLatencyMs: Long?,
    val attemptCount: Int,
    val completed: Boolean
)