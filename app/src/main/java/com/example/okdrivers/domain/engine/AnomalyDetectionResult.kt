package com.example.okdrivers.domain.engine

import com.example.okdrivers.domain.model.AnomalySeverity
import com.example.okdrivers.domain.model.AnomalyType

data class AnomalyDetectionResult(
    val confidence: Float,
    val severity: AnomalySeverity,
    val classifications: List<AnomalyType>,
    val requiresVerification: Boolean,
    val isEscalated: Boolean,
    val reasoning: List<String>
)
