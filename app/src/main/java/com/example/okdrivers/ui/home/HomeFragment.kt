package com.example.okdrivers.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.example.okdrivers.R
import com.example.okdrivers.data.repository.SensorRepository
import com.example.okdrivers.domain.engine.AdaptiveBaselineEngine
import com.example.okdrivers.domain.engine.DriverStateEngine
import com.example.okdrivers.domain.model.SensorSample
import com.example.okdrivers.sensors.GpsLocationManager
import com.example.okdrivers.sensors.NetworkStatusManager
import com.example.okdrivers.sensors.VehicleTelemetrySample
import com.example.okdrivers.sensors.VehicleTelemetrySimulator

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var ivProfile: ImageView

    private lateinit var cardSystemStatus: LinearLayout
    private lateinit var cardDriverState: LinearLayout
    private lateinit var cardVehicleHealth: LinearLayout
    private lateinit var cardLocation: LinearLayout
    private lateinit var cardNetwork: LinearLayout

    private lateinit var tvDriverState: TextView
    private lateinit var tvVehicleHealth: TextView
    private lateinit var tvLatitude: TextView
    private lateinit var tvLongitude: TextView
    private lateinit var tvNetwork: TextView

    @Inject
    lateinit var gpsLocationManager: GpsLocationManager

    @Inject
    lateinit var networkStatusManager: NetworkStatusManager

    @Inject
    lateinit var vehicleTelemetrySimulator: VehicleTelemetrySimulator

    @Inject
    lateinit var sensorRepository: SensorRepository

    @Inject
    lateinit var driverStateEngine: DriverStateEngine

    @Inject
    lateinit var adaptiveBaselineEngine: AdaptiveBaselineEngine

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val fineLocation =
                permissions[
                    Manifest.permission.ACCESS_FINE_LOCATION
                ] == true

            val coarseLocation =
                permissions[
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ] == true

            if (fineLocation || coarseLocation) {
                startGpsUpdates()
            } else {
                tvLatitude.text = "Permission denied"
                tvLongitude.text = "Enable location"
            }
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(
            view,
            savedInstanceState
        )

        ivProfile =
            view.findViewById(R.id.ivProfile)

        cardSystemStatus =
            view.findViewById(R.id.cardSystemStatus)

        cardDriverState =
            view.findViewById(R.id.cardDriverState)

        cardVehicleHealth =
            view.findViewById(R.id.cardVehicleHealth)

        cardLocation =
            view.findViewById(R.id.cardLocation)

        cardNetwork =
            view.findViewById(R.id.cardNetwork)

        tvDriverState =
            view.findViewById(R.id.tvDriverState)

        tvVehicleHealth =
            view.findViewById(R.id.tvVehicleHealth)

        tvLatitude =
            view.findViewById(R.id.tvLatitude)

        tvLongitude =
            view.findViewById(R.id.tvLongitude)

        tvNetwork =
            view.findViewById(R.id.tvNetwork)

        tvDriverState.text = "Alertness: Good"
        tvVehicleHealth.text = "No issues"

        setupClickListeners()
        updateNetworkStatus()
        checkLocationPermission()

//        startTelemetryTest()
//        startDriverStateTest()
        startAdaptiveBaselineTest()
    }

    private fun setupClickListeners() {

        ivProfile.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Profile clicked",
                Toast.LENGTH_SHORT
            ).show()
        }

        cardSystemStatus.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "All systems are being monitored",
                Toast.LENGTH_SHORT
            ).show()
        }

        cardDriverState.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Driver State clicked",
                Toast.LENGTH_SHORT
            ).show()
        }

        cardVehicleHealth.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Vehicle Health clicked",
                Toast.LENGTH_SHORT
            ).show()
        }

        cardLocation.setOnClickListener {
            checkLocationPermission()
        }

        cardNetwork.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Network status is monitored automatically",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

