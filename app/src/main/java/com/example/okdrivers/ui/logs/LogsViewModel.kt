package com.example.okdrivers.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.okdrivers.data.repository.IncidentRepository
import com.example.okdrivers.data.repository.IncidentTimelineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LogsViewModel @Inject constructor(
    incidentRepository: IncidentRepository,
    timelineRepository: IncidentTimelineRepository
) : ViewModel() {

    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    val uiState: StateFlow<LogsUiState> = incidentRepository.observeIncidents()
        .map { incidents ->
            incidents.maxByOrNull { it.updatedAt }
        }
        .flatMapLatest { incident ->
            if (incident == null) {
                flowOf(LogsUiState.Idle("No incident history available"))
            } else {
                timelineRepository.observeTimeline(incident.id)
                    .map { transitions ->
                        if (transitions.isEmpty()) {
                            LogsUiState.Idle("No timeline events for incident ${incident.id.take(8)}")
                        } else {
                            val models = transitions.map { transition ->
                                val timeStr = dateFormat.format(Date(transition.timestamp))
                                val reasonStr = transition.reason.ifBlank { "${transition.fromState} → ${transition.toState}" }
                                TimelineUiModel(
                                    id = transition.id,
                                    timestampMillis = transition.timestamp,
                                    timeText = timeStr,
                                    reasonText = reasonStr
                                )
                            }
                            LogsUiState.Success(incidentId = incident.id, timeline = models)
                        }
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LogsUiState.Idle("Loading incident logs...")
        )
}

sealed interface LogsUiState {
    data class Idle(val message: String) : LogsUiState
    data class Success(val incidentId: String, val timeline: List<TimelineUiModel>) : LogsUiState
}
