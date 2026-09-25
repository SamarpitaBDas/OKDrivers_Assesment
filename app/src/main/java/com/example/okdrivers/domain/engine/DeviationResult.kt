package com.example.okdrivers.domain.engine

data class DeviationResult(
    val deviationScore: Float,
    val confidence: Float,
    val isOutsideNormalRange: Boolean
)