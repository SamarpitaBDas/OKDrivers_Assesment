package com.example.okdrivers.domain.engine

data class BaselineStatistics(
    val sampleCount: Int,
    val averageSpeedKmh: Float,
    val minimumSpeedKmh: Float,
    val maximumSpeedKmh: Float,
    val averageGForce: Float,
    val minimumGForce: Float,
    val maximumGForce: Float,
    val averageRpm: Float,
    val minimumRpm: Float,
    val maximumRpm: Float,
    val averageEngineLoad: Float,
    val minimumEngineLoad: Float,
    val maximumEngineLoad: Float,
    val updatedAt: Long
)