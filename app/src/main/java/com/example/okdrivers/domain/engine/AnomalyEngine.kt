package com.example.okdrivers.domain.engine

import com.example.okdrivers.domain.engine.rules.*
import com.example.okdrivers.domain.model.AnomalyEvent
import com.example.okdrivers.domain.model.AnomalySeverity
import com.example.okdrivers.domain.model.AnomalyType
import com.example.okdrivers.sensors.GpsLocationSample
import com.example.okdrivers.sensors.MotionSensorSample
import com.example.okdrivers.sensors.VehicleTelemetrySample
import java.util.UUID
import javax.inject.Inject

class AnomalyEngine @Inject constructor(
    anomalyConfidenceEngine: AnomalyConfidenceEngine
) {

    private val ruleConfig = AnomalyRuleConfig()

    private val rules1To7: List<AnomalyRule> = listOf(
        SuddenDecelerationRule(anomalyConfidenceEngine),
        SevereGForceRule(anomalyConfidenceEngine),
        RapidSpeedDropRule(anomalyConfidenceEngine),
        EngineStopRule(),
        AirbagDeploymentRule(),
        DriverUnresponsiveRule(),
        CriticalVehicleParameterRule()
    )

    fun evaluate(
        driverId: String? = null,
        vehicleId: String? = null,
        vehicleTelemetry: VehicleTelemetrySample?,
        driverState: DriverState?,
        motionSensor: MotionSensorSample?,
        gpsLocation: GpsLocationSample?,
        driverBaseline: BaselineStatistics?,
        vehicleBaseline: BaselineStatistics?,
        recentHistory: List<AnomalyEvent> = emptyList(),
        now: Long = System.currentTimeMillis()
    ): AnomalyDetectionResult {

        val context = RuleEvaluationContext(
            driverId = driverId,
            vehicleId = vehicleId,
            vehicleTelemetry = vehicleTelemetry,
            driverState = driverState,
            motionSensor = motionSensor,
            gpsLocation = gpsLocation,
            driverBaseline = driverBaseline,
            vehicleBaseline = vehicleBaseline,
            recentHistory = recentHistory,
            now = now,
            config = ruleConfig
        )

        val events = mutableListOf<AnomalyEvent>()

        // Pass 1: Run rules 1-7
        for (rule in rules1To7) {
            rule.evaluate(context)?.let { events.add(it) }
        }

        // Pass 2: Run Rule 8 (Multiple Signals Combined) against collected events
        val combinedRule = MultipleSignalsCombinedRule(events)
        combinedRule.evaluate(context)?.let { events.add(it) }

        var maxConfidence = events.maxOfOrNull { it.confidence } ?: 0f

        // Recent history compounding factor
        val recentWindowCount = recentHistory.count { now - it.timestamp < 30_000 }
        if (recentWindowCount > 0) {
            maxConfidence = minOf(1.0f, maxConfidence + (recentWindowCount * 0.1f))
        }

        val overallSeverity = when {
            events.any { it.severity == AnomalySeverity.CRITICAL } -> AnomalySeverity.CRITICAL
            events.any { it.severity == AnomalySeverity.HIGH } -> AnomalySeverity.HIGH
            events.any { it.severity == AnomalySeverity.MEDIUM } -> AnomalySeverity.MEDIUM
            events.isNotEmpty() -> events.maxOf { it.severity }
            else -> AnomalySeverity.LOW
        }

        val isAlertDriver = driverState == null || driverState.condition == DriverCondition.ALERT || driverState.isResponsive
        val isStandardBrakingOnly = events.size == 1 && events.any { it.type == AnomalyType.HARD_BRAKING } && motionSensor != null && motionSensor.gForce <= 0.85f

        val requiresVerification = when {
            isStandardBrakingOnly && isAlertDriver -> false
            overallSeverity == AnomalySeverity.LOW -> false
            else -> true
        }

        val isEscalated = overallSeverity == AnomalySeverity.CRITICAL && maxConfidence >= 0.9f

        if (events.isEmpty()) {
            events.add(
                AnomalyEvent(
                    id = UUID.randomUUID().toString(),
                    timestamp = now,
                    type = AnomalyType.UNKNOWN,
                    severity = AnomalySeverity.LOW,
                    confidence = 0f,
                    reason = "Normal driving operation observed within expected baseline parameters",
                    latitude = gpsLocation?.latitude,
                    longitude = gpsLocation?.longitude,
                    driverId = driverId,
                    vehicleId = vehicleId,
                    sensorSampleTimestamp = motionSensor?.timestamp,
                    telemetryTimestamp = vehicleTelemetry?.timestamp,
                    requiresVerification = false,
                    isEscalated = false
                )
            )
        }

        return AnomalyDetectionResult(
            events = events.distinctBy { it.type },
            overallConfidence = maxConfidence,
            overallSeverity = overallSeverity,
            requiresVerification = requiresVerification,
            isEscalated = isEscalated
        )
    }
}

typealias DriverState = com.example.okdrivers.domain.model.DriverState
typealias DriverCondition = com.example.okdrivers.domain.model.DriverCondition
