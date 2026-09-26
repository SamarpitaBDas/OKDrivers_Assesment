package com.example.okdrivers.ui.community

import com.example.okdrivers.domain.model.ResponderStatus

data class ResponderUiModel(
    val id: String,
    val name: String,
    val initials: String,
    val distanceText: String,
    val etaText: String,
    val statusText: String,
    val reputationText: String,
    val status: ResponderStatus
)
