package com.example.okdrivers.ui.logs

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.okdrivers.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LogsFragment : Fragment() {

    private val viewModel: LogsViewModel by viewModels()
    private lateinit var timelineAdapter: TimelineAdapter

    private var tvLogsSubtitle: TextView? = null
    private var rvTimeline: RecyclerView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_logs, container, false)
        tvLogsSubtitle = view.findViewById(R.id.tvLogsSubtitle)
        rvTimeline = view.findViewById(R.id.rvTimeline)

        timelineAdapter = TimelineAdapter()
        rvTimeline?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = timelineAdapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    when (state) {
                        is LogsUiState.Idle -> {
                            tvLogsSubtitle?.text = state.message
                            rvTimeline?.visibility = View.GONE
                        }
                        is LogsUiState.Success -> {
                            tvLogsSubtitle?.text = "Incident timeline (${state.incidentId.take(8)})"
                            rvTimeline?.visibility = View.VISIBLE
                            timelineAdapter.submitList(state.timeline)
                        }
                    }
                }
            }
        }

        return view
    }
}
