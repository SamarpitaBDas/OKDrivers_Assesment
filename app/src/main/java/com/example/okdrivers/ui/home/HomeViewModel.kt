package com.example.okdrivers.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.okdrivers.data.repository.SensorRepository
import com.example.okdrivers.data.repository.ServiceStateRepository
import com.example.okdrivers.domain.engine.DriverStateEngine
import com.example.okdrivers.domain.engine.IncidentStateMachine
import com.example.okdrivers.domain.engine.LiveAnomalyStatusHolder
import com.example.okdrivers.domain.model.DriverCondition
import com.example.okdrivers.domain.model.EmergencyState
import com.example.okdrivers.sensors.DmsSample
import com.example.okdrivers.sensors.GazeDirection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class HomeUiState(
    val emergencyState: EmergencyState,
    val isOnline: Boolean,
    val batteryPercentage: Int,
    val isCharging: Boolean,
    val anomalyConfidence: Float,
    val serviceRunning: Boolean,
    val driverCondition: DriverCondition,
    val driverAttentionScore: Float,
    val telemetryStatusText: String,
    val telemetryIsCritical: Boolean
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    incidentStateMachine: IncidentStateMachine,
    sensorRepository: SensorRepository,
    driverStateEngine: DriverStateEngine,
    liveAnomalyStatusHolder: LiveAnomalyStatusHolder,
    private val serviceStateRepository: ServiceStateRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        incidentStateMachine.currentState,
        liveAnomalyStatusHolder.currentConfidence,
        serviceStateRepository.isServiceRunningFlow,
        sensorRepository.observeSensorSnapshot()
    ) { state, confidence, isRunning, snapshot ->
        val network = snapshot.network
        val battery = snapshot.battery
        val telemetry = snapshot.vehicleTelemetry
        val dmsSample = snapshot.dms ?: DmsSample(
            timestamp = System.currentTimeMillis(),
            perclos = 0.08f,
            gazeDirection = GazeDirection.FORWARD,
            headPitch = 0f, headYaw = 0f, headRoll = 0f,
            blinkRate = 14f, yawnDetected = false, gazeAwayDurationMs = 0L,
            attentionScore = 0.95f, isResponsive = true, condition = DriverCondition.ALERT
        )

        val driverState = driverStateEngine.process(dmsSample)

        val airbag = telemetry?.airbagDeployed == true
        val rpm = telemetry?.rpm ?: 1800f
        val speed = telemetry?.speedKmh ?: 45f

        val telemetryIsCritical = airbag || (rpm == 0f && (speed > 10f || state != EmergencyState.NORMAL_OPERATION))
        val telemetryText = when {
            airbag -> "CRITICAL - Airbag Deployed!"
            rpm == 0f && speed > 10f -> "CRITICAL - Engine Stalled (RPM 0)"
            rpm == 0f && state != EmergencyState.NORMAL_OPERATION -> "CRITICAL - Vehicle Stopped (RPM 0)"
            speed <= 25f && telemetry != null && telemetry.throttlePosition == 0f && telemetry.engineLoad <= 15f -> String.format(Locale.US, "Hard Braking (%.1f km/h)", speed)
            else -> String.format(Locale.US, "Normal (%.1f km/h)", speed)
        }

        HomeUiState(
            emergencyState = state,
            isOnline = network?.isOnline ?: true,
            batteryPercentage = battery?.batteryPercentage ?: 100,
            isCharging = battery?.isCharging ?: false,
            anomalyConfidence = confidence,
            serviceRunning = isRunning,
            driverCondition = driverState.condition,
            driverAttentionScore = driverState.attentionScore,
            telemetryStatusText = telemetryText,
            telemetryIsCritical = telemetryIsCritical
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
            serviceRunning = true,
            driverCondition = DriverCondition.ALERT,
            driverAttentionScore = 0.95f,
            telemetryStatusText = "Normal (45.0 km/h)",
            telemetryIsCritical = false
        )
    )

    fun setServiceRunning(running: Boolean) {
        viewModelScope.launch {
            serviceStateRepository.setServiceRunning(running)
        }
    }
}
