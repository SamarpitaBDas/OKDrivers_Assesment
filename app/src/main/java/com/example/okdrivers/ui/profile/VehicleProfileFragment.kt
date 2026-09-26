package com.example.okdrivers.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.okdrivers.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class VehicleProfileFragment : Fragment(R.layout.fragment_vehicle_profile) {

    private val viewModel: VehicleProfileViewModel by viewModels()

    private var etRegistration: TextInputEditText? = null
    private var etMake: TextInputEditText? = null
    private var etModel: TextInputEditText? = null
    private var etYear: TextInputEditText? = null
    private var tvLinkedDriverName: TextView? = null
    private var btnBack: MaterialButton? = null
    private var btnEdit: MaterialButton? = null
    private var btnSave: MaterialButton? = null

    private var isFirstPopulation = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnBack = view.findViewById(R.id.btnBack)
        etRegistration = view.findViewById(R.id.etRegistration)
        etMake = view.findViewById(R.id.etMake)
        etModel = view.findViewById(R.id.etModel)
        etYear = view.findViewById(R.id.etYear)
        tvLinkedDriverName = view.findViewById(R.id.tvLinkedDriverName)
        btnEdit = view.findViewById(R.id.btnEdit)
        btnSave = view.findViewById(R.id.btnSave)

        btnBack?.setOnClickListener {
            findNavController().navigateUp()
        }

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
        btnEdit?.setOnClickListener {
            viewModel.onEditToggle()
        }

        btnSave?.setOnClickListener {
            val reg = etRegistration?.text?.toString().orEmpty()
            val make = etMake?.text?.toString().orEmpty()
            val model = etModel?.text?.toString().orEmpty()
            val year = etYear?.text?.toString().orEmpty()

            viewModel.save(reg, make, model, year)
        }
    }

    private fun renderUiState(state: VehicleProfileUiState) {
        if (!state.isLoading) {
            if (isFirstPopulation || !state.isEditing) {
                etRegistration?.setText(state.registrationNumber)
                etMake?.setText(state.make)
                etModel?.setText(state.model)
                etYear?.setText(state.year)
                isFirstPopulation = false
            }

            tvLinkedDriverName?.text = "Driver: ${state.linkedDriverName ?: "Demo Driver"}"

            val isEditing = state.isEditing
            etRegistration?.isEnabled = isEditing
            etMake?.isEnabled = isEditing
            etModel?.isEnabled = isEditing
            etYear?.isEnabled = isEditing

            btnEdit?.text = if (isEditing) "Cancel" else "Edit"
            btnSave?.visibility = if (isEditing) View.VISIBLE else View.GONE
            btnSave?.isEnabled = !state.isSaving

            state.saveError?.let { errorMsg ->
                Snackbar.make(requireView(), errorMsg, Snackbar.LENGTH_LONG).show()
            }
        }
    }
}
