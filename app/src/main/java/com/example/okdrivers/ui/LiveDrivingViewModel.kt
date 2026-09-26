package com.example.okdrivers.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.okdrivers.data.repository.SensorRepository
import com.example.okdrivers.data.repository.TelemetryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class LiveDrivingUiState(
    val speedKmh: Float?,
    val rpm: Int?,
    val gForce: Float?,
    val latitude: Double?,
    val longitude: Double?,
    val heading: Float?,
    val gpsAvailable: Boolean
)

@HiltViewModel
class LiveDrivingViewModel @Inject constructor(
    sensorRepository: SensorRepository,
    telemetryRepository: TelemetryRepository
) : ViewModel() {

    val uiState: StateFlow<LiveDrivingUiState> = combine(
        sensorRepository.observeMotion(),
        sensorRepository.observeGps(),
        telemetryRepository.observeTelemetry()
    ) { motion, gps, telemetryList ->
        val latestTelemetry = telemetryList.firstOrNull()
        LiveDrivingUiState(
            speedKmh = latestTelemetry?.speedKmh ?: gps.speedMetersPerSecond * 3.6f,
            rpm = latestTelemetry?.rpm?.toInt(),
            gForce = motion.gForce,
            latitude = gps.latitude,
            longitude = gps.longitude,
            heading = gps.headingDegrees,
            gpsAvailable = true
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LiveDrivingUiState(
            speedKmh = null,
            rpm = null,
            gForce = null,
            latitude = null,
            longitude = null,
            heading = null,
            gpsAvailable = false
        )
    )
}
