package com.example.okdrivers.domain.engine

import com.example.okdrivers.domain.model.SafetyBaseline
import java.util.UUID
import kotlin.math.min

data class BaselineStatistics(
    val sampleCount: Int,
    val averageSpeedKmh: Float,
    val minimumSpeedKmh: Float,
    val maximumSpeedKmh: Float,
    val speedStdDev: Float,
    val averageGForce: Float,
    val minimumGForce: Float,
    val maximumGForce: Float,
    val gForceStdDev: Float,
    val averageRpm: Float,
    val minimumRpm: Float,
    val maximumRpm: Float,
    val rpmStdDev: Float,
    val averageEngineLoad: Float,
    val minimumEngineLoad: Float,
    val maximumEngineLoad: Float,
    val engineLoadStdDev: Float,
    val updatedAt: Long
)

fun SafetyBaseline.toBaselineStatistics(): BaselineStatistics {
    return BaselineStatistics(
        sampleCount = sampleCount,
        averageSpeedKmh = averageSpeedKmh,
        minimumSpeedKmh = averageSpeedKmh,
        maximumSpeedKmh = averageSpeedKmh,
        speedStdDev = 0.05f,
        averageGForce = averageBrakingG,
        minimumGForce = averageBrakingG,
        maximumGForce = maximumNormalGForce,
        gForceStdDev = 0.05f,
        averageRpm = averageRpm.toFloat(),
        minimumRpm = averageRpm.toFloat(),
        maximumRpm = averageRpm.toFloat(),
        rpmStdDev = 0f,
        averageEngineLoad = averageEngineLoad,
        minimumEngineLoad = averageEngineLoad,
        maximumEngineLoad = averageEngineLoad,
        engineLoadStdDev = 0f,
        updatedAt = updatedAt
    )
}

fun BaselineStatistics.toSafetyBaseline(
    driverId: String,
    vehicleId: String,
    existingBaseline: SafetyBaseline?
): SafetyBaseline {
    return SafetyBaseline(
        id = existingBaseline?.id ?: UUID.randomUUID().toString(),
        driverId = driverId,
        vehicleId = vehicleId,
        averageSpeedKmh = averageSpeedKmh,
        averageRpm = averageRpm.toInt(),
        averageEngineLoad = averageEngineLoad,
        averageBrakingG = averageGForce,
        maximumNormalGForce = maximumGForce,
        sampleCount = sampleCount,
        confidence = min(1.0f, sampleCount / 50f),
        createdAt = existingBaseline?.createdAt ?: updatedAt,
        updatedAt = updatedAt
    )
}
