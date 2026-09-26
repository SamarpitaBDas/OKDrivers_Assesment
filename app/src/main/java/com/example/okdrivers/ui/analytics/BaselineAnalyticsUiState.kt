package com.example.okdrivers.ui.analytics

data class BaselineAnalyticsUiState(
    val isLoading: Boolean = true,
    val hasBaseline: Boolean = false,
    val sampleCount: Int = 0,
    val confidencePercent: Int = 0,
    val averageSpeedKmh: Float? = null,
    val averageBrakingG: Float? = null,
    val maximumNormalGForce: Float? = null,
    val averageRpm: Int? = null,
    val averageEngineLoad: Float? = null,
    val lastUpdatedText: String = "—",
    val currentGForceDeviation: Float? = null,
    val currentSpeedDeviation: Float? = null,
    val isCurrentlyOutsideNormalRange: Boolean = false
)
