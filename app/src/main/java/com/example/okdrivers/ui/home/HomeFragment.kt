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
import com.example.okdrivers.sensors.GpsLocationManager
import com.example.okdrivers.sensors.NetworkStatusManager
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

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val fineLocation =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

            val coarseLocation =
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

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
        super.onViewCreated(view, savedInstanceState)

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

        // Temporary telemetry verification
        startTelemetryTest()

        // NetworkCallback verification was already completed.
        // Keep this commented for now.
        // startNetworkUpdates()
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

    /**
     * Temporary telemetry test.
     *
     * This collects simulated vehicle telemetry and
     * prints the values to Logcat every second.
     */
    private fun startTelemetryTest() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                vehicleTelemetrySimulator
                    .observeTelemetry()
                    .collect { telemetry ->

                        Log.d(
                            "OKDRIVER_TELEMETRY",
                            """
                            Speed: ${telemetry.speedKmh} km/h
                            RPM: ${telemetry.rpm}
                            Load: ${telemetry.engineLoad}%
                            Throttle: ${telemetry.throttlePosition}%
                            Temp: ${telemetry.engineTemperatureCelsius}°C
                            Voltage: ${telemetry.batteryVoltage}V
                            Fault: ${telemetry.diagnosticFault}
                            """.trimIndent()
                        )
                    }
            }
        }
    }

    /**
     * NetworkCallback based monitoring.
     *
     * Currently kept here for reference/testing.
     * The simulator test does not depend on it.
     */
    private fun startNetworkUpdates() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                networkStatusManager
                    .observeNetwork()
                    .collect { network ->

                        tvNetwork.text =
                            if (network.isOnline) {
                                "Online"
                            } else {
                                "Offline"
                            }

                        Log.d(
                            "OKDRIVER_NETWORK",
                            "Network online: ${network.isOnline}"
                        )
                    }
            }
        }
    }

    /**
     * Initial network status check.
     */
    private fun updateNetworkStatus() {

        val connectivityManager =
            requireContext().getSystemService(
                Context.CONNECTIVITY_SERVICE
            ) as ConnectivityManager

        val network =
            connectivityManager.activeNetwork

        val capabilities =
            connectivityManager.getNetworkCapabilities(
                network
            )

        val connected =
            capabilities?.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            ) == true

        if (connected) {
            tvNetwork.text = "Online"
        } else {
            tvNetwork.text = "Offline"
        }
    }

    /**
     * Checks whether location permission has been granted.
     */
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

        if (fineLocationGranted || coarseLocationGranted) {

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

    /**
     * Collects GPS location updates.
     */
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