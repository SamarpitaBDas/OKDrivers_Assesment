package com.example.okdrivers.domain.engine.rules

import com.example.okdrivers.domain.model.AnomalyEvent
import com.example.okdrivers.domain.model.AnomalySeverity
import com.example.okdrivers.domain.model.AnomalyType
import com.example.okdrivers.domain.model.DriverCondition
import java.util.UUID

// 1. Sudden Deceleration (Baseline-aware)
class SuddenDecelerationRule(
    private val anomalyConfidenceEngine: com.example.okdrivers.domain.engine.AnomalyConfidenceEngine
) : AnomalyRule {
    override fun evaluate(context: RuleEvaluationContext): AnomalyEvent? {
        val motion = context.motionSensor ?: return null
        val baseline = context.driverBaseline ?: return null

        val deviation = anomalyConfidenceEngine.calculateDriverGForceDeviation(motion.gForce, baseline)
        val isSuddenDecel = motion.gForce <= context.config.hardBrakingGForceThreshold ||
                (motion.gForce < baseline.averageGForce - 0.15f)

        if (isSuddenDecel || deviation.isOutsideNormalRange) {
            val isAlert = context.driverState == null || context.driverState.condition == DriverCondition.ALERT || context.driverState.isResponsive
            val conf = maxOf(deviation.confidence, 0.6f)
            return AnomalyEvent(
                id = UUID.randomUUID().toString(),
                timestamp = context.now,
                type = AnomalyType.HARD_BRAKING,
                severity = if (motion.gForce <= 0.7f) AnomalySeverity.HIGH else AnomalySeverity.MEDIUM,
                confidence = conf,
                reason = "Sudden deceleration detected (G-Force: %.2fG, deviation: %.2f sigma)".format(motion.gForce, deviation.deviationScore),
                latitude = context.gpsLocation?.latitude,
                longitude = context.gpsLocation?.longitude,
                driverId = context.driverId,
                vehicleId = context.vehicleId,
                sensorSampleTimestamp = motion.timestamp,
                telemetryTimestamp = context.vehicleTelemetry?.timestamp,
                requiresVerification = !(isSuddenDecel && motion.gForce <= 0.85f && isAlert),
                isEscalated = false
            )
        }
        return null
    }
}

// 2. Severe G-Force (Baseline-aware)
class SevereGForceRule(
    private val anomalyConfidenceEngine: com.example.okdrivers.domain.engine.AnomalyConfidenceEngine
) : AnomalyRule {
    override fun evaluate(context: RuleEvaluationContext): AnomalyEvent? {
        val motion = context.motionSensor ?: return null
        val baseline = context.driverBaseline ?: return null

        val deviation = anomalyConfidenceEngine.calculateDriverGForceDeviation(motion.gForce, baseline)
        val isSevere = motion.gForce >= context.config.severeGForceThreshold || motion.gForce <= 0.3f ||
                (deviation.isOutsideNormalRange && deviation.deviationScore >= 3.5f && (motion.gForce < 0.4f || motion.gForce > 1.8f))

        if (isSevere) {
            val conf = maxOf(deviation.confidence, 0.75f)
            return AnomalyEvent(
                id = UUID.randomUUID().toString(),
                timestamp = context.now,
                type = AnomalyType.SEVERE_G_FORCE,
                severity = AnomalySeverity.HIGH,
                confidence = conf,
                reason = "Severe G-Force detected: %.2fG (Z-score: %.1f)".format(motion.gForce, deviation.deviationScore),
                latitude = context.gpsLocation?.latitude,
                longitude = context.gpsLocation?.longitude,
                driverId = context.driverId,
                vehicleId = context.vehicleId,
                sensorSampleTimestamp = motion.timestamp,
                telemetryTimestamp = context.vehicleTelemetry?.timestamp,
                requiresVerification = true,
                isEscalated = false
            )
        }
        return null
    }
}

// 3. Rapid Speed Drop (Baseline-aware)
class RapidSpeedDropRule(
    private val anomalyConfidenceEngine: com.example.okdrivers.domain.engine.AnomalyConfidenceEngine
) : AnomalyRule {
    override fun evaluate(context: RuleEvaluationContext): AnomalyEvent? {
        val telemetry = context.vehicleTelemetry ?: return null
        val baseline = context.vehicleBaseline ?: return null

        val deviation = anomalyConfidenceEngine.calculateVehicleSpeedDeviation(telemetry.speedKmh, baseline)
        val speedDrop = baseline.averageSpeedKmh - telemetry.speedKmh
        if (speedDrop >= context.config.rapidSpeedDropDeltaKmh || (deviation.isOutsideNormalRange && speedDrop > 10f)) {
            return AnomalyEvent(
                id = UUID.randomUUID().toString(),
                timestamp = context.now,
                type = AnomalyType.RAPID_SPEED_DROP,
                severity = AnomalySeverity.MEDIUM,
                confidence = deviation.confidence,
                reason = "Rapid speed drop detected: down by %.1f km/h from baseline %.1f km/h".format(speedDrop, baseline.averageSpeedKmh),
                latitude = context.gpsLocation?.latitude,
                longitude = context.gpsLocation?.longitude,
                driverId = context.driverId,
                vehicleId = context.vehicleId,
                sensorSampleTimestamp = null,
                telemetryTimestamp = telemetry.timestamp,
                requiresVerification = false,
                isEscalated = false
            )
        }
        return null
    }
}

// 4. RPM Drop / Engine Stop (Static threshold)
class EngineStopRule : AnomalyRule {
    override fun evaluate(context: RuleEvaluationContext): AnomalyEvent? {
        val telemetry = context.vehicleTelemetry ?: return null
        if (telemetry.rpm == 0f && telemetry.speedKmh > context.config.minSpeedForEngineStopKmh) {
            return AnomalyEvent(
                id = UUID.randomUUID().toString(),
                timestamp = context.now,
                type = AnomalyType.ENGINE_STOP,
                severity = AnomalySeverity.CRITICAL,
                confidence = 0.95f,
                reason = "Engine stopped unexpectedly while moving at %.1f km/h (RPM = 0)".format(telemetry.speedKmh),
                latitude = context.gpsLocation?.latitude,
                longitude = context.gpsLocation?.longitude,
                driverId = context.driverId,
                vehicleId = context.vehicleId,
                sensorSampleTimestamp = null,
                telemetryTimestamp = telemetry.timestamp,
                requiresVerification = true,
                isEscalated = true
            )
        }
        return null
    }
}

// 5. Simulated Airbag Deployment (Static threshold)
class AirbagDeploymentRule : AnomalyRule {
    override fun evaluate(context: RuleEvaluationContext): AnomalyEvent? {
        val telemetry = context.vehicleTelemetry ?: return null
        if (telemetry.airbagDeployed) {
            return AnomalyEvent(
                id = UUID.randomUUID().toString(),
                timestamp = context.now,
                type = AnomalyType.CRITICAL_VEHICLE_ANOMALY,
                severity = AnomalySeverity.CRITICAL,
                confidence = 0.99f,
                reason = "Simulated airbag deployment signal received from vehicle telemetry",
                latitude = context.gpsLocation?.latitude,
                longitude = context.gpsLocation?.longitude,
                driverId = context.driverId,
                vehicleId = context.vehicleId,
                sensorSampleTimestamp = context.motionSensor?.timestamp,
                telemetryTimestamp = telemetry.timestamp,
                requiresVerification = true,
                isEscalated = true
            )
        }
        return null
    }
}

// 6. Driver Unresponsive After Alert (Phase 7 AI verification dependency stub)
class DriverUnresponsiveRule : AnomalyRule {
    override fun evaluate(context: RuleEvaluationContext): AnomalyEvent? {
        val driverState = context.driverState ?: return null
        // TODO: Wire to Phase 7 AI voice prompt response verification state when available
        if (driverState.condition == DriverCondition.UNRESPONSIVE || !driverState.isResponsive) {
            return AnomalyEvent(
                id = UUID.randomUUID().toString(),
                timestamp = context.now,
                type = AnomalyType.DRIVER_UNRESPONSIVE,
                severity = AnomalySeverity.CRITICAL,
                confidence = 0.95f,
                reason = "Driver unresponsive after alert / impaired driver state (Attention: %.2f)".format(driverState.attentionScore),
                latitude = context.gpsLocation?.latitude,
                longitude = context.gpsLocation?.longitude,
                driverId = context.driverId,
                vehicleId = context.vehicleId,
                sensorSampleTimestamp = null,
                telemetryTimestamp = context.vehicleTelemetry?.timestamp,
                requiresVerification = true,
                isEscalated = true
            )
        }
        return null
    }
}

// 7. Critical Vehicle Parameter Anomaly (Static threshold)
class CriticalVehicleParameterRule : AnomalyRule {
    override fun evaluate(context: RuleEvaluationContext): AnomalyEvent? {
        val telemetry = context.vehicleTelemetry ?: return null
        val isFault = !telemetry.diagnosticFault.isNullOrBlank()
        val isHighTemp = telemetry.engineTemperatureCelsius >= context.config.highTempThresholdCelsius
        val isLowVoltage = telemetry.batteryVoltage <= context.config.lowVoltageThreshold

        if (isFault || isHighTemp || isLowVoltage) {
            val reason = when {
                isFault -> "Diagnostic fault reported: ${telemetry.diagnosticFault}"
                isHighTemp -> "Critical engine temperature: %.1f°C".format(telemetry.engineTemperatureCelsius)
                else -> "Low battery voltage: %.1fV".format(telemetry.batteryVoltage)
            }
            return AnomalyEvent(
                id = UUID.randomUUID().toString(),
                timestamp = context.now,
                type = AnomalyType.CRITICAL_VEHICLE_ANOMALY,
                severity = AnomalySeverity.HIGH,
                confidence = 0.85f,
                reason = reason,
                latitude = context.gpsLocation?.latitude,
                longitude = context.gpsLocation?.longitude,
                driverId = context.driverId,
                vehicleId = context.vehicleId,
                sensorSampleTimestamp = null,
                telemetryTimestamp = telemetry.timestamp,
                requiresVerification = true,
                isEscalated = false
            )
        }
        return null
    }
}

// 8. Multiple Signals Combined (Multi-pass aggregator running after rules 1–7)
class MultipleSignalsCombinedRule(
    private val triggeredEvents: List<AnomalyEvent>
) : AnomalyRule {
    override fun evaluate(context: RuleEvaluationContext): AnomalyEvent? {
        if (triggeredEvents.size >= 2) {
            val conf = java.lang.Math.min(1.0f, 0.8f + (triggeredEvents.size * 0.05f))
            return AnomalyEvent(
                id = UUID.randomUUID().toString(),
                timestamp = context.now,
                type = AnomalyType.MULTIPLE_ABNORMAL_SIGNALS,
                severity = AnomalySeverity.HIGH,
                confidence = conf,
                reason = "Multiple abnormal signals combined (%d concurrent triggers fired)".format(triggeredEvents.size),
                latitude = context.gpsLocation?.latitude,
                longitude = context.gpsLocation?.longitude,
                driverId = context.driverId,
                vehicleId = context.vehicleId,
                sensorSampleTimestamp = context.motionSensor?.timestamp,
                telemetryTimestamp = context.vehicleTelemetry?.timestamp,
                requiresVerification = true,
                isEscalated = false
            )
        }
        return null
    }
}
