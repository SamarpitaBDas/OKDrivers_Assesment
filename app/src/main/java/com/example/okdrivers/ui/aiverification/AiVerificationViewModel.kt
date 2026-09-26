package com.example.okdrivers.ui.aiverification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.okdrivers.audio.DemoVoiceResponseController
import com.example.okdrivers.audio.VoiceVerificationRouter
import com.example.okdrivers.data.repository.AIConversationRepository
import com.example.okdrivers.data.repository.IncidentRepository
import com.example.okdrivers.domain.model.AIConversationSession
import com.example.okdrivers.domain.model.UrgencyLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AiVerificationViewModel @Inject constructor(
    private val incidentRepository: IncidentRepository,
    private val aiConversationRepository: AIConversationRepository,
    private val router: VoiceVerificationRouter,
    private val demoController: DemoVoiceResponseController
) : ViewModel() {

    private val forceSimulatedRefreshFlow = MutableStateFlow(0)

    val uiState: StateFlow<AiVerificationUiState> = combine(
        incidentRepository.observeActiveIncident(),
        forceSimulatedRefreshFlow
    ) { incident, _ -> incident }
        .flatMapLatest { incident ->
            if (incident == null) {
                flowOf(
                    AiVerificationUiState(
                        isRealServiceActive = router.shouldUseRealService(),
                        isForceSimulatedMode = demoController.isForceSimulatedMode()
                    )
                )
            } else {
                aiConversationRepository.observeSessions(incident.id).map { sessions ->
                    mapStateForIncident(incident.id, sessions)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AiVerificationUiState()
        )

    private fun mapStateForIncident(
        incidentId: String,
        sessions: List<AIConversationSession>
    ): AiVerificationUiState {
        val latestSession = sessions.lastOrNull()
        val currentAttemptCount = latestSession?.attemptCount ?: 1
        val currentUrgencyLevel = when (currentAttemptCount) {
            1 -> UrgencyLevel.INITIAL
            2 -> UrgencyLevel.URGENT
            else -> UrgencyLevel.FINAL
        }

        val activePromptText = latestSession?.prompt ?: router.getPromptText(currentUrgencyLevel)
        val isSessionInProgress = latestSession != null && !latestSession.completed

        val statusText = when {
            isSessionInProgress -> "Attempt $currentAttemptCount in progress — listening for STT response..."
            latestSession?.responseClassification != null -> "Attempt $currentAttemptCount completed — Result: ${latestSession.responseClassification?.name}"
            else -> "AI Verification active — awaiting response"
        }

        return AiVerificationUiState(
            hasActiveIncident = true,
            incidentId = incidentId,
            currentUrgencyLevel = currentUrgencyLevel,
            currentAttemptCount = currentAttemptCount,
            activePromptText = activePromptText,
            latestTranscript = latestSession?.response,
            latestClassification = latestSession?.responseClassification,
            latestLatencyMs = latestSession?.responseLatencyMs,
            isSessionInProgress = isSessionInProgress,
            isRealServiceActive = router.shouldUseRealService(),
            isForceSimulatedMode = demoController.isForceSimulatedMode(),
            sessionHistory = sessions,
            verificationStatusText = statusText
        )
    }

    fun setSimulatedResponse(transcript: String?) {
        demoController.setNextSimulatedResponse(transcript)
    }

    fun toggleForceSimulatedMode(enabled: Boolean) {
        demoController.setForceSimulatedMode(enabled)
        forceSimulatedRefreshFlow.value += 1
    }
}
