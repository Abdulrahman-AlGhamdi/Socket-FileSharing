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
import com.android.share.manager.scan.ScanManagerImpl.ScanState
import com.android.share.manager.sender.SenderManagerImpl.SendState
import com.android.share.manager.connectivity.ConnectivityManager
import com.android.share.util.navigateTo
import com.android.share.util.showSnackBar
import com.android.share.util.viewBinding
import com.google.android.material.progressindicator.LinearProgressIndicator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class SenderFragment : Fragment(R.layout.fragment_sender) {

    private val binding by viewBinding(FragmentSenderBinding::bind)
    private val viewModel by viewModels<SenderViewModel>()
    private val directions = SenderFragmentDirections
    private var progressDialog: AlertDialog? = null
    private var fileUri = Uri.EMPTY

    private lateinit var senderAdapter: SenderAdapter
    private lateinit var progress: LinearProgressIndicator
    private lateinit var alertDialog: AlertDialog

    private lateinit var scanJob: Job
    private lateinit var scanResultJob: Job
    private lateinit var requestJob: Job
    private lateinit var requestResultJob: Job

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()
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
        binding.toolbar.setNavigationOnClickListener {
            val action = directions.actionSenderFragmentToReceiverFragment()
            findNavController().navigateTo(action, R.id.senderFragment)
        }

        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId != R.id.rescan) return@setOnMenuItemClickListener false
            if (::scanJob.isInitialized) scanJob.cancel()
            scanJob = viewModel.startScanning()
            true
        }

        ConnectivityManager(requireContext()).observe(viewLifecycleOwner) { hasInternet ->
            if (hasInternet) {
                if (::scanJob.isInitialized) scanJob.cancel()
                scanJob = viewModel.startScanning()
            } else {
                binding.empty.visibility = View.GONE
                binding.result.visibility = View.GONE
                binding.scanning.visibility = View.GONE
                binding.internet.visibility = View.VISIBLE
            }
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
                SendState.SendStarted -> {
                    dismissDialog()
                    alertDialog = AlertDialog.Builder(requireContext()).apply {
                        this.setTitle("Request Connection")
                        this.setMessage("Request connection has been sent the receiver and waiting for the approval")
                        this.setCancelable(false)
                    }.show()
                }
                SendState.SendFailed -> {
                    dismissDialog()
                    alertDialog = AlertDialog.Builder(requireContext()).apply {
                        this.setTitle("Request Connection")
                        this.setMessage("The receiver requested is no longer available")
                        this.setPositiveButton("Scan") { _, _ ->
                            scanJob = viewModel.startScanning()
                        }
                    }.show()
                }
                SendState.SendAccepted -> {
                    dismissDialog()
                    alertDialog = AlertDialog.Builder(requireContext()).apply {
                        this.setTitle("Request Connection")
                        this.setMessage("The receiver has accepted your connection request")
                        this.setCancelable(false)
                        this.setPositiveButton("OK", null)
                    }.show()
                }
                SendState.SendRefused -> {
                    dismissDialog()
                    alertDialog = AlertDialog.Builder(requireContext()).apply {
                        this.setTitle("Request Connection")
                        this.setMessage("The receiver has refused your connection request")
                        this.setNegativeButton("Cancel", null)
                    }.show()
                }
                is SendState.SendProgress -> {
                    if (progressDialog != null) {
                        progress.setProgress(it.progress, true)
                        return@collect
                    }

                    dismissDialog()
                    val dialogView = layoutInflater.inflate(R.layout.receiver_progress_dialog, null)
                    progress = dialogView.findViewById(R.id.progress)

                    progressDialog = AlertDialog.Builder(requireContext()).apply {
                        this.setView(dialogView)
                        this.setCancelable(false)
                        this.setTitle("Sending ${it.name} file")
                    }.show()
                }
                is SendState.SendComplete -> {
                    dismissDialog()
                    fileUri = Uri.EMPTY

                    alertDialog = AlertDialog.Builder(requireContext()).apply {
                        this.setTitle("File Sent")
                        this.setMessage("${it.name} file has been sent successfully")
                        this.setPositiveButton("OK", null)
                    }.show()
                }
                SendState.Idle -> Unit
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
                requestJob = viewModel.sendRequest(receiver, documentFile)
            }
            this.setNegativeButton("Cancel", null)
        }.show()
    }

    private fun dismissDialog() {
        progressDialog?.dismiss()
        progressDialog = null
        if (::alertDialog.isInitialized) alertDialog.dismiss()
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