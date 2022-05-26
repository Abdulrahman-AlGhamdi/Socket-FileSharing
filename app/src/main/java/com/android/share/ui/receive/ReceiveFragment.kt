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
    private var isActive = false

    private lateinit var receiveJob: Job
    private lateinit var receiveStateJob: Job
    private lateinit var requestStateJob: Job

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
                    binding.receive.isEnabled = false
                    binding.receiving.visibility = View.GONE
                    binding.internet.visibility = View.GONE
                    binding.receive.visibility = View.VISIBLE
                    binding.progress.visibility = View.VISIBLE
                }
                is ReceiveState.ReceiveStarted -> {
//                    binding.receiver.text = it.uniqueNumber
                    binding.receive.isEnabled = true
                    binding.progress.visibility = View.GONE
                    binding.internet.visibility = View.GONE
                    binding.receive.visibility = View.VISIBLE
                    binding.receiving.visibility = View.VISIBLE
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

    private fun updateButtonStyle(): Unit = if (isActive) {
        binding.receive.setBackgroundColor(resources.getColor(R.color.red, null))
        binding.receive.setText(R.string.receive_button_stop)

        if (::receiveJob.isInitialized) receiveJob.cancel()
        receiveJob = viewModel.startReceiving()
        receiveStateJob = getReceiveState()
        requestStateJob = getRequestState()
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

    override fun onDestroyView() {
        if (::receiveJob.isInitialized) receiveJob.cancel()
        if (::receiveStateJob.isInitialized) receiveStateJob.cancel()
        if (::requestStateJob.isInitialized) requestStateJob.cancel()
        viewModel.closeServerSocket()
        super.onDestroyView()
    }
}