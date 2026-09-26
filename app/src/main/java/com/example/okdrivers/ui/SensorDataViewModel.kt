package com.example.okdrivers.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.okdrivers.data.repository.SensorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SensorDataUiState(
    val accelX: Float,
    val accelY: Float,
    val accelZ: Float,
    val gForce: Float,
    val gyroX: Float,
    val gyroY: Float,
    val gyroZ: Float,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Float,
    val heading: Float,
    val batteryPercentage: Int,
    val isCharging: Boolean,
    val isOnline: Boolean,
    val telemetrySpeed: Float,
    val telemetryRpm: Float,
    val engineLoad: Float,
    val engineTemp: Float,
    val timestamp: Long
)

@HiltViewModel
class SensorDataViewModel @Inject constructor(
    sensorRepository: SensorRepository
) : ViewModel() {

    val uiState: StateFlow<SensorDataUiState> = combine(
        sensorRepository.observeMotion(),
        sensorRepository.observeGps(),
        sensorRepository.observeBattery(),
        sensorRepository.observeNetwork(),
        sensorRepository.observeVehicleTelemetry()
    ) { motion, gps, battery, network, telemetry ->
        SensorDataUiState(
            accelX = motion.accelerationX,
            accelY = motion.accelerationY,
            accelZ = motion.accelerationZ,
            gForce = motion.gForce,
            gyroX = motion.gyroX,
            gyroY = motion.gyroY,
            gyroZ = motion.gyroZ,
            latitude = gps.latitude,
            longitude = gps.longitude,
            speedKmh = gps.speedMetersPerSecond * 3.6f,
            heading = gps.headingDegrees,
            batteryPercentage = battery.batteryPercentage,
            isCharging = battery.isCharging,
            isOnline = network.isOnline,
            telemetrySpeed = telemetry.speedKmh,
            telemetryRpm = telemetry.rpm,
            engineLoad = telemetry.engineLoad,
            engineTemp = telemetry.engineTemperatureCelsius,
            timestamp = System.currentTimeMillis()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SensorDataUiState(
            0f, 0f, 0f, 0f, 0f, 0f, 0f,
            0.0, 0.0, 0f, 0f,
            100, false, true,
            0f, 0f, 0f, 0f,
            System.currentTimeMillis()
        )
    )
}
