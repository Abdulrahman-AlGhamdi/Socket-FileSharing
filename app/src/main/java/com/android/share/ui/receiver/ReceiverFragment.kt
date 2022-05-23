package com.android.share.ui.receiver

import android.app.AlertDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.android.share.R
import com.android.share.databinding.FragmentReceiverBinding
import com.android.share.manager.receiver.ReceiverManagerImpl.ReceiveState
import com.android.share.util.viewBinding
import com.google.android.material.progressindicator.LinearProgressIndicator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReceiverFragment : Fragment(R.layout.fragment_receiver) {

    private val binding by viewBinding(FragmentReceiverBinding::bind)
    private val viewModel by viewModels<ReceiverViewModel>()

    private lateinit var cm: ConnectivityManager
    private var progressDialog: AlertDialog? = null
    private lateinit var progress: LinearProgressIndicator
    private lateinit var alertDialog: AlertDialog

    private lateinit var authenticateJob: Job
    private lateinit var authenticateResultJob: Job

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()
    }

    private fun init() {
        cm = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.registerDefaultNetworkCallback(networkCallback)
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            lifecycleScope.launch(Dispatchers.Main) {
                dismissDialog()
                authenticateJob = viewModel.startAuthentication()
                authenticateResultJob = getAuthenticateResult()
            }
        }

        override fun onLost(network: Network) {
            lifecycleScope.launch(Dispatchers.Main) {
                dismissDialog()
                viewModel.closeServerSocket()
                binding.progress.visibility = View.GONE
                binding.receiving.visibility = View.GONE
                binding.internet.visibility = View.VISIBLE
            }
        }
    }

    private fun getAuthenticateResult() = lifecycleScope.launch(Dispatchers.Main) {
        viewModel.authenticateState.collect {
            when (it) {
                ReceiveState.ReceiveInitializing -> {
                    dismissDialog()
                    binding.internet.visibility = View.GONE
                    binding.receiving.visibility = View.GONE
                    binding.progress.visibility = View.VISIBLE
                }
                is ReceiveState.ReceiveStarted -> {
                    dismissDialog()
                    binding.receiver.text = it.uniqueNumber
                    binding.progress.visibility = View.GONE
                    binding.internet.visibility = View.GONE
                    binding.receiving.visibility = View.VISIBLE
                }
                ReceiveState.Failed -> {
                    dismissDialog()
                    authenticateJob = viewModel.startAuthentication()
                }
                is ReceiveState.Connect -> {
                    dismissDialog()
                    alertDialog = AlertDialog.Builder(requireContext()).apply {
                        this.setTitle("Request Connection")
                        this.setMessage("Sender number: ${it.uniqueNumber} would like to share a ${it.name} file")
                        this.setCancelable(false)
                        this.setPositiveButton("Accept") { _, _ -> viewModel.requestCallback(true) }
                        this.setNegativeButton("Refuse") { _, _ -> viewModel.requestCallback(false) }
                    }.show()
                }
                is ReceiveState.ReceiveProgress -> {
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
                        this.setTitle("Receiving ${it.name} file")
                    }.show()
                }
                is ReceiveState.ReceiveComplete -> {
                    dismissDialog()
                    alertDialog = AlertDialog.Builder(requireContext()).apply {
                        this.setTitle("File Received")
                        this.setMessage("${it.name} file has been received successfully")
                        this.setPositiveButton("OK", null)
                    }.show()
                }
                ReceiveState.Idle -> Unit
            }
        }
    }

    private fun dismissDialog() {
        progressDialog?.dismiss()
        progressDialog = null
        if (::alertDialog.isInitialized) alertDialog.dismiss()
    }

    override fun onDestroyView() {
        if (::authenticateJob.isInitialized) authenticateJob.cancel()
        if (::authenticateResultJob.isInitialized) authenticateResultJob.cancel()
        viewModel.closeServerSocket()
        cm.unregisterNetworkCallback(networkCallback)
        super.onDestroyView()
    }
}