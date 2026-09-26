package com.example.okdrivers.ui.incident

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.okdrivers.data.repository.AIConversationRepository
import com.example.okdrivers.data.repository.AnomalyRepository
import com.example.okdrivers.data.repository.IncidentRepository
import com.example.okdrivers.data.repository.ResponderActionRepository
import com.example.okdrivers.domain.engine.EmergencyOrchestratorCoordinator
import com.example.okdrivers.domain.model.AIConversationSession
import com.example.okdrivers.domain.model.EmergencyState
import com.example.okdrivers.domain.model.ResponderAction
import com.example.okdrivers.domain.model.ResponderActionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EmergencyIncidentViewModel @Inject constructor(
    private val incidentRepository: IncidentRepository,
    private val anomalyRepository: AnomalyRepository,
    private val aiConversationRepository: AIConversationRepository,
    private val responderActionRepository: ResponderActionRepository,
    private val coordinator: EmergencyOrchestratorCoordinator
) : ViewModel() {

    val uiState: StateFlow<EmergencyIncidentUiState> = incidentRepository.observeActiveIncident()
        .flatMapLatest { incident ->
            if (incident == null) {
                flowOf(EmergencyIncidentUiState())
            } else {
                val sessionsFlow = aiConversationRepository.observeSessions(incident.id)
                val actionsFlow = responderActionRepository.observeActions(incident.id)

                combine(sessionsFlow, actionsFlow) { sessions, actions ->
                    val primaryAnomaly = incident.primaryAnomalyId?.let { anomalyRepository.getEvent(it) }
                    val triggerReason = primaryAnomaly?.reason ?: "Sensor threshold exceeded"

                    val canSelfResolve = when (incident.currentState) {
                        EmergencyState.ANOMALY_DETECTION,
                        EmergencyState.AI_VERIFICATION,
                        EmergencyState.COMMUNITY_MOBILIZATION,
                        EmergencyState.COMMUNITY_RESPONSE -> true
                        else -> false
                    }

                    val contextualStatusLine = deriveContextualStatusLine(incident.currentState, sessions, actions)

                    EmergencyIncidentUiState(
                        hasActiveIncident = true,
                        emergencyState = incident.currentState,
                        severity = incident.severity,
                        confidence = incident.anomalyConfidence,
                        formattedConfidence = String.format(Locale.US, "%.0f%%", incident.anomalyConfidence * 100f),
                        triggerReason = triggerReason,
                        contextualStatusLine = contextualStatusLine,
                        formattedCreatedTime = formatTime(incident.createdAt),
                        formattedLocation = formatLocation(incident.latitude, incident.longitude),
                        incidentId = incident.id,
                        canSelfResolve = canSelfResolve
                    )
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = EmergencyIncidentUiState()
        )

    fun selfResolve() {
        val currentState = uiState.value
        if (currentState.canSelfResolve && currentState.incidentId != null) {
            viewModelScope.launch {
                coordinator.cancelAndResolve(currentState.incidentId, "Manually cancelled by driver")
            }
        }
    }

    companion object {
        fun deriveContextualStatusLine(
            state: EmergencyState?,
            sessions: List<AIConversationSession>,
            actions: List<ResponderAction>
        ): String {
            return when (state) {
                null, EmergencyState.NORMAL_OPERATION -> "No active emergency — all systems normal"
                EmergencyState.ANOMALY_DETECTION -> "Anomaly detected — evaluating severity"
                EmergencyState.AI_VERIFICATION -> {
                    val attempt = (sessions.size + 1).coerceAtMost(3)
                    "Attempt $attempt of 3 — voice prompt active"
                }
                EmergencyState.COMMUNITY_MOBILIZATION -> {
                    val notifiedCount = actions.count { it.action == ResponderActionType.NOTIFIED }
                    val radius = if (notifiedCount > 3) "10km" else "5km"
                    "Searching within $radius radius — $notifiedCount responders notified"
                }
                EmergencyState.COMMUNITY_RESPONSE -> "Responder en route — contacting driver"
                EmergencyState.AUTHORITY_ESCALATION -> "Emergency payload dispatched — dispatcher notified"
                EmergencyState.RESOLVED -> "Incident resolved"
            }
        }

        fun formatTime(timestamp: Long): String {
            if (timestamp <= 0L) return "—"
            val sdf = SimpleDateFormat("HH:mm:ss 'on' MMM dd, yyyy", Locale.US)
            return sdf.format(Date(timestamp))
        }

        fun formatLocation(lat: Double?, lng: Double?): String {
            if (lat == null || lng == null) return "—"
            return String.format(Locale.US, "Lat %.4f°, Long %.4f°", lat, lng)
        }
    }
}
