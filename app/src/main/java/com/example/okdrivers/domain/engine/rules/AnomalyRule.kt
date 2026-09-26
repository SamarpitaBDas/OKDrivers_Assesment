package com.example.okdrivers.domain.engine.rules

import com.example.okdrivers.domain.engine.BaselineStatistics
import com.example.okdrivers.domain.model.AnomalyEvent
import com.example.okdrivers.domain.model.DriverState
import com.example.okdrivers.sensors.GpsLocationSample
import com.example.okdrivers.sensors.MotionSensorSample
import com.example.okdrivers.sensors.VehicleTelemetrySample

object AnomalyThresholds {
    // Centralized tunable thresholds for Section 18 demo and live tuning
    var hardBrakingGForceThreshold: Float = 0.85f
    var severeGForceThreshold: Float = 1.3f
    var rapidSpeedDropDeltaKmh: Float = 20f
    var minSpeedForEngineStopKmh: Float = 20f
    var highTempThresholdCelsius: Float = 120f
    var lowVoltageThreshold: Float = 11.0f

    fun resetToDefaults() {
        hardBrakingGForceThreshold = 0.85f
        severeGForceThreshold = 1.3f
        rapidSpeedDropDeltaKmh = 20f
        minSpeedForEngineStopKmh = 20f
        highTempThresholdCelsius = 120f
        lowVoltageThreshold = 11.0f
    }
}

data class AnomalyRuleConfig(
    val hardBrakingGForceThreshold: Float = AnomalyThresholds.hardBrakingGForceThreshold,
    val severeGForceThreshold: Float = AnomalyThresholds.severeGForceThreshold,
    val rapidSpeedDropDeltaKmh: Float = AnomalyThresholds.rapidSpeedDropDeltaKmh,
    val minSpeedForEngineStopKmh: Float = AnomalyThresholds.minSpeedForEngineStopKmh,
    val highTempThresholdCelsius: Float = AnomalyThresholds.highTempThresholdCelsius,
    val lowVoltageThreshold: Float = AnomalyThresholds.lowVoltageThreshold
)

data class RuleEvaluationContext(
    val driverId: String?,
    val vehicleId: String?,
    val vehicleTelemetry: VehicleTelemetrySample?,
    val driverState: DriverState?,
    val motionSensor: MotionSensorSample?,
    val gpsLocation: GpsLocationSample?,
    val driverBaseline: BaselineStatistics?,
    val vehicleBaseline: BaselineStatistics?,
    val recentHistory: List<AnomalyEvent>,
    val now: Long,
    val config: AnomalyRuleConfig
)

interface AnomalyRule {
    fun evaluate(context: RuleEvaluationContext): AnomalyEvent?
}
