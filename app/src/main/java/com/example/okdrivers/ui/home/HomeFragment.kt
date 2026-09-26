package com.example.okdrivers.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.okdrivers.R
import com.example.okdrivers.sensors.GpsLocationManager
import com.example.okdrivers.service.SafetyMonitoringService
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.LinearProgressIndicator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    private val viewModel: HomeViewModel by viewModels()

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

    private var switchSafetyService: MaterialSwitch? = null
    private var tvServiceStatus: TextView? = null
    private var tvAnomalyConfidenceBadge: TextView? = null
    private var pbAnomalyConfidence: LinearProgressIndicator? = null

    @Inject
    lateinit var gpsLocationManager: GpsLocationManager

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (fineLocation || coarseLocation) {
                startGpsUpdates()
            } else {
                tvLatitude.text = "Permission denied"
                tvLongitude.text = "Enable location"
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ivProfile = view.findViewById(R.id.ivProfile)
        cardSystemStatus = view.findViewById(R.id.cardSystemStatus)
        cardDriverState = view.findViewById(R.id.cardDriverState)
        cardVehicleHealth = view.findViewById(R.id.cardVehicleHealth)
        cardLocation = view.findViewById(R.id.cardLocation)
        cardNetwork = view.findViewById(R.id.cardNetwork)

        tvDriverState = view.findViewById(R.id.tvDriverState)
        tvVehicleHealth = view.findViewById(R.id.tvVehicleHealth)
        tvLatitude = view.findViewById(R.id.tvLatitude)
        tvLongitude = view.findViewById(R.id.tvLongitude)
        tvNetwork = view.findViewById(R.id.tvNetwork)

        switchSafetyService = view.findViewById(R.id.switchSafetyService)
        tvServiceStatus = view.findViewById(R.id.tvServiceStatus)
        tvAnomalyConfidenceBadge = view.findViewById(R.id.tvAnomalyConfidenceBadge)
        pbAnomalyConfidence = view.findViewById(R.id.pbAnomalyConfidence)

        setupClickListeners()
        checkLocationPermission()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    renderUiState(state)
                }
            }
        }
    }

    private fun renderUiState(state: HomeUiState) {
        tvNetwork.text = if (state.isOnline) "Online" else "Offline"
        tvDriverState.text = "State: ${state.emergencyState.name}"
        tvVehicleHealth.text = "Battery: ${state.batteryPercentage}%${if (state.isCharging) " (Charging)" else ""}"

        // Service Toggle handling with listener re-entrance guard
        if (switchSafetyService?.isChecked != state.serviceRunning) {
            switchSafetyService?.setOnCheckedChangeListener(null)
            switchSafetyService?.isChecked = state.serviceRunning
            attachSwitchListener()
        }
        tvServiceStatus?.text = if (state.serviceRunning) "Service Active in Background" else "Service Paused"

        // Anomaly Confidence rendering
        val confPercent = (state.anomalyConfidence * 100f).toInt().coerceIn(0, 100)
        pbAnomalyConfidence?.progress = confPercent
        tvAnomalyConfidenceBadge?.text = "$confPercent%"

        val badgeColor = when {
            confPercent >= 70 -> ContextCompat.getColor(requireContext(), R.color.ok_red)
            confPercent >= 30 -> ContextCompat.getColor(requireContext(), R.color.ok_orange)
            else -> ContextCompat.getColor(requireContext(), R.color.ok_green)
        }
        tvAnomalyConfidenceBadge?.setTextColor(badgeColor)
    }

    private fun attachSwitchListener() {
        switchSafetyService?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                SafetyMonitoringService.startService(requireContext())
            } else {
                SafetyMonitoringService.stopService(requireContext())
            }
            viewModel.setServiceRunning(isChecked)
        }
    }

    private fun setupClickListeners() {
        attachSwitchListener()

        ivProfile.setOnClickListener {
            findNavController().navigate(R.id.driverProfileFragment)
        }

        cardSystemStatus.setOnClickListener {
            findNavController().navigate(R.id.emergencyIncidentFragment)
        }

        cardDriverState.setOnClickListener {
            Toast.makeText(requireContext(), "Driver State clicked", Toast.LENGTH_SHORT).show()
        }

        cardVehicleHealth.setOnClickListener {
            findNavController().navigate(R.id.vehicleProfileFragment)
        }

        cardLocation.setOnClickListener {
            checkLocationPermission()
        }

        cardNetwork.setOnClickListener {
            Toast.makeText(requireContext(), "Network status is monitored automatically", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkLocationPermission() {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
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

    private fun startGpsUpdates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                gpsLocationManager.observeLocation().collect { location ->
                    tvLatitude.text = String.format(Locale.US, "Lat %.4f°", location.latitude)
                    tvLongitude.text = String.format(Locale.US, "Long %.4f°", location.longitude)
                }
            }
        }
    }
}
