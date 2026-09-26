package com.example.okdrivers.domain.model

data class AIConversationSession(
    val id: String,
    val incidentId: String,
    val startedAt: Long,
    val endedAt: Long?,
    val prompt: String,
    val response: String?,
    val responseClassification: ResponseClassification?,
    val responseLatencyMs: Long?,
    val attemptCount: Int,
    val completed: Boolean
)

enum class ResponseClassification {
    RESPONSIVE,
    IMPAIRED,
    UNRESPONSIVE
}
