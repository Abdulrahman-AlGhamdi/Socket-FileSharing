package com.android.share.ui.request

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.android.share.R
import com.android.share.databinding.FragmentRequestBinding
import com.android.share.manager.sender.RequestManagerImpl.RequestState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class RequestFragment : DialogFragment() {

    private lateinit var binding: FragmentRequestBinding

    private val args by navArgs<RequestFragmentArgs>()
    private val viewModel by viewModels<RequestViewModel>()

    private lateinit var requestJob: Job
    private lateinit var requestResultJob: Job

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = FragmentRequestBinding.inflate(layoutInflater)
        isCancelable = false

        val alertDialog = AlertDialog.Builder(requireContext()).apply {
            this.setView(binding.root)
            this.setCancelable(false)
        }.create()

        requestResultJob = getRequestResult()

        return alertDialog
    }

    private fun getRequestResult() = lifecycleScope.launch(Dispatchers.Main) {
        viewModel.requestState.collect {
            when (it) {
                RequestState.RequestIdle -> {
                    val documentFile = DocumentFile.fromSingleUri(requireContext(), args.fileUri)!!
                    val fileName = documentFile.name ?: UUID.randomUUID().toString()
                    val uniqueNumber = args.receiver.substringAfterLast(".")

                    binding.message.text = getString(R.string.request_idle, fileName, uniqueNumber)
                    binding.positive.setText(R.string.request_positive_yes)
                    binding.negative.setText(R.string.request_negative_cancel)
                    binding.negative.setOnClickListener { dismiss() }
                    binding.positive.setOnClickListener {
                        if (::requestJob.isInitialized) requestJob.cancel()
                        requestJob = viewModel.sendRequest(args.receiver, documentFile)
                    }

                    binding.progress.visibility = View.GONE
                    binding.message.visibility = View.VISIBLE
                    binding.positive.visibility = View.VISIBLE
                    binding.negative.visibility = View.VISIBLE
                }
                RequestState.RequestStarted -> {
                    binding.message.setText(R.string.request_started)
                    binding.progress.visibility = View.GONE
                    binding.positive.visibility = View.GONE
                    binding.negative.visibility = View.GONE
                    binding.message.visibility = View.VISIBLE
                }
                RequestState.RequestFailed -> {
                    binding.message.setText(R.string.request_failed)
                    binding.positive.setText(R.string.request_positive_ok)
                    binding.positive.setOnClickListener { dismiss() }
                    binding.progress.visibility = View.GONE
                    binding.negative.visibility = View.GONE
                    binding.message.visibility = View.VISIBLE
                    binding.positive.visibility = View.VISIBLE
                }
                RequestState.RequestRefused -> {
                    binding.message.setText(R.string.request_refused)
                    binding.positive.setText(R.string.request_negative_cancel)
                    binding.positive.setOnClickListener { dismiss() }
                    binding.progress.visibility = View.GONE
                    binding.negative.visibility = View.GONE
                    binding.message.visibility = View.VISIBLE
                    binding.positive.visibility = View.VISIBLE
                }
                is RequestState.RequestProgress -> {
                    binding.message.text = getString(R.string.request_progress, it.name)
                    binding.progress.setProgress(it.progress, true)
                    binding.negative.visibility = View.GONE
                    binding.positive.visibility = View.GONE
                    binding.message.visibility = View.VISIBLE
                    binding.progress.visibility = View.VISIBLE
                }
                is RequestState.RequestComplete -> {
                    binding.message.text = getString(R.string.request_completed, it.name)
                    binding.positive.setText(R.string.request_positive_ok)
                    binding.positive.setOnClickListener { dismiss() }
                    binding.progress.visibility = View.GONE
                    binding.negative.visibility = View.GONE
                    binding.message.visibility = View.VISIBLE
                    binding.positive.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onDestroyView() {
        if (::requestJob.isInitialized) requestJob.cancel()
        if (::requestResultJob.isInitialized) requestResultJob.cancel()
        viewModel.closeClientSocket()
        super.onDestroyView()
    }
}