package com.example.okdrivers.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.okdrivers.data.repository.BaselineRepository
import com.example.okdrivers.data.repository.CurrentProfileRepository
import com.example.okdrivers.data.repository.SensorRepository
import com.example.okdrivers.data.repository.TelemetryRepository
import com.example.okdrivers.domain.engine.DeviationCalculator
import com.example.okdrivers.domain.model.SafetyBaseline
import com.example.okdrivers.sensors.MotionSensorSample
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
class BaselineAnalyticsViewModel @Inject constructor(
    private val baselineRepository: BaselineRepository,
    private val currentProfileRepository: CurrentProfileRepository,
    private val sensorRepository: SensorRepository,
    private val telemetryRepository: TelemetryRepository
) : ViewModel() {

    val uiState: StateFlow<BaselineAnalyticsUiState> = combine(
        currentProfileRepository.observeCurrentDriverId(),
        currentProfileRepository.observeCurrentVehicleId()
    ) { driverId, vehicleId -> Pair(driverId, vehicleId) }
        .flatMapLatest { (driverId, vehicleId) ->
            combine(
                baselineRepository.observeBaseline(driverId, vehicleId),
                sensorRepository.observeMotion(),
                telemetryRepository.observeTelemetry()
            ) { baseline, motionSample, telemetryList ->
                mapState(baseline, motionSample, telemetryList.firstOrNull())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BaselineAnalyticsUiState()
        )

    init {
        viewModelScope.launch {
            val driverId = "driver_1"
            val vehicleId = "vehicle_1"
            val existing = baselineRepository.getBaseline(driverId, vehicleId)
            if (existing == null) {
                val now = System.currentTimeMillis()
                val seeded = SafetyBaseline(
                    id = "baseline_1",
                    driverId = driverId,
                    vehicleId = vehicleId,
                    averageSpeedKmh = 42.5f,
                    averageRpm = 2100,
                    averageEngineLoad = 35.0f,
                    averageBrakingG = 0.22f,
                    maximumNormalGForce = 0.45f,
                    sampleCount = 28,
                    confidence = 0.56f,
                    createdAt = now,
                    updatedAt = now
                )
                baselineRepository.saveBaseline(seeded)
            }
        }
    }

    private fun mapState(
        baseline: SafetyBaseline?,
        motion: MotionSensorSample?,
        telemetry: com.example.okdrivers.domain.model.VehicleTelemetry?
    ): BaselineAnalyticsUiState {
        if (baseline == null) {
            return BaselineAnalyticsUiState(isLoading = false, hasBaseline = false)
        }

        val sampleCount = baseline.sampleCount
        val hasSufficientSamples = sampleCount >= 10
        val confidencePercent = ((baseline.confidence).coerceIn(0f, 1f) * 100f).toInt()

        val currentGForce = motion?.gForce ?: baseline.averageBrakingG
        val currentSpeed = telemetry?.speedKmh ?: baseline.averageSpeedKmh

        val gForceDeviation = DeviationCalculator.computeDeviation(
            currentValue = currentGForce,
            mean = baseline.averageBrakingG,
            stdDev = 0.08f,
            maxVal = baseline.maximumNormalGForce
        )

        val speedDeviation = DeviationCalculator.computeDeviation(
            currentValue = currentSpeed,
            mean = baseline.averageSpeedKmh,
            stdDev = 5.0f
        )

        val isOutside = gForceDeviation.isOutsideNormalRange || speedDeviation.isOutsideNormalRange

        return BaselineAnalyticsUiState(
            isLoading = false,
            hasBaseline = hasSufficientSamples,
            sampleCount = sampleCount,
            confidencePercent = confidencePercent,
            averageSpeedKmh = baseline.averageSpeedKmh,
            averageRpm = baseline.averageRpm,
            averageEngineLoad = baseline.averageEngineLoad,
            averageBrakingG = baseline.averageBrakingG,
            maximumNormalGForce = baseline.maximumNormalGForce,
            lastUpdatedText = formatTime(baseline.updatedAt),
            currentGForceDeviation = gForceDeviation.deviationScore,
            currentSpeedDeviation = speedDeviation.deviationScore,
            isCurrentlyOutsideNormalRange = isOutside
        )
    }

    companion object {
        fun formatTime(timestamp: Long): String {
            if (timestamp <= 0L) return "—"
            val sdf = SimpleDateFormat("HH:mm:ss 'on' MMM dd, yyyy", Locale.US)
            return sdf.format(Date(timestamp))
        }
    }
}
