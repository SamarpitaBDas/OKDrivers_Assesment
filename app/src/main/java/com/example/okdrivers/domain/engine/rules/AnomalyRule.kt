package com.example.okdrivers.domain.engine.rules

import com.example.okdrivers.domain.engine.BaselineStatistics
import com.example.okdrivers.domain.model.AnomalyEvent
import com.example.okdrivers.domain.model.DriverState
import com.example.okdrivers.sensors.GpsLocationSample
import com.example.okdrivers.sensors.MotionSensorSample
import com.example.okdrivers.sensors.VehicleTelemetrySample

data class AnomalyRuleConfig(
    val hardBrakingGForceThreshold: Float = 0.85f,
    val severeGForceThreshold: Float = 1.3f,
    val rapidSpeedDropDeltaKmh: Float = 20f,
    val minSpeedForEngineStopKmh: Float = 20f,
    val highTempThresholdCelsius: Float = 120f,
    val lowVoltageThreshold: Float = 11.0f
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
