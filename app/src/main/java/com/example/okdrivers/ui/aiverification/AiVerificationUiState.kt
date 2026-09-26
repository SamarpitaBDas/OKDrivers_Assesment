package com.example.okdrivers.ui.aiverification

import com.example.okdrivers.domain.model.AIConversationSession
import com.example.okdrivers.domain.model.ResponseClassification
import com.example.okdrivers.domain.model.UrgencyLevel

data class AiVerificationUiState(
    val hasActiveIncident: Boolean = false,
    val incidentId: String? = null,
    val currentUrgencyLevel: UrgencyLevel? = null,
    val currentAttemptCount: Int = 0,
    val activePromptText: String? = null,
    val latestTranscript: String? = null,
    val latestClassification: ResponseClassification? = null,
    val latestLatencyMs: Long? = null,
    val isSessionInProgress: Boolean = false,
    val isRealServiceActive: Boolean = false,
    val isForceSimulatedMode: Boolean = false,
    val sessionHistory: List<AIConversationSession> = emptyList(),
    val verificationStatusText: String = "No active AI verification session — system operating normally"
)