//    private fun startTelemetryTest() {
//
//        viewLifecycleOwner.lifecycleScope.launch {
//
//            viewLifecycleOwner.repeatOnLifecycle(
//                Lifecycle.State.STARTED
//            ) {
//
//                vehicleTelemetrySimulator
//                    .observeTelemetry()
//                    .collect { telemetry ->
//
//                        Log.d(
//                            "OKDRIVER_TELEMETRY",
//                            """
//                            Speed: ${telemetry.speedKmh} km/h
//                            RPM: ${telemetry.rpm}
//                            Load: ${telemetry.engineLoad}%
//                            Throttle: ${telemetry.throttlePosition}%
//                            Temp: ${telemetry.engineTemperatureCelsius}°C
//                            Voltage: ${telemetry.batteryVoltage}V
//                            Fault: ${telemetry.diagnosticFault}
//                            """.trimIndent()
//                        )
//                    }
//            }
//        }
//    }

//    private fun startDriverStateTest() {
//
//        viewLifecycleOwner.lifecycleScope.launch {
//
//            viewLifecycleOwner.repeatOnLifecycle(
//                Lifecycle.State.STARTED
//            ) {
//
//                sensorRepository
//                    .observeDms()
//                    .collect { dmsSample ->
//
//                        val driverState =
//                            driverStateEngine.process(
//                                dmsSample
//                            )
//
//                        Log.d(
//                            "OKDRIVER_DRIVER_STATE",
//                            """
//                            Timestamp: ${driverState.timestamp}
//                            PERCLOS: ${driverState.perclos}
//                            Gaze: ${driverState.gazeDirection}
//                            Head Pitch: ${driverState.headPitch}°
//                            Head Yaw: ${driverState.headYaw}°
//                            Head Roll: ${driverState.headRoll}°
//                            Blink Rate: ${driverState.blinkRate}
//                            Yawn: ${driverState.yawnDetected}
//                            Gaze Away: ${driverState.gazeAwayDurationMs} ms
//                            Attention: ${driverState.attentionScore}
//                            Responsive: ${driverState.isResponsive}
//                            Condition: ${driverState.condition}
//                            """.trimIndent()
//                        )
//                    }
//            }
//        }
//    }

    private fun startAdaptiveBaselineTest() {

        val initialSample =
            SensorSample(
                timestamp = System.currentTimeMillis(),
                accelerationX = 0f,
                accelerationY = 0f,
                accelerationZ = 9.81f,
                gForce = 0.50f,
                pitch = 0f,
                roll = 0f,
                yaw = 0f,
                latitude = 0.0,
                longitude = 0.0,
                speedKmh = 40f,
                heading = 0f,
                batteryPercentage = 80,
                isCharging = false,
                isNetworkOnline = true
            )

        val firstResult =
            adaptiveBaselineEngine.updateDriverBaseline(
                currentSample = initialSample,
                previousBaseline = null
            )

        Log.d(
            "OKDRIVER_BASELINE",
            """
            INITIAL BASELINE
            Updated: ${firstResult.updated}
            Average Speed: ${firstResult.baseline.averageSpeedKmh}
            Average G-Force: ${firstResult.baseline.averageGForce}
            Sample Count: ${firstResult.baseline.sampleCount}
            Updated At: ${firstResult.baseline.updatedAt}
            """.trimIndent()
        )

        val normalSample =
            initialSample.copy(
                timestamp = System.currentTimeMillis(),
                gForce = 0.70f,
                speedKmh = 50f
            )

        val secondResult =
            adaptiveBaselineEngine.updateDriverBaseline(
                currentSample = normalSample,
                previousBaseline = firstResult.baseline
            )

        Log.d(
            "OKDRIVER_BASELINE",
            """
            EMA UPDATE
            Updated: ${secondResult.updated}
            Average Speed: ${secondResult.baseline.averageSpeedKmh}
            Average G-Force: ${secondResult.baseline.averageGForce}
            Sample Count: ${secondResult.baseline.sampleCount}
            Updated At: ${secondResult.baseline.updatedAt}
            """.trimIndent()
        )

        val anomalySample =
            initialSample.copy(
                timestamp = System.currentTimeMillis(),
                gForce = 2.0f,
                speedKmh = 0f
            )

        val anomalyResult =
            adaptiveBaselineEngine.updateDriverBaseline(
                currentSample = anomalySample,
                previousBaseline = secondResult.baseline,
                allowUpdate = false
            )

        Log.d(
            "OKDRIVER_BASELINE",
            """
            ANOMALY TEST
            Updated: ${anomalyResult.updated}
            Average Speed: ${anomalyResult.baseline.averageSpeedKmh}
            Average G-Force: ${anomalyResult.baseline.averageGForce}
            Sample Count: ${anomalyResult.baseline.sampleCount}
            Updated At: ${anomalyResult.baseline.updatedAt}
            """.trimIndent()
        )

        startVehicleBaselineTest()
    }

    private fun startVehicleBaselineTest() {

        val initialTelemetry =
            VehicleTelemetrySample(
                timestamp = System.currentTimeMillis(),
                speedKmh = 40f,
                rpm = 1800f,
                engineLoad = 35f,
                throttlePosition = 25f,
                engineTemperatureCelsius = 88f,
                batteryVoltage = 13.9f,
                diagnosticFault = null
            )

        val firstResult =
            adaptiveBaselineEngine.updateVehicleBaseline(
                currentSample = initialTelemetry,
                previousBaseline = null
            )

        Log.d(
            "OKDRIVER_VEHICLE_BASELINE",
            """
            INITIAL BASELINE
            Updated: ${firstResult.updated}
            Average Speed: ${firstResult.baseline.averageSpeedKmh}
            Average RPM: ${firstResult.baseline.averageRpm}
            Average Load: ${firstResult.baseline.averageEngineLoad}
            Sample Count: ${firstResult.baseline.sampleCount}
            Updated At: ${firstResult.baseline.updatedAt}
            """.trimIndent()
        )

        val normalTelemetry =
            initialTelemetry.copy(
                timestamp = System.currentTimeMillis(),
                speedKmh = 50f,
                rpm = 2000f,
                engineLoad = 40f
            )

        val secondResult =
            adaptiveBaselineEngine.updateVehicleBaseline(
                currentSample = normalTelemetry,
                previousBaseline = firstResult.baseline
            )

        Log.d(
            "OKDRIVER_VEHICLE_BASELINE",
            """
            EMA UPDATE
            Updated: ${secondResult.updated}
            Average Speed: ${secondResult.baseline.averageSpeedKmh}
            Average RPM: ${secondResult.baseline.averageRpm}
            Average Load: ${secondResult.baseline.averageEngineLoad}
            Sample Count: ${secondResult.baseline.sampleCount}
            Updated At: ${secondResult.baseline.updatedAt}
            """.trimIndent()
        )

        val anomalyTelemetry =
            initialTelemetry.copy(
                timestamp = System.currentTimeMillis(),
                speedKmh = 0f,
                rpm = 0f,
                engineLoad = 0f,
                diagnosticFault = "ENGINE_FAULT"
            )

        val anomalyResult =
            adaptiveBaselineEngine.updateVehicleBaseline(
                currentSample = anomalyTelemetry,
                previousBaseline = secondResult.baseline,
                allowUpdate = false
            )

        Log.d(
            "OKDRIVER_VEHICLE_BASELINE",
            """
            ANOMALY TEST
            Updated: ${anomalyResult.updated}
            Average Speed: ${anomalyResult.baseline.averageSpeedKmh}
            Average RPM: ${anomalyResult.baseline.averageRpm}
            Average Load: ${anomalyResult.baseline.averageEngineLoad}
            Sample Count: ${anomalyResult.baseline.sampleCount}
            Updated At: ${anomalyResult.baseline.updatedAt}
            """.trimIndent()
        )
    }

    private fun updateNetworkStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                networkStatusManager.observeNetwork()
                    .collect { status ->
                        if (status.isOnline) {
                            tvNetwork.text = "Online"
                        } else {
                            tvNetwork.text = "Offline"
                        }
                    }
            }
        }
    }

    private fun checkLocationPermission() {

        val fineLocationGranted =
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted =
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (
            fineLocationGranted ||
            coarseLocationGranted
        ) {
            startGpsUpdates()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun startGpsUpdates() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                gpsLocationManager
                    .observeLocation()
                    .collect { location ->

                        tvLatitude.text =
                            String.format(
                                "Lat %.4f°",
                                location.latitude
                            )

                        tvLongitude.text =
                            String.format(
                                "Long %.4f°",
                                location.longitude
                            )
                    }
            }
        }
    }
}