package com.example.okdrivers.domain.engine

import com.example.okdrivers.domain.model.AnomalyEvent
import com.example.okdrivers.domain.model.AnomalySeverity
import com.example.okdrivers.domain.model.AnomalyType

data class AnomalyDetectionResult(
    val events: List<AnomalyEvent>,
    val overallConfidence: Float,
    val overallSeverity: AnomalySeverity,
    val requiresVerification: Boolean,
    val isEscalated: Boolean
) {
    // Convenience properties for callers and tests
    val classifications: List<AnomalyType> = events.map { it.type }
    val reasoning: List<String> = events.map { it.reason }
    val confidence: Float = overallConfidence
    val severity: AnomalySeverity = overallSeverity
}
