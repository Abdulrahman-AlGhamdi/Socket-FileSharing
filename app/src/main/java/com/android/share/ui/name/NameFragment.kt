package com.android.share.ui.name

import android.app.Dialog
import android.os.Bundle
import androidx.core.widget.addTextChangedListener
import com.android.share.R
import com.android.share.databinding.FragmentNameBinding
import com.android.share.manager.preference.PreferenceManager
import com.android.share.util.Constants
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class NameFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentNameBinding
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = FragmentNameBinding.inflate(layoutInflater)
        this.isCancelable = false

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.RoundedDialog).apply {
            this.setView(binding.root)
            this.setCancelable(false)
        }.create()

        init()

        return dialog
    }

    private fun init() {
        preferenceManager = PreferenceManager(requireContext())

        binding.confirm.setOnClickListener {
            val name = binding.name.text.toString()
            if (name.isEmpty()) return@setOnClickListener
            preferenceManager.putString(Constants.RECEIVER_NAME, name)
            this.dismiss()
        }

        binding.name.addTextChangedListener {
            val name = it.toString()
            if (name.isNotEmpty()) binding.nameHeader.error = null
            else binding.nameHeader.error = getString(R.string.name_error)
        }
    }
}