package com.example.okdrivers.ui.profile

data class DriverProfileUiState(
    val isLoading: Boolean = true,
    val driverId: String? = null,
    val name: String = "",
    val phoneNumber: String = "",
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val memberSinceText: String = "",
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: String? = null
)
