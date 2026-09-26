package com.example.okdrivers.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.okdrivers.data.repository.ServiceStateRepository
import com.example.okdrivers.domain.engine.IncidentStateMachine
import com.example.okdrivers.domain.engine.LiveAnomalyStatusHolder
import com.example.okdrivers.domain.model.EmergencyState
import com.example.okdrivers.sensors.BatteryStatusManager
import com.example.okdrivers.sensors.NetworkStatusManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val emergencyState: EmergencyState,
    val isOnline: Boolean,
    val batteryPercentage: Int,
    val isCharging: Boolean,
    val anomalyConfidence: Float,
    val serviceRunning: Boolean
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    incidentStateMachine: IncidentStateMachine,
    networkStatusManager: NetworkStatusManager,
    batteryStatusManager: BatteryStatusManager,
    liveAnomalyStatusHolder: LiveAnomalyStatusHolder,
    private val serviceStateRepository: ServiceStateRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        incidentStateMachine.currentState,
        networkStatusManager.observeNetwork(),
        batteryStatusManager.observeBattery(),
        liveAnomalyStatusHolder.currentConfidence,
        serviceStateRepository.isServiceRunningFlow
    ) { state, network, battery, confidence, isRunning ->
        HomeUiState(
            emergencyState = state,
            isOnline = network.isOnline,
            batteryPercentage = battery.batteryPercentage,
            isCharging = battery.isCharging,
            anomalyConfidence = confidence,
            serviceRunning = isRunning
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(
            emergencyState = EmergencyState.NORMAL_OPERATION,
            isOnline = true,
            batteryPercentage = 100,
            isCharging = false,
            anomalyConfidence = 0f,
            serviceRunning = true
        )
    )

    fun setServiceRunning(running: Boolean) {
        viewModelScope.launch {
            serviceStateRepository.setServiceRunning(running)
        }
    }
}
