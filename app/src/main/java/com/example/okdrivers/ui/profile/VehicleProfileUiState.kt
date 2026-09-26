package com.example.okdrivers.ui.profile

data class VehicleProfileUiState(
    val isLoading: Boolean = true,
    val vehicleId: String? = null,
    val registrationNumber: String = "",
    val make: String = "",
    val model: String = "",
    val year: String = "",
    val linkedDriverName: String? = null,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: String? = null
)
