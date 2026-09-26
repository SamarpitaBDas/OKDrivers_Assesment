package com.example.okdrivers.ui.testcontrols

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.okdrivers.R
import com.example.okdrivers.domain.model.EmergencyState
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TestControlsFragment : Fragment(R.layout.fragment_test_controls) {

    private val viewModel: TestControlsViewModel by viewModels()

    private var btnBack: MaterialButton? = null
    private var tvActiveScenarioName: TextView? = null
    private var tvEmergencyStateBadge: TextView? = null
    private var tvActiveIncidentId: TextView? = null
    private var tvStatusMessage: TextView? = null

    private var btnNormalDriving: MaterialButton? = null
    private var btnHardBraking: MaterialButton? = null
    private var btnSuspectedAccident: MaterialButton? = null
    private var btnDriverResponds: MaterialButton? = null
    private var btnDriverSilent: MaterialButton? = null
    private var btnResponderAccepts: MaterialButton? = null
    private var btnNoCommunityResponse: MaterialButton? = null
    private var btnAirbagEvent: MaterialButton? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnBack = view.findViewById(R.id.btnBack)
        tvActiveScenarioName = view.findViewById(R.id.tvActiveScenarioName)
        tvEmergencyStateBadge = view.findViewById(R.id.tvEmergencyStateBadge)
        tvActiveIncidentId = view.findViewById(R.id.tvActiveIncidentId)
        tvStatusMessage = view.findViewById(R.id.tvStatusMessage)

        btnNormalDriving = view.findViewById(R.id.btnNormalDriving)
        btnHardBraking = view.findViewById(R.id.btnHardBraking)
        btnSuspectedAccident = view.findViewById(R.id.btnSuspectedAccident)
        btnDriverResponds = view.findViewById(R.id.btnDriverResponds)
        btnDriverSilent = view.findViewById(R.id.btnDriverSilent)
        btnResponderAccepts = view.findViewById(R.id.btnResponderAccepts)
        btnNoCommunityResponse = view.findViewById(R.id.btnNoCommunityResponse)
        btnAirbagEvent = view.findViewById(R.id.btnAirbagEvent)

        setupClickListeners()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    renderUiState(state)
                }
            }
        }
    }

    private fun setupClickListeners() {
        btnBack?.setOnClickListener {
            findNavController().navigateUp()
        }

        btnNormalDriving?.setOnClickListener {
            viewModel.triggerNormalDriving()
            Toast.makeText(requireContext(), "Triggered: 1. Normal Driving", Toast.LENGTH_SHORT).show()
        }

        btnHardBraking?.setOnClickListener {
            viewModel.triggerHardBraking()
            Toast.makeText(requireContext(), "Triggered: 2. 0.8G Hard Brake", Toast.LENGTH_SHORT).show()
        }

        btnSuspectedAccident?.setOnClickListener {
            viewModel.triggerSuspectedAccident()
            Toast.makeText(requireContext(), "Triggered: 3. Suspected Accident", Toast.LENGTH_SHORT).show()
        }

        btnDriverResponds?.setOnClickListener {
            viewModel.triggerDriverResponds()
            Toast.makeText(requireContext(), "Triggered: 4. Driver Responds (\"I'm okay\")", Toast.LENGTH_SHORT).show()
        }

        btnDriverSilent?.setOnClickListener {
            viewModel.triggerDriverSilent()
            Toast.makeText(requireContext(), "Triggered: 5. Driver Silent (Exhaust 3 Attempts)", Toast.LENGTH_SHORT).show()
        }

        btnResponderAccepts?.setOnClickListener {
            viewModel.triggerResponderAccepts()
            Toast.makeText(requireContext(), "Triggered: 6. Responder Accepts", Toast.LENGTH_SHORT).show()
        }

        btnNoCommunityResponse?.setOnClickListener {
            viewModel.triggerNoCommunityResponse()
            Toast.makeText(requireContext(), "Triggered: 7. No Community Response (Exhaust 20km)", Toast.LENGTH_SHORT).show()
        }

        btnAirbagEvent?.setOnClickListener {
            viewModel.triggerAirbagEvent()
            Toast.makeText(requireContext(), "Triggered: 8. Critical Airbag Event", Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderUiState(state: TestControlsUiState) {
        tvActiveScenarioName?.text = state.activeScenarioName
        tvEmergencyStateBadge?.text = state.currentEmergencyState.name

        val badgeColor = when (state.currentEmergencyState) {
            EmergencyState.NORMAL_OPERATION -> ContextCompat.getColor(requireContext(), R.color.ok_green)
            EmergencyState.ANOMALY_DETECTION,
            EmergencyState.AI_VERIFICATION -> ContextCompat.getColor(requireContext(), R.color.ok_orange)
            EmergencyState.COMMUNITY_MOBILIZATION,
            EmergencyState.COMMUNITY_RESPONSE -> ContextCompat.getColor(requireContext(), R.color.ok_blue)
            EmergencyState.AUTHORITY_ESCALATION -> ContextCompat.getColor(requireContext(), R.color.ok_red)
            EmergencyState.RESOLVED -> ContextCompat.getColor(requireContext(), R.color.ok_secondary)
        }
        tvEmergencyStateBadge?.setTextColor(badgeColor)

        tvActiveIncidentId?.text = "Active Incident ID: ${state.activeIncidentId ?: "—"}"
        tvStatusMessage?.text = state.statusMessage
    }
}
