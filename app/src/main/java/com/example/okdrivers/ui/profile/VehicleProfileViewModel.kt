package com.example.okdrivers.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.okdrivers.data.repository.CurrentProfileRepository
import com.example.okdrivers.data.repository.DriverRepository
import com.example.okdrivers.data.repository.ProfileInitializer
import com.example.okdrivers.data.repository.VehicleRepository
import com.example.okdrivers.domain.model.VehicleProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class VehicleProfileViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val driverRepository: DriverRepository,
    private val currentProfileRepository: CurrentProfileRepository,
    private val profileInitializer: ProfileInitializer
) : ViewModel() {

    private val isEditingFlow = MutableStateFlow(false)
    private val isSavingFlow = MutableStateFlow(false)
    private val saveErrorFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<VehicleProfileUiState> = currentProfileRepository.observeCurrentVehicleId()
        .flatMapLatest { vehicleId ->
            vehicleRepository.observeVehicle(vehicleId)
        }.combine(
            combine(isEditingFlow, isSavingFlow, saveErrorFlow) { editing, saving, error ->
                Triple(editing, saving, error)
            }
        ) { vehicle, (editing, saving, error) ->
            if (vehicle == null) {
                VehicleProfileUiState(isLoading = true, saveError = error)
            } else {
                val linkedDriverName = vehicle.driverId?.let { driverRepository.getDriver(it)?.name } ?: "Demo Driver"
                VehicleProfileUiState(
                    isLoading = false,
                    vehicleId = vehicle.id,
                    registrationNumber = vehicle.registrationNumber,
                    make = vehicle.make,
                    model = vehicle.model,
                    year = vehicle.year?.toString() ?: "",
                    linkedDriverName = linkedDriverName,
                    isEditing = editing,
                    isSaving = saving,
                    saveError = error
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = VehicleProfileUiState()
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
        registrationNumber: String,
        make: String,
        model: String,
        yearText: String
    ) {
        val trimmedReg = registrationNumber.trim()
        if (trimmedReg.isBlank()) {
            saveErrorFlow.value = "Registration number cannot be empty"
            return
        }

        val trimmedYear = yearText.trim()
        var parsedYear: Int? = null
        if (trimmedYear.isNotEmpty()) {
            val yearInt = trimmedYear.toIntOrNull()
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            if (yearInt == null || yearInt !in 1980..(currentYear + 1)) {
                saveErrorFlow.value = "Vehicle year must be a valid year (1980–${currentYear + 1})"
                return
            }
            parsedYear = yearInt
        }

        val currentVehicleId = uiState.value.vehicleId ?: "vehicle_1"
        viewModelScope.launch {
            isSavingFlow.value = true
            saveErrorFlow.value = null

            val existing = vehicleRepository.getVehicle(currentVehicleId)
            val now = System.currentTimeMillis()
            val updated = VehicleProfile(
                id = currentVehicleId,
                registrationNumber = trimmedReg,
                make = make.trim().ifBlank { "Unknown" },
                model = model.trim().ifBlank { "Unknown" },
                year = parsedYear,
                driverId = existing?.driverId ?: "driver_1",
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )

            vehicleRepository.saveVehicle(updated)
            isSavingFlow.value = false
            isEditingFlow.value = false
        }
    }
}
