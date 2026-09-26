package com.example.okdrivers.domain.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveAnomalyStatusHolder @Inject constructor() {
    private val _currentConfidence = MutableStateFlow(0f)
    val currentConfidence: StateFlow<Float> = _currentConfidence.asStateFlow()

    fun updateConfidence(confidence: Float) {
        _currentConfidence.value = confidence.coerceIn(0f, 1f)
    }
}
