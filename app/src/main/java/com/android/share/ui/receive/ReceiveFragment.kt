package com.android.share.ui.receive

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.android.share.R
import com.android.share.databinding.FragmentReceiveBinding
import com.android.share.manager.connectivity.ConnectivityManager
import com.android.share.manager.receive.ReceiveManagerImpl.ReceiveState
import com.android.share.manager.receive.ReceiveManagerImpl.RequestState
import com.android.share.util.navigateTo
import com.android.share.util.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReceiveFragment : Fragment(R.layout.fragment_receive) {

    private val binding by viewBinding(FragmentReceiveBinding::bind)
    private val viewModel by activityViewModels<ReceiveViewModel>()
    private val directions = ReceiveFragmentDirections

    private lateinit var receiveJob: Job
    private lateinit var receiveStateJob: Job
    private lateinit var requestStateJob: Job
    private lateinit var buttonStatus: ButtonStatus

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()
        receiveStateJob = getReceiveState()
        requestStateJob = getRequestState()
    }

    private fun init() {
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId != R.id.imported_files) return@setOnMenuItemClickListener false
            val action = directions.actionReceiveFragmentToImportedFilesFragment()
            findNavController().navigateTo(action, R.id.receiveFragment)
            true
        }

        binding.receive.setOnClickListener {
            if (buttonStatus == ButtonStatus.NOT_ACTIVE) {
                if (::receiveJob.isInitialized) receiveJob.cancel()
                receiveJob = viewModel.startReceiving()
            } else viewModel.closeServerSocket()
        }

        ConnectivityManager(requireContext()).observe(viewLifecycleOwner) { hasInternet ->
            if (hasInternet) {
                buttonStatus = ButtonStatus.NOT_ACTIVE
                updateButtonStyle(buttonStatus)
                binding.progress.visibility = View.GONE
                binding.internet.visibility = View.GONE
                binding.animation.visibility = View.GONE
                binding.receive.visibility = View.VISIBLE
            } else {
                if (::receiveJob.isInitialized) receiveJob.cancel()
                viewModel.closeServerSocket()
                buttonStatus = ButtonStatus.NOT_ACTIVE
                updateButtonStyle(buttonStatus)
                binding.receive.visibility = View.GONE
                binding.progress.visibility = View.GONE
                binding.animation.visibility = View.GONE
                binding.internet.visibility = View.VISIBLE
            }
        }
    }

    private fun getReceiveState() = lifecycleScope.launch(Dispatchers.Main) {
        viewModel.receiveState.collect {
            when (it) {
                ReceiveState.ReceiveInitializing -> {
                    buttonStatus = ButtonStatus.INITIALIZING
                    updateButtonStyle(buttonStatus)
                    binding.internet.visibility = View.GONE
                    binding.animation.visibility = View.GONE
                    binding.receive.visibility = View.VISIBLE
                    binding.progress.visibility = View.VISIBLE
                }
                ReceiveState.ReceiveStarted -> {
                    buttonStatus = ButtonStatus.ACTIVE
                    updateButtonStyle(buttonStatus)
                    binding.progress.visibility = View.GONE
                    binding.internet.visibility = View.GONE
                    binding.receive.visibility = View.VISIBLE
                    binding.animation.visibility = View.VISIBLE
                }
                ReceiveState.ReceiveClosed -> {
                    buttonStatus = ButtonStatus.NOT_ACTIVE
                    updateButtonStyle(buttonStatus)
                    binding.internet.visibility = View.GONE
                    binding.progress.visibility = View.GONE
                    binding.animation.visibility = View.GONE
                    binding.receive.visibility = View.VISIBLE
                }
                ReceiveState.ReceiveIdle -> Unit
            }
        }
    }

    private fun getRequestState() = lifecycleScope.launch(Dispatchers.Main) {
        viewModel.requestState.collect {
            when (it) {
                is RequestState.RequestConnect -> {
                    val action = directions.actionReceiveFragmentToRequestFragment()
                    findNavController().navigateTo(action, R.id.receiveFragment)
                }
                else -> Unit
            }
        }
    }

    private fun updateButtonStyle(buttonStatus: ButtonStatus): Unit = when (buttonStatus) {
        ButtonStatus.INITIALIZING -> {
            binding.receive.text = null
            binding.receive.isEnabled = false
            binding.receive.setBackgroundColor(resources.getColor(R.color.gray, null))
        }
        ButtonStatus.ACTIVE -> {
            binding.receive.isEnabled = true
            binding.receive.setText(R.string.receive_button_stop)
            binding.receive.setBackgroundColor(resources.getColor(R.color.red, null))
            binding.message.setText(R.string.receive_message_on)
        }
        ButtonStatus.NOT_ACTIVE -> {
            binding.receive.isEnabled = true
            binding.receive.setText(R.string.receive_button_start)
            binding.receive.setBackgroundColor(resources.getColor(R.color.green, null))
            binding.message.setText(R.string.receive_message_off)
        }
    }

    override fun onStop() {
        if (::receiveJob.isInitialized) receiveJob.cancel()
        viewModel.closeServerSocket()
        super.onStop()
    }

    private enum class ButtonStatus { ACTIVE, NOT_ACTIVE, INITIALIZING }

    override fun onDestroyView() {
        if (::receiveJob.isInitialized) receiveJob.cancel()
        if (::receiveStateJob.isInitialized) receiveStateJob.cancel()
        if (::requestStateJob.isInitialized) requestStateJob.cancel()
        viewModel.closeServerSocket()
        super.onDestroyView()
    }
}