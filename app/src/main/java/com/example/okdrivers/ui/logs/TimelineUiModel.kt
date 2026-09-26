package com.example.okdrivers.ui.logs

data class TimelineUiModel(
    val id: String,
    val timestampMillis: Long,
    val timeText: String,
    val reasonText: String
)
