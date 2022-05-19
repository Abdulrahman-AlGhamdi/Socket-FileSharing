package com.android.share.ui.sender

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.android.share.R
import com.android.share.databinding.FragmentSenderBinding
import com.android.share.manager.request.RequestManagerImpl.RequestState
import com.android.share.manager.scan.ScanManagerImpl.ScanState
import com.android.share.util.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SenderFragment : Fragment(R.layout.fragment_sender) {

    private val binding by viewBinding(FragmentSenderBinding::bind)
    private val viewModel by viewModels<SenderViewModel>()
    private lateinit var senderAdapter: SenderAdapter
    private lateinit var alertDialog: AlertDialog

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

    private fun requestConnection(receiver: String) {
        if (::requestJob.isInitialized) requestJob.cancel()
        val uniqueNumber = receiver.substringAfterLast(".")

        alertDialog = AlertDialog.Builder(requireContext()).apply {
            this.setTitle("Request Connection")
            this.setMessage("Are you sure you want to connect to receiver number: $uniqueNumber?")
            this.setCancelable(false)
            this.setPositiveButton("Yes") { _, _ ->
                requestJob = viewModel.requestConnection(receiver)
            }
            this.setNegativeButton("Cancel", null)
        }.show()
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

    override fun onDestroyView() {
        if (::scanJob.isInitialized) scanJob.cancel()
        if (::scanResultJob.isInitialized) scanResultJob.cancel()
        if (::requestJob.isInitialized) requestJob.cancel()
        if (::requestResultJob.isInitialized) requestResultJob.cancel()
        viewModel.closeClientSocket()
        super.onDestroyView()
    }
}