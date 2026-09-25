package com.example.okdrivers.domain.engine

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