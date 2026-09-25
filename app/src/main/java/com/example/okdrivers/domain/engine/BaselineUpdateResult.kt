package com.example.okdrivers.domain.engine

data class BaselineUpdateResult(
    val baseline: BaselineStatistics,
    val updated: Boolean,
    val updateTimestamp: Long
)