package com.android.share.ui.send

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.android.share.R
import com.android.share.databinding.FragmentSendBinding
import com.android.share.manager.send.SendManagerImpl.RequestState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class SendFragment : DialogFragment() {

    private lateinit var binding: FragmentSendBinding

    private val args by navArgs<SendFragmentArgs>()
    private val viewModel by viewModels<SendViewModel>()

    private lateinit var requestJob: Job
    private lateinit var requestResultJob: Job

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = FragmentSendBinding.inflate(layoutInflater)
        this.isCancelable = false

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.RoundedDialog).apply {
            this.setView(binding.root)
            this.setCancelable(false)
        }.create()

        requestResultJob = getRequestResult()

        return dialog
    }

    private fun getRequestResult() = lifecycleScope.launch(Dispatchers.Main) {
        viewModel.sendState.collect {
            when (it) {
                RequestState.RequestIdle -> {
                    val documentFile = DocumentFile.fromSingleUri(requireContext(), args.fileUri)!!
                    val fileName = documentFile.name ?: UUID.randomUUID().toString()

                    binding.title.text = fileName
                    binding.message.text = getString(R.string.send_idle, args.receiverName)
                    binding.positive.setText(R.string.send_positive_send)
                    binding.negative.setText(R.string.negative_cancel)
                    binding.fileIcon.setImageResource(R.drawable.icon_file)
                    binding.fileIcon.setColorFilter(resources.getColor(R.color.black, null))
                    binding.negative.setOnClickListener { dismiss() }
                    binding.positive.setOnClickListener {
                        if (::requestJob.isInitialized) requestJob.cancel()
                        requestJob = viewModel.sendRequest(args.receiverIpAddress, documentFile)
                    }

                    binding.progress.visibility = View.GONE
                    binding.fileIcon.visibility = View.VISIBLE
                    binding.positive.visibility = View.VISIBLE
                    binding.negative.visibility = View.VISIBLE
                }
                RequestState.RequestStarted -> {
                    binding.title.setText(R.string.send_started_title)
                    binding.message.setText(R.string.send_started_message)
                    binding.progress.visibility = View.GONE
                    binding.positive.visibility = View.GONE
                    binding.negative.visibility = View.GONE
                    binding.fileIcon.visibility = View.GONE
                }
                RequestState.RequestFailed -> {
                    binding.title.setText(R.string.send_failed_title)
                    binding.message.setText(R.string.send_failed_message)
                    binding.positive.setText(R.string.send_positive_dismiss)
                    binding.fileIcon.setImageResource(R.drawable.icon_error)
                    binding.fileIcon.setColorFilter(resources.getColor(R.color.yellow, null))
                    binding.positive.setOnClickListener { dismiss() }
                    binding.progress.visibility = View.GONE
                    binding.negative.visibility = View.GONE
                    binding.fileIcon.visibility = View.VISIBLE
                    binding.positive.visibility = View.VISIBLE
                }
                RequestState.RequestRefused -> {
                    binding.title.setText(R.string.send_refused_title)
                    binding.message.setText(R.string.send_refused_message)
                    binding.positive.setText(R.string.send_positive_dismiss)
                    binding.fileIcon.setImageResource(R.drawable.icon_denied)
                    binding.fileIcon.setColorFilter(resources.getColor(R.color.red, null))
                    binding.positive.setOnClickListener { dismiss() }
                    binding.progress.visibility = View.GONE
                    binding.negative.visibility = View.GONE
                    binding.fileIcon.visibility = View.VISIBLE
                    binding.positive.visibility = View.VISIBLE
                }
                is RequestState.RequestProgress -> {
                    binding.title.setText(R.string.send_progress_title)
                    binding.message.text = getString(R.string.send_progress_message, it.name)
                    binding.progress.setProgress(it.progress, true)
                    binding.negative.visibility = View.GONE
                    binding.positive.visibility = View.GONE
                    binding.fileIcon.visibility = View.GONE
                    binding.progress.visibility = View.VISIBLE
                }
                is RequestState.RequestComplete -> {
                    binding.title.setText(R.string.send_completed_title)
                    binding.message.text = getString(R.string.send_completed_message, it.name)
                    binding.positive.setText(R.string.send_positive_dismiss)
                    binding.fileIcon.setImageResource(R.drawable.icon_complete)
                    binding.fileIcon.setColorFilter(resources.getColor(R.color.green, null))
                    binding.positive.setOnClickListener { dismiss() }
                    binding.progress.visibility = View.GONE
                    binding.negative.visibility = View.GONE
                    binding.message.visibility = View.VISIBLE
                    binding.fileIcon.visibility = View.VISIBLE
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