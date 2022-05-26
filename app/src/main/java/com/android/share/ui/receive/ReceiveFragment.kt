package com.android.share.ui.receive

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.android.share.R
import com.android.share.databinding.FragmentReceiveBinding
import com.android.share.manager.connectivity.ConnectivityManager
import com.android.share.manager.receiver.ReceiveManagerImpl.ReceiveState
import com.android.share.util.viewBinding
import com.google.android.material.progressindicator.LinearProgressIndicator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReceiveFragment : Fragment(R.layout.fragment_receive) {

    private val binding by viewBinding(FragmentReceiveBinding::bind)
    private val viewModel by viewModels<ReceiveViewModel>()

    private var isActive = false
    private var progressDialog: AlertDialog? = null
    private lateinit var progress: LinearProgressIndicator
    private lateinit var alertDialog: AlertDialog

    private lateinit var receiveJob: Job
    private lateinit var receiveStateJob: Job

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()
    }

    private fun init() {
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        binding.receive.setOnClickListener {
            isActive = !isActive
            updateButtonStyle()
        }

        ConnectivityManager(requireContext()).observe(viewLifecycleOwner) { hasInternet ->
            if (hasInternet) {
                binding.receive.isEnabled = true
                binding.progress.visibility = View.GONE
                binding.internet.visibility = View.GONE
                binding.receiving.visibility = View.GONE
                binding.receive.visibility = View.VISIBLE
            } else {
                if (::receiveJob.isInitialized) receiveJob.cancel()

                isActive = false
                dismissDialog()
                updateButtonStyle()
                viewModel.closeServerSocket()

                binding.receive.isEnabled = true
                binding.receive.visibility = View.GONE
                binding.progress.visibility = View.GONE
                binding.receiving.visibility = View.GONE
                binding.internet.visibility = View.VISIBLE
            }
        }
    }

    private fun getReceiveState() = lifecycleScope.launch(Dispatchers.Main) {
        viewModel.receiveState.collect {
            when (it) {
                ReceiveState.ReceiveInitializing -> {
                    dismissDialog()
                    binding.receive.isEnabled = false
                    binding.receiving.visibility = View.GONE
                    binding.internet.visibility = View.GONE
                    binding.receive.visibility = View.VISIBLE
                    binding.progress.visibility = View.VISIBLE
                }
                is ReceiveState.ReceiveStarted -> {
                    dismissDialog()
//                    binding.receiver.text = it.uniqueNumber
                    binding.receive.isEnabled = true
                    binding.progress.visibility = View.GONE
                    binding.internet.visibility = View.GONE
                    binding.receive.visibility = View.VISIBLE
                    binding.receiving.visibility = View.VISIBLE
                }
                ReceiveState.Failed -> {
                    if (::receiveJob.isInitialized) receiveJob.cancel()
                    isActive = false
                    updateButtonStyle()
                    dismissDialog()

                    binding.receive.isEnabled = true
                    binding.progress.visibility = View.GONE
                    binding.internet.visibility = View.GONE
                    binding.receiving.visibility = View.GONE
                    binding.receive.visibility = View.VISIBLE
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

    private fun updateButtonStyle(): Unit = if (isActive) {
        binding.receive.setBackgroundColor(resources.getColor(R.color.red, null))
        binding.receive.setText(R.string.receive_button_stop)

        if (::receiveJob.isInitialized) receiveJob.cancel()
        dismissDialog()
        receiveJob = viewModel.startReceiving()
        receiveStateJob = getReceiveState()
    } else {
        viewModel.closeServerSocket()
        binding.receive.setBackgroundColor(resources.getColor(R.color.green, null))
        binding.receive.setText(R.string.receive_button_start)

        binding.receive.isEnabled = true
        binding.receiving.visibility = View.GONE
        binding.internet.visibility = View.GONE
        binding.progress.visibility = View.GONE
        binding.receive.visibility = View.VISIBLE
    }

    private fun dismissDialog() {
        progressDialog?.dismiss()
        progressDialog = null
        if (::alertDialog.isInitialized) alertDialog.dismiss()
    }

    override fun onDestroyView() {
        if (::receiveJob.isInitialized) receiveJob.cancel()
        if (::receiveStateJob.isInitialized) receiveStateJob.cancel()
        viewModel.closeServerSocket()
        super.onDestroyView()
    }
}