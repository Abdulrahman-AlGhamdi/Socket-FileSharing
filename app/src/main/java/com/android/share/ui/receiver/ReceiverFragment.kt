package com.android.share.ui.receiver

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.android.share.R
import com.android.share.databinding.FragmentReceiverBinding
import com.android.share.manager.authenticate.AuthenticateManagerImpl.AuthenticateState
import com.android.share.util.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReceiverFragment : Fragment(R.layout.fragment_receiver) {

    private val binding by viewBinding(FragmentReceiverBinding::bind)
    private val viewModel by viewModels<ReceiverViewModel>()
    private lateinit var alertDialog: AlertDialog

    private lateinit var authenticateJob: Job
    private lateinit var authenticateResultJob: Job

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()
        authenticateJob = viewModel.startAuthentication()
        authenticateResultJob = getAuthenticateResult()
    }

    private fun init() {
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
    }

    private fun getAuthenticateResult() = lifecycleScope.launch(Dispatchers.Main) {
        viewModel.authenticateState.collect {
            when (it) {
                AuthenticateState.ReceiveInitializing -> {
                    binding.internet.visibility = View.GONE
                    binding.receiving.visibility = View.GONE
                    binding.progress.visibility = View.VISIBLE
                }
                is AuthenticateState.ReceiveStarted -> {
                    binding.receiver.text = it.uniqueNumber
                    binding.progress.visibility = View.GONE
                    binding.internet.visibility = View.GONE
                    binding.receiving.visibility = View.VISIBLE
                }
                AuthenticateState.Failed -> {
                    if (::alertDialog.isInitialized) alertDialog.dismiss()
                    authenticateJob = viewModel.startAuthentication()
                }
                AuthenticateState.NoInternet -> {
                    binding.progress.visibility = View.GONE
                    binding.receiving.visibility = View.GONE
                    binding.internet.visibility = View.VISIBLE
                }
                is AuthenticateState.Connect -> {
                    if (::alertDialog.isInitialized) alertDialog.dismiss()
                    alertDialog = AlertDialog.Builder(requireContext()).apply {
                        this.setTitle("Request Connection")
                        this.setMessage("Sender number: ${it.uniqueNumber} would like to share a ${it.name} file")
                        this.setCancelable(false)
                        this.setPositiveButton("Accept") { _, _ -> viewModel.acceptConnection(true) }
                        this.setNegativeButton("Refuse") { _, _ -> viewModel.acceptConnection(false) }
                    }.show()
                }
                AuthenticateState.Idle -> Unit
            }
        }
    }

    override fun onDestroyView() {
        if (::authenticateJob.isInitialized) authenticateJob.cancel()
        if (::authenticateResultJob.isInitialized) authenticateResultJob.cancel()
        viewModel.closeServerSocket()
        super.onDestroyView()
    }
}