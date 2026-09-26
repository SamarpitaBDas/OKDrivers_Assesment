package com.example.okdrivers.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.okdrivers.data.repository.CurrentProfileRepository
import com.example.okdrivers.data.repository.DriverRepository
import com.example.okdrivers.data.repository.ProfileInitializer
import com.example.okdrivers.domain.model.DriverProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DriverProfileViewModel @Inject constructor(
    private val driverRepository: DriverRepository,
    private val currentProfileRepository: CurrentProfileRepository,
    private val profileInitializer: ProfileInitializer
) : ViewModel() {

    private val isEditingFlow = MutableStateFlow(false)
    private val isSavingFlow = MutableStateFlow(false)
    private val saveErrorFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DriverProfileUiState> = currentProfileRepository.observeCurrentDriverId()
        .flatMapLatest { driverId ->
            driverRepository.observeDriver(driverId)
        }.combine(
            combine(isEditingFlow, isSavingFlow, saveErrorFlow) { editing, saving, error ->
                Triple(editing, saving, error)
            }
        ) { driver, (editing, saving, error) ->
            if (driver == null) {
                DriverProfileUiState(isLoading = true, saveError = error)
            } else {
                DriverProfileUiState(
                    isLoading = false,
                    driverId = driver.id,
                    name = driver.name,
                    phoneNumber = driver.phoneNumber ?: "",
                    emergencyContactName = driver.emergencyContactName ?: "",
                    emergencyContactPhone = driver.emergencyContactPhone ?: "",
                    memberSinceText = formatTimestamp(driver.createdAt),
                    isEditing = editing,
                    isSaving = saving,
                    saveError = error
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DriverProfileUiState()
        )

    init {
        viewModelScope.launch {
            profileInitializer.seedDefaultProfilesIfNeeded()
        }
    }

    fun onEditToggle() {
        saveErrorFlow.value = null
        isEditingFlow.value = !isEditingFlow.value
    }

    fun save(
        name: String,
        phoneNumber: String,
        emergencyContactName: String,
        emergencyContactPhone: String
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            saveErrorFlow.value = "Driver name cannot be empty"
            return
        }

        val trimmedPhone = phoneNumber.trim()
        if (trimmedPhone.isNotEmpty() && trimmedPhone.count { it.isDigit() } < 7) {
            saveErrorFlow.value = "Driver phone number must contain at least 7 digits"
            return
        }

        val trimmedEmergencyPhone = emergencyContactPhone.trim()
        if (trimmedEmergencyPhone.isNotEmpty() && trimmedEmergencyPhone.count { it.isDigit() } < 7) {
            saveErrorFlow.value = "Emergency contact phone must contain at least 7 digits"
            return
        }

        val currentDriverId = uiState.value.driverId ?: "driver_1"
        viewModelScope.launch {
            isSavingFlow.value = true
            saveErrorFlow.value = null

            val existing = driverRepository.getDriver(currentDriverId)
            val now = System.currentTimeMillis()
            val updated = DriverProfile(
                id = currentDriverId,
                name = trimmedName,
                phoneNumber = trimmedPhone.ifBlank { null },
                emergencyContactName = emergencyContactName.trim().ifBlank { null },
                emergencyContactPhone = trimmedEmergencyPhone.ifBlank { null },
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )

            driverRepository.saveDriver(updated)
            isSavingFlow.value = false
            isEditingFlow.value = false
        }
    }

    companion object {
        fun formatTimestamp(timestamp: Long): String {
            if (timestamp <= 0L) return "—"
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
            return sdf.format(Date(timestamp))
        }
    }
}
