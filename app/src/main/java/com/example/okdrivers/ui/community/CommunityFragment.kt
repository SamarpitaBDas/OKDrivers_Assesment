package com.example.okdrivers.ui.community

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
class CommunityFragment : Fragment() {

    private val viewModel: CommunityViewModel by viewModels()
    private lateinit var responderAdapter: ResponderAdapter

    private var tvCommunityActive: TextView? = null
    private var tvEmptyResponders: TextView? = null
    private var rvResponders: RecyclerView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_community, container, false)
        tvCommunityActive = view.findViewById(R.id.tvCommunityActive)
        tvEmptyResponders = view.findViewById(R.id.tvEmptyResponders)
        rvResponders = view.findViewById(R.id.rvResponders)

        responderAdapter = ResponderAdapter()
        rvResponders?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = responderAdapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    when (state) {
                        is CommunityUiState.Idle -> {
                            tvEmptyResponders?.text = state.message
                            tvEmptyResponders?.visibility = View.VISIBLE
                            rvResponders?.visibility = View.GONE
                            tvCommunityActive?.text = "Active safety network monitoring"
                        }
                        is CommunityUiState.Success -> {
                            if (state.responders.isEmpty()) {
                                tvEmptyResponders?.text = "No active responders nearby"
                                tvEmptyResponders?.visibility = View.VISIBLE
                                rvResponders?.visibility = View.GONE
                            } else {
                                tvEmptyResponders?.visibility = View.GONE
                                rvResponders?.visibility = View.VISIBLE
                                responderAdapter.submitList(state.responders)
                            }
                            tvCommunityActive?.text = "${state.activeCount} responders nearby"
                        }
                    }
                }
            }
        }

        return view
    }
}
