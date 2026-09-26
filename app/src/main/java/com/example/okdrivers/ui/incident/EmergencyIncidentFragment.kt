package com.example.okdrivers.ui.incident

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.okdrivers.R
import com.example.okdrivers.domain.model.AnomalySeverity
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EmergencyIncidentFragment : Fragment(R.layout.fragment_emergency_incident) {

    private val viewModel: EmergencyIncidentViewModel by viewModels()

    private var tvEmergencyState: TextView? = null
    private var tvSeverityBadge: TextView? = null
    private var tvContextualStatusLine: TextView? = null
    private var pbConfidence: LinearProgressIndicator? = null
    private var tvConfidencePercentage: TextView? = null
    private var tvTriggerReason: TextView? = null
    private var tvIncidentId: TextView? = null
    private var tvCreatedTime: TextView? = null
    private var tvLocation: TextView? = null
    private var btnViewAiExchange: MaterialButton? = null
    private var btnCancelEmergency: MaterialButton? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvEmergencyState = view.findViewById(R.id.tvEmergencyState)
        tvSeverityBadge = view.findViewById(R.id.tvSeverityBadge)
        tvContextualStatusLine = view.findViewById(R.id.tvContextualStatusLine)
        pbConfidence = view.findViewById(R.id.pbConfidence)
        tvConfidencePercentage = view.findViewById(R.id.tvConfidencePercentage)
        tvTriggerReason = view.findViewById(R.id.tvTriggerReason)
        tvIncidentId = view.findViewById(R.id.tvIncidentId)
        tvCreatedTime = view.findViewById(R.id.tvCreatedTime)
        tvLocation = view.findViewById(R.id.tvLocation)
        btnViewAiExchange = view.findViewById(R.id.btnViewAiExchange)
        btnCancelEmergency = view.findViewById(R.id.btnCancelEmergency)

        btnViewAiExchange?.setOnClickListener {
            findNavController().navigate(R.id.aiVerificationFragment)
        }

        btnCancelEmergency?.setOnClickListener {
            viewModel.selfResolve()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    renderUiState(state)
                }
            }
        }
    }

    private fun renderUiState(state: EmergencyIncidentUiState) {
        tvEmergencyState?.text = state.emergencyState?.name ?: "NORMAL OPERATION"
        tvContextualStatusLine?.text = state.contextualStatusLine

        val severity = state.severity
        tvSeverityBadge?.text = severity?.name ?: "NORMAL"
        val severityColor = when (severity) {
            AnomalySeverity.CRITICAL -> ContextCompat.getColor(requireContext(), R.color.ok_red)
            AnomalySeverity.HIGH -> ContextCompat.getColor(requireContext(), R.color.ok_orange)
            AnomalySeverity.MEDIUM -> ContextCompat.getColor(requireContext(), R.color.ok_blue)
            AnomalySeverity.LOW -> ContextCompat.getColor(requireContext(), R.color.ok_green)
            null -> ContextCompat.getColor(requireContext(), R.color.ok_secondary)
        }
        tvSeverityBadge?.setTextColor(severityColor)

        val progress = ((state.confidence ?: 0f) * 100f).toInt().coerceIn(0, 100)
        pbConfidence?.progress = progress
        tvConfidencePercentage?.text = state.formattedConfidence

        tvTriggerReason?.text = state.triggerReason ?: "None — monitoring active"
        tvIncidentId?.text = "Incident ID: ${state.incidentId ?: "—"}"
        tvCreatedTime?.text = "Created: ${state.formattedCreatedTime}"
        tvLocation?.text = "Location: ${state.formattedLocation}"

        btnCancelEmergency?.isEnabled = state.canSelfResolve
    }
}
