package com.example.okdrivers.data.repository

import com.example.okdrivers.domain.model.DriverProfile
import com.example.okdrivers.domain.model.VehicleProfile
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileInitializer @Inject constructor(
    private val driverRepository: DriverRepository,
    private val vehicleRepository: VehicleRepository
) {
    suspend fun seedDefaultProfilesIfNeeded() {
        val now = System.currentTimeMillis()

        val drivers = driverRepository.observeDrivers().first()
        if (drivers.isEmpty()) {
            driverRepository.saveDriver(
                DriverProfile(
                    id = "driver_1",
                    name = "Demo Driver",
                    phoneNumber = "+1-555-0199",
                    emergencyContactName = "Alex Johnson",
                    emergencyContactPhone = "+1-555-0122",
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        val vehicles = vehicleRepository.observeVehicles().first()
        if (vehicles.isEmpty()) {
            vehicleRepository.saveVehicle(
                VehicleProfile(
                    id = "vehicle_1",
                    registrationNumber = "KA-01-AB-1234",
                    make = "Toyota",
                    model = "Camry",
                    year = 2024,
                    driverId = "driver_1",
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }
}
