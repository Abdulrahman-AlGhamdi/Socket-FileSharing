package com.android.share.ui.request

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.android.share.databinding.FragmentRequestBinding
import com.android.share.manager.receive.ReceiveManagerImpl.RequestState
import com.android.share.ui.receive.ReceiveViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RequestFragment : DialogFragment() {

    private lateinit var binding: FragmentRequestBinding
    private val viewModel by activityViewModels<ReceiveViewModel>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = FragmentRequestBinding.inflate(layoutInflater)
        this.isCancelable = false

        val dialog = AlertDialog.Builder(requireContext()).apply {
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
                    binding.title.text = "Request Connection"
                    binding.message.text = "${it.senderName} would like to share a ${it.name} file"
                    binding.positive.text = "Accept"
                    binding.negative.text = "Refuse"
                    binding.positive.setOnClickListener { viewModel.requestCallback(true) }
                    binding.negative.setOnClickListener {
                        viewModel.requestCallback(false)
                        dismiss()
                    }

                    binding.progress.visibility = View.GONE
                    binding.positive.visibility = View.VISIBLE
                    binding.negative.visibility = View.VISIBLE
                }
                RequestState.RequestFailed -> {
                    binding.title.text = "Request Connection"
                    binding.message.text = "Something went wrong"
                    binding.positive.text = "Dismiss"
                    binding.positive.setOnClickListener { dismiss() }
                    binding.progress.visibility = View.GONE
                    binding.negative.visibility = View.GONE
                    binding.positive.visibility = View.VISIBLE
                }
                is RequestState.RequestProgress -> {
                    binding.title.text = "Request Connection"
                    binding.message.text = "Receiving ${it.name} file"
                    binding.progress.setProgress(it.progress, true)
                    binding.negative.visibility = View.GONE
                    binding.positive.visibility = View.GONE
                    binding.progress.visibility = View.VISIBLE
                }
                is RequestState.RequestComplete -> {
                    binding.title.text = "File Received"
                    binding.message.text = "${it.name} file has been received successfully"
                    binding.positive.text = "Dismiss"
                    binding.positive.setOnClickListener { dismiss() }
                    binding.negative.visibility = View.GONE
                    binding.progress.visibility = View.GONE
                    binding.positive.visibility = View.VISIBLE
                }
                else -> Unit
            }
        }
    }
}