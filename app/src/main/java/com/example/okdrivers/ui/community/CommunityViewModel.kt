package com.example.okdrivers.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.okdrivers.data.repository.IncidentRepository
import com.example.okdrivers.data.repository.ResponderRepository
import com.example.okdrivers.domain.community.ResponderDistanceCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import java.util.Locale

@HiltViewModel
class CommunityViewModel @Inject constructor(
    responderRepository: ResponderRepository,
    incidentRepository: IncidentRepository
) : ViewModel() {

    private val defaultLat = 37.7749
    private val defaultLng = -122.4194

    val uiState: StateFlow<CommunityUiState> = combine(
        responderRepository.observeActiveResponders(),
        incidentRepository.observeIncidents()
    ) { responders, incidents ->
        val activeIncident = incidents.firstOrNull { it.resolvedAt == null }
        val centerLat = activeIncident?.latitude ?: defaultLat
        val centerLng = activeIncident?.longitude ?: defaultLng

        if (responders.isEmpty()) {
            CommunityUiState.Idle("No active responders nearby")
        } else {
            val mappedWithDist = responders.map { responder ->
                val distKm = ResponderDistanceCalculator.distanceKm(
                    Pair(centerLat, centerLng),
                    Pair(responder.latitude, responder.longitude)
                )
                val etaMinutes = (distKm / 40.0 * 60.0).toInt().coerceAtLeast(1)
                val initials = responder.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()
                val statusText = when (responder.status) {
                    com.example.okdrivers.domain.model.ResponderStatus.AVAILABLE -> "Active / Available"
                    com.example.okdrivers.domain.model.ResponderStatus.NOTIFIED -> "Notified"
                    com.example.okdrivers.domain.model.ResponderStatus.ACCEPTED -> "Accepted"
                    com.example.okdrivers.domain.model.ResponderStatus.ENROUTE -> "En Route"
                    com.example.okdrivers.domain.model.ResponderStatus.ARRIVED -> "Arrived"
                    com.example.okdrivers.domain.model.ResponderStatus.DECLINED -> "Declined"
                    com.example.okdrivers.domain.model.ResponderStatus.UNAVAILABLE -> "Unavailable"
                }

                val uiModel = ResponderUiModel(
                    id = responder.id,
                    name = responder.name,
                    initials = initials.ifEmpty { "MC" },
                    distanceText = String.format(Locale.US, "%.1f km", distKm),
                    etaText = "$etaMinutes min ETA",
                    statusText = statusText,
                    reputationText = String.format(Locale.US, "%.1f", responder.reputationScore ?: 4.5f),
                    status = responder.status
                )
                Pair(uiModel, distKm)
            }.sortedBy { it.second }

            CommunityUiState.Success(
                activeCount = responders.size,
                responders = mappedWithDist.map { it.first }
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CommunityUiState.Idle("Loading responders...")
    )
}

sealed interface CommunityUiState {
    data class Idle(val message: String) : CommunityUiState
    data class Success(val activeCount: Int, val responders: List<ResponderUiModel>) : CommunityUiState
}
