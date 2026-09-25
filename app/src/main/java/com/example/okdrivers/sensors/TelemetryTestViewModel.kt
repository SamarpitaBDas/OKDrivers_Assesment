package com.example.okdrivers.sensors.test

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.okdrivers.sensors.VehicleTelemetrySimulator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TelemetryTestViewModel @Inject constructor(
    private val vehicleTelemetrySimulator: VehicleTelemetrySimulator
) : ViewModel() {

    init {
        startTelemetryTest()
    }

    private fun startTelemetryTest() {

        viewModelScope.launch {

            vehicleTelemetrySimulator
                .observeTelemetry()
                .collect { telemetry ->

                    Log.d(
                        "OKDRIVER_TELEMETRY",
                        """
                        Speed: ${telemetry.speedKmh} km/h
                        RPM: ${telemetry.rpm}
                        Load: ${telemetry.engineLoad}%
                        Throttle: ${telemetry.throttlePosition}%
                        Temp: ${telemetry.engineTemperatureCelsius}°C
                        Voltage: ${telemetry.batteryVoltage}V
                        Fault: ${telemetry.diagnosticFault}
                        """.trimIndent()
                    )
                }
        }
    }
}