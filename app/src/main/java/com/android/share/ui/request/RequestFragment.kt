package com.android.share.ui.request

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.android.share.R
import com.android.share.databinding.FragmentRequestBinding
import com.android.share.manager.receive.ReceiveManagerImpl.RequestState
import com.android.share.ui.receive.ReceiveViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RequestFragment : DialogFragment() {

    private lateinit var binding: FragmentRequestBinding
    private val viewModel by activityViewModels<ReceiveViewModel>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = FragmentRequestBinding.inflate(layoutInflater)
        this.isCancelable = false

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.RoundedDialog).apply {
            this.setView(binding.root)
            this.setCancelable(false)
        }.create()

        getRequestState()

        return dialog
    }

    private fun getRequestState() = lifecycleScope.launch(Dispatchers.Main) {
        viewModel.requestState.collect {
            when (it) {
                is RequestState.RequestConnect -> {
                    binding.title.setText(R.string.request_connect_title)
                    binding.message.text = getString(R.string.request_connect_message, it.senderName, it.name)
                    binding.positive.setText(R.string.request_positive_accept)
                    binding.negative.setText(R.string.request_negative_refuse)
                    binding.fileIcon.setImageResource(R.drawable.icon_image)
                    binding.fileIcon.setColorFilter(resources.getColor(R.color.black, null))
                    binding.positive.setOnClickListener { viewModel.requestCallback(true) }
                    binding.negative.setOnClickListener {
                        viewModel.requestCallback(false)
                        dismiss()
                    }

                    binding.progress.visibility = View.GONE
                    binding.fileIcon.visibility = View.VISIBLE
                    binding.positive.visibility = View.VISIBLE
                    binding.negative.visibility = View.VISIBLE
                }
                RequestState.RequestFailed -> {
                    binding.title.setText(R.string.request_failed_title)
                    binding.message.setText(R.string.request_failed_message)
                    binding.positive.setText(R.string.request_positive_dismiss)
                    binding.fileIcon.setImageResource(R.drawable.icon_error)
                    binding.fileIcon.setColorFilter(resources.getColor(R.color.yellow, null))
                    binding.positive.setOnClickListener { dismiss() }
                    binding.progress.visibility = View.GONE
                    binding.negative.visibility = View.GONE
                    binding.fileIcon.visibility = View.VISIBLE
                    binding.positive.visibility = View.VISIBLE
                }
                is RequestState.RequestProgress -> {
                    binding.title.setText(R.string.request_progress_title)
                    binding.message.text = getString(R.string.request_progress_message, it.name)
                    binding.progress.setProgress(it.progress, true)
                    binding.negative.visibility = View.GONE
                    binding.positive.visibility = View.GONE
                    binding.fileIcon.visibility = View.GONE
                    binding.progress.visibility = View.VISIBLE
                }
                is RequestState.RequestComplete -> {
                    binding.title.setText(R.string.request_complete_title)
                    binding.message.text = getString(R.string.request_complete_message, it.name)
                    binding.positive.setText(R.string.request_positive_dismiss)
                    binding.fileIcon.setImageResource(R.drawable.icon_complete)
                    binding.fileIcon.setColorFilter(resources.getColor(R.color.green, null))
                    binding.positive.setOnClickListener { dismiss() }
                    binding.negative.visibility = View.GONE
                    binding.progress.visibility = View.GONE
                    binding.fileIcon.visibility = View.VISIBLE
                    binding.positive.visibility = View.VISIBLE
                }
                else -> Unit
            }
        }
    }
}