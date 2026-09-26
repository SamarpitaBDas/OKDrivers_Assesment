package com.example.okdrivers.ui.profile

import com.example.okdrivers.data.repository.CurrentProfileRepository
import com.example.okdrivers.data.repository.DriverRepository
import com.example.okdrivers.data.repository.ProfileInitializer
import com.example.okdrivers.data.repository.VehicleRepository
import com.example.okdrivers.domain.model.DriverProfile
import com.example.okdrivers.domain.model.VehicleProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VehicleProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeDriverRepository : DriverRepository {
        val drivers = mutableListOf<DriverProfile>()
        val driversFlow = MutableStateFlow<List<DriverProfile>>(emptyList())

        override fun observeDrivers(): Flow<List<DriverProfile>> = driversFlow
        override fun observeDriver(id: String): Flow<DriverProfile?> = driversFlow.map { list -> list.find { it.id == id } }
        override suspend fun getDriver(id: String): DriverProfile? = drivers.find { it.id == id }
        override suspend fun saveDriver(driver: DriverProfile) {
            val idx = drivers.indexOfFirst { it.id == driver.id }
            if (idx >= 0) {
                drivers[idx] = driver
            } else {
                drivers.add(driver)
            }
            driversFlow.value = drivers.toList()
        }
    }

    private class FakeVehicleRepository : VehicleRepository {
        val vehicles = mutableListOf<VehicleProfile>()
        val vehiclesFlow = MutableStateFlow<List<VehicleProfile>>(emptyList())

        override fun observeVehicles(): Flow<List<VehicleProfile>> = vehiclesFlow
        override fun observeVehicle(id: String): Flow<VehicleProfile?> = vehiclesFlow.map { list -> list.find { it.id == id } }
        override suspend fun getVehicle(id: String): VehicleProfile? = vehicles.find { it.id == id }
        override suspend fun saveVehicle(vehicle: VehicleProfile) {
            val idx = vehicles.indexOfFirst { it.id == vehicle.id }
            if (idx >= 0) {
                vehicles[idx] = vehicle
            } else {
                vehicles.add(vehicle)
            }
            vehiclesFlow.value = vehicles.toList()
        }
    }

    private class FakeCurrentProfileRepository : CurrentProfileRepository {
        val driverIdFlow = MutableStateFlow("driver_1")
        val vehicleIdFlow = MutableStateFlow("vehicle_1")

        override fun observeCurrentDriverId(): Flow<String> = driverIdFlow
        override fun observeCurrentVehicleId(): Flow<String> = vehicleIdFlow
        override suspend fun setCurrentDriverId(driverId: String) { driverIdFlow.value = driverId }
        override suspend fun setCurrentVehicleId(vehicleId: String) { vehicleIdFlow.value = vehicleId }
    }

    @Test
    fun testAutomaticSeedingAndVehicleObservation() = runTest {
        val driverRepo = FakeDriverRepository()
        val vehicleRepo = FakeVehicleRepository()
        val profileInitializer = ProfileInitializer(driverRepo, vehicleRepo)
        val currentProfileRepo = FakeCurrentProfileRepository()

        val viewModel = VehicleProfileViewModel(vehicleRepo, driverRepo, currentProfileRepo, profileInitializer)
        backgroundScope.launch { viewModel.uiState.collect {} }
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("vehicle_1", state.vehicleId)
        assertEquals("KA-01-AB-1234", state.registrationNumber)
        assertEquals("Toyota", state.make)
        assertEquals("Camry", state.model)
        assertEquals("2024", state.year)
        assertEquals("Demo Driver", state.linkedDriverName)
    }

    @Test
    fun testEditToggleAndYearValidation() = runTest {
        val driverRepo = FakeDriverRepository()
        val vehicleRepo = FakeVehicleRepository()
        val profileInitializer = ProfileInitializer(driverRepo, vehicleRepo)
        val currentProfileRepo = FakeCurrentProfileRepository()

        val viewModel = VehicleProfileViewModel(vehicleRepo, driverRepo, currentProfileRepo, profileInitializer)
        backgroundScope.launch { viewModel.uiState.collect {} }
        testScheduler.advanceUntilIdle()

        viewModel.onEditToggle()
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isEditing)

        // Invalid registration number error
        viewModel.save("", "Toyota", "Camry", "2024")
        testScheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.saveError)
        assertEquals("Registration number cannot be empty", viewModel.uiState.value.saveError)

        // Invalid year error
        viewModel.save("KA-01-AB-1234", "Toyota", "Camry", "1800")
        testScheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.saveError)
        assertTrue(viewModel.uiState.value.saveError!!.contains("must be a valid year"))
    }

    @Test
    fun testSuccessfulVehicleSave() = runTest {
        val driverRepo = FakeDriverRepository()
        val vehicleRepo = FakeVehicleRepository()
        val profileInitializer = ProfileInitializer(driverRepo, vehicleRepo)
        val currentProfileRepo = FakeCurrentProfileRepository()

        val viewModel = VehicleProfileViewModel(vehicleRepo, driverRepo, currentProfileRepo, profileInitializer)
        backgroundScope.launch { viewModel.uiState.collect {} }
        testScheduler.advanceUntilIdle()

        viewModel.onEditToggle()
        viewModel.save("KA-05-MH-9999", "Honda", "Civic", "2023")
        testScheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.saveError)
        assertFalse(viewModel.uiState.value.isEditing)

        val updatedVehicle = vehicleRepo.getVehicle("vehicle_1")
        assertNotNull(updatedVehicle)
        assertEquals("KA-05-MH-9999", updatedVehicle?.registrationNumber)
        assertEquals("Honda", updatedVehicle?.make)
        assertEquals("Civic", updatedVehicle?.model)
        assertEquals(2023, updatedVehicle?.year)
    }
}
