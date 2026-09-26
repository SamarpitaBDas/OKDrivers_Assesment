package com.example.okdrivers.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.okdrivers.data.repository.SensorRepository
import com.example.okdrivers.domain.engine.DriverStateEngine
import com.example.okdrivers.domain.model.DriverCondition
import com.example.okdrivers.domain.model.DriverState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DriverStateUiState(
    val perclos: Float,
    val gazeDirection: String,
    val headPitch: Float,
    val headYaw: Float,
    val headRoll: Float,
    val blinkRate: Float,
    val yawnDetected: Boolean,
    val attentionScore: Float,
    val isResponsive: Boolean,
    val condition: DriverCondition
)

@HiltViewModel
class DriverStateViewModel @Inject constructor(
    sensorRepository: SensorRepository,
    driverStateEngine: DriverStateEngine
) : ViewModel() {

    val uiState: StateFlow<DriverStateUiState> = sensorRepository.observeDms()
        .map { dmsSample ->
            val state: DriverState = driverStateEngine.process(dmsSample)
            DriverStateUiState(
                perclos = state.perclos,
                gazeDirection = state.gazeDirection,
                headPitch = state.headPitch,
                headYaw = state.headYaw,
                headRoll = state.headRoll,
                blinkRate = state.blinkRate,
                yawnDetected = state.yawnDetected,
                attentionScore = state.attentionScore,
                isResponsive = state.isResponsive,
                condition = state.condition
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DriverStateUiState(
                perclos = 0.05f,
                gazeDirection = "CENTER",
                headPitch = 0f,
                headYaw = 0f,
                headRoll = 0f,
                blinkRate = 15f,
                yawnDetected = false,
                attentionScore = 0.95f,
                isResponsive = true,
                condition = DriverCondition.ALERT
            )
        )
}
