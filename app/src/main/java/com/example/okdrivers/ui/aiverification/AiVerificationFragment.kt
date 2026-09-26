package com.example.okdrivers.ui.aiverification

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.okdrivers.R
import com.example.okdrivers.domain.model.ResponseClassification
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AiVerificationFragment : Fragment(R.layout.fragment_ai_verification) {

    private val viewModel: AiVerificationViewModel by viewModels()

    private var tvOverallStatus: TextView? = null
    private var tvUrgencyBadge: TextView? = null
    private var tvAttemptBadge: TextView? = null
    private var tvListeningStatus: TextView? = null
    private var tvActivePromptText: TextView? = null
    private var tvLiveTranscript: TextView? = null
    private var tvClassificationBadge: TextView? = null
    private var tvLatencyBadge: TextView? = null
    private var rvAttemptHistory: RecyclerView? = null
    private var swForceSimulatedMode: MaterialSwitch? = null
    private var btnSimulateResponsive: MaterialButton? = null
    private var btnSimulateImpaired: MaterialButton? = null
    private var btnSimulateUnresponsive: MaterialButton? = null

    private lateinit var adapter: AttemptSessionAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvOverallStatus = view.findViewById(R.id.tvOverallStatus)
        tvUrgencyBadge = view.findViewById(R.id.tvUrgencyBadge)
        tvAttemptBadge = view.findViewById(R.id.tvAttemptBadge)
        tvListeningStatus = view.findViewById(R.id.tvListeningStatus)
        tvActivePromptText = view.findViewById(R.id.tvActivePromptText)
        tvLiveTranscript = view.findViewById(R.id.tvLiveTranscript)
        tvClassificationBadge = view.findViewById(R.id.tvClassificationBadge)
        tvLatencyBadge = view.findViewById(R.id.tvLatencyBadge)
        rvAttemptHistory = view.findViewById(R.id.rvAttemptHistory)
        swForceSimulatedMode = view.findViewById(R.id.swForceSimulatedMode)
        btnSimulateResponsive = view.findViewById(R.id.btnSimulateResponsive)
        btnSimulateImpaired = view.findViewById(R.id.btnSimulateImpaired)
        btnSimulateUnresponsive = view.findViewById(R.id.btnSimulateUnresponsive)

        adapter = AttemptSessionAdapter()
        rvAttemptHistory?.layoutManager = LinearLayoutManager(requireContext())
        rvAttemptHistory?.adapter = adapter

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
        swForceSimulatedMode?.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleForceSimulatedMode(isChecked)
            Toast.makeText(
                requireContext(),
                "Simulated STT mode ${if (isChecked) "Enabled" else "Disabled"}",
                Toast.LENGTH_SHORT
            ).show()
        }

        btnSimulateResponsive?.setOnClickListener {
            viewModel.setSimulatedResponse("I'm okay")
            Toast.makeText(requireContext(), "Next response set to: \"I'm okay\"", Toast.LENGTH_SHORT).show()
        }

        btnSimulateImpaired?.setOnClickListener {
            viewModel.setSimulatedResponse("I need help")
            Toast.makeText(requireContext(), "Next response set to: \"I need help\"", Toast.LENGTH_SHORT).show()
        }

        btnSimulateUnresponsive?.setOnClickListener {
            viewModel.setSimulatedResponse(null)
            Toast.makeText(requireContext(), "Next response set to: Silence / Unresponsive", Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderUiState(state: AiVerificationUiState) {
        tvOverallStatus?.text = state.verificationStatusText
        tvUrgencyBadge?.text = state.currentUrgencyLevel?.name ?: "INITIAL"
        tvAttemptBadge?.text = "Attempt ${state.currentAttemptCount} of 3"
        tvListeningStatus?.visibility = if (state.isSessionInProgress) View.VISIBLE else View.GONE
        tvActivePromptText?.text = state.activePromptText ?: "No active prompt"

        val transcript = state.latestTranscript
        tvLiveTranscript?.text = if (transcript.isNullOrBlank()) {
            if (state.isSessionInProgress) "Listening for speech..." else "Awaiting speech response..."
        } else {
            "\"$transcript\""
        }

        val classification = state.latestClassification
        tvClassificationBadge?.text = classification?.name ?: "PENDING"
        val classificationColor = when (classification) {
            ResponseClassification.RESPONSIVE -> ContextCompat.getColor(requireContext(), R.color.ok_green)
            ResponseClassification.IMPAIRED -> ContextCompat.getColor(requireContext(), R.color.ok_orange)
            ResponseClassification.UNRESPONSIVE -> ContextCompat.getColor(requireContext(), R.color.ok_red)
            null -> ContextCompat.getColor(requireContext(), R.color.ok_secondary)
        }
        tvClassificationBadge?.setTextColor(classificationColor)

        tvLatencyBadge?.text = "Latency: ${state.latestLatencyMs?.let { "$it ms" } ?: "—"}"

        if (swForceSimulatedMode?.isChecked != state.isForceSimulatedMode) {
            swForceSimulatedMode?.setOnCheckedChangeListener(null)
            swForceSimulatedMode?.isChecked = state.isForceSimulatedMode
            swForceSimulatedMode?.setOnCheckedChangeListener { _, isChecked ->
                viewModel.toggleForceSimulatedMode(isChecked)
            }
        }

        adapter.updateSessions(state.sessionHistory)
    }
}
