package com.android.share.ui.sender

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.android.share.R
import com.android.share.databinding.FragmentSenderBinding
import com.android.share.manager.request.RequestManagerImpl.RequestState
import com.android.share.manager.scan.ScanManagerImpl.ScanState
import com.android.share.util.showSnackBar
import com.android.share.util.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class SenderFragment : Fragment(R.layout.fragment_sender) {

    private val binding by viewBinding(FragmentSenderBinding::bind)
    private val viewModel by viewModels<SenderViewModel>()
    private lateinit var senderAdapter: SenderAdapter
    private lateinit var alertDialog: AlertDialog
    private var fileUri = Uri.EMPTY

    private lateinit var scanJob: Job
    private lateinit var scanResultJob: Job
    private lateinit var requestJob: Job
    private lateinit var requestResultJob: Job

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()
        scanJob = viewModel.startScanning()
        scanResultJob = getScanResult()
        requestResultJob = getRequestResult()
    }

    private fun init() {
        senderAdapter = SenderAdapter(object : SenderAdapterListeners {
            override fun onReceiverClick(receiver: String) {
                requestConnection(receiver)
            }
        })

        binding.recycler.adapter = senderAdapter
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId != R.id.rescan) return@setOnMenuItemClickListener false
            if (::scanJob.isInitialized) scanJob.cancel()
            scanJob = viewModel.startScanning()
            true
        }
    }

    private fun getScanResult() = lifecycleScope.launch(Dispatchers.Main) {
        viewModel.scanState.collect { state ->
            when (state) {
                ScanState.Empty -> {
                    binding.result.visibility = View.GONE
                    binding.internet.visibility = View.GONE
                    binding.scanning.visibility = View.GONE
                    binding.empty.visibility = View.VISIBLE
                }
                ScanState.NoInternet -> {
                    binding.empty.visibility = View.GONE
                    binding.result.visibility = View.GONE
                    binding.scanning.visibility = View.GONE
                    binding.internet.visibility = View.VISIBLE
                }
                is ScanState.Progress -> {
                    binding.progress.max = state.max
                    binding.progress.setProgress(state.progress, true)
                    binding.sender.text = state.uniqueNumber

                    binding.empty.visibility = View.GONE
                    binding.result.visibility = View.GONE
                    binding.internet.visibility = View.GONE
                    binding.scanning.visibility = View.VISIBLE
                }
                is ScanState.Complete -> {
                    senderAdapter.setReceiversList(state.receivers)

                    binding.empty.visibility = View.GONE
                    binding.internet.visibility = View.GONE
                    binding.scanning.visibility = View.GONE
                    binding.result.visibility = View.VISIBLE
                }
                ScanState.Idle -> Unit
            }
        }
    }

    private fun getRequestResult() = lifecycleScope.launch(Dispatchers.Main) {
        viewModel.requestState.collect {
            when (it) {
                RequestState.RequestStarted -> {
                    if (::alertDialog.isInitialized) alertDialog.dismiss()
                    alertDialog = AlertDialog.Builder(requireContext()).apply {
                        this.setTitle("Request Connection")
                        this.setMessage("Request connection has been sent the receiver and waiting for the approval")
                        this.setCancelable(false)
                    }.show()
                }
                RequestState.RequestFailed -> {
                    if (::alertDialog.isInitialized) alertDialog.dismiss()
                    alertDialog = AlertDialog.Builder(requireContext()).apply {
                        this.setTitle("Request Connection")
                        this.setMessage("The receiver requested is no longer available")
                        this.setPositiveButton("Scan") { _, _ ->
                            scanJob = viewModel.startScanning()
                        }
                    }.show()
                }
                RequestState.RequestAccepted -> {
                    if (::alertDialog.isInitialized) alertDialog.dismiss()
                    alertDialog = AlertDialog.Builder(requireContext()).apply {
                        this.setTitle("Request Connection")
                        this.setMessage("The receiver has accepted your connection request")
                        this.setCancelable(false)
                        this.setPositiveButton("OK", null)
                    }.show()
                }
                RequestState.RequestRefused -> {
                    if (::alertDialog.isInitialized) alertDialog.dismiss()
                    alertDialog = AlertDialog.Builder(requireContext()).apply {
                        this.setTitle("Request Connection")
                        this.setMessage("The receiver has refused your connection request")
                        this.setNegativeButton("Cancel", null)
                    }.show()
                }
                RequestState.Idle -> Unit
            }
        }
    }

    private fun requestConnection(receiver: String) {
        if (::requestJob.isInitialized) requestJob.cancel()
        val documentFile = if (fileUri != Uri.EMPTY) getFileFromUri(fileUri) else null

        if (documentFile == null) {
            requireView().showSnackBar("Please choose file in order to share it")
            return
        }

        val uniqueNumber = receiver.substringAfterLast(".")
        val name = documentFile.name ?: UUID.randomUUID().toString()

        alertDialog = AlertDialog.Builder(requireContext()).apply {
            this.setTitle("Request File Sending")
            this.setMessage("Are you sure you want to send $name file to receiver number: $uniqueNumber?")
            this.setCancelable(false)
            this.setPositiveButton("Yes") { _, _ ->
                requestJob = viewModel.requestConnection(receiver, documentFile)
            }
            this.setNegativeButton("Cancel", null)
        }.show()
    }

    override fun onResume() {
        super.onResume()
        val fileIntent = requireActivity().intent
        if (fileIntent.action != Intent.ACTION_SEND) return
        fileUri = fileIntent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as Uri
    }

    private fun getFileFromUri(fileUri: Uri): DocumentFile? {
        val documentFile = DocumentFile.fromSingleUri(requireContext(), fileUri) ?: return null
        val fileSize = documentFile.length().div(1024 * 1024)

        if (fileSize > 150) {
            requireView().showSnackBar("The file size is over 150MB")
            return null
        }

        return documentFile
    }

    override fun onDestroyView() {
        if (::scanJob.isInitialized) scanJob.cancel()
        if (::scanResultJob.isInitialized) scanResultJob.cancel()
        if (::requestJob.isInitialized) requestJob.cancel()
        if (::requestResultJob.isInitialized) requestResultJob.cancel()
        viewModel.closeClientSocket()
        super.onDestroyView()
    }
}