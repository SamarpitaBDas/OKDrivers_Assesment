package com.example.okdrivers.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
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
class DriverProfileFragment : Fragment(R.layout.fragment_driver_profile) {

    private val viewModel: DriverProfileViewModel by viewModels()

    private var etName: TextInputEditText? = null
    private var etPhone: TextInputEditText? = null
    private var tvMemberSince: TextView? = null
    private var etEmergencyName: TextInputEditText? = null
    private var etEmergencyPhone: TextInputEditText? = null
    private var btnBack: MaterialButton? = null
    private var btnEdit: MaterialButton? = null
    private var btnSave: MaterialButton? = null

    private var isFirstPopulation = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnBack = view.findViewById(R.id.btnBack)
        etName = view.findViewById(R.id.etName)
        etPhone = view.findViewById(R.id.etPhone)
        tvMemberSince = view.findViewById(R.id.tvMemberSince)
        etEmergencyName = view.findViewById(R.id.etEmergencyName)
        etEmergencyPhone = view.findViewById(R.id.etEmergencyPhone)
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
            val name = etName?.text?.toString().orEmpty()
            val phone = etPhone?.text?.toString().orEmpty()
            val emergencyName = etEmergencyName?.text?.toString().orEmpty()
            val emergencyPhone = etEmergencyPhone?.text?.toString().orEmpty()

            viewModel.save(name, phone, emergencyName, emergencyPhone)
        }
    }

    private fun renderUiState(state: DriverProfileUiState) {
        if (!state.isLoading) {
            if (isFirstPopulation || !state.isEditing) {
                etName?.setText(state.name)
                etPhone?.setText(state.phoneNumber)
                etEmergencyName?.setText(state.emergencyContactName)
                etEmergencyPhone?.setText(state.emergencyContactPhone)
                isFirstPopulation = false
            }

            tvMemberSince?.text = "Member Since: ${state.memberSinceText}"

            val isEditing = state.isEditing
            etName?.isEnabled = isEditing
            etPhone?.isEnabled = isEditing
            etEmergencyName?.isEnabled = isEditing
            etEmergencyPhone?.isEnabled = isEditing

            btnEdit?.text = if (isEditing) "Cancel" else "Edit"
            btnSave?.visibility = if (isEditing) View.VISIBLE else View.GONE
            btnSave?.isEnabled = !state.isSaving

            state.saveError?.let { errorMsg ->
                Snackbar.make(requireView(), errorMsg, Snackbar.LENGTH_LONG).show()
            }
        }
    }
}
