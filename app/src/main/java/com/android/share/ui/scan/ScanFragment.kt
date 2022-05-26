package com.android.share.ui.scan

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
import com.android.share.databinding.FragmentScanBinding
import com.android.share.manager.connectivity.ConnectivityManager
import com.android.share.manager.scan.ScanManagerImpl.ScanState
import com.android.share.util.navigateTo
import com.android.share.util.showSnackBar
import com.android.share.util.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ScanFragment : Fragment(R.layout.fragment_scan) {

    private val binding by viewBinding(FragmentScanBinding::bind)
    private val viewModel by viewModels<ScanViewModel>()

    private var fileUri = Uri.EMPTY
    private val directions = ScanFragmentDirections
    private lateinit var scanAdapter: ScanAdapter

    private lateinit var scanJob: Job
    private lateinit var scanResultJob: Job

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()
        scanResultJob = getScanResult()
    }

    private fun init() {
        scanAdapter = ScanAdapter(object : ScanAdapterCallback {
            override fun onReceiverClick(receiver: String) {
                if (fileUri != Uri.EMPTY && getFileFromUri(fileUri) != null) {
                    val action = directions.actionScanFragmentToSendFragment(receiver, fileUri)
                    findNavController().navigateTo(action, R.id.scanFragment)
                } else requireView().showSnackBar("Please choose file in order to share it")
            }
        })

        binding.recycler.adapter = scanAdapter
        binding.toolbar.setNavigationOnClickListener {
            val action = directions.actionScanFragmentToReceiveFragment()
            findNavController().navigateTo(action, R.id.scanFragment)
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
                    scanAdapter.setReceiversList(state.receivers)

                    binding.empty.visibility = View.GONE
                    binding.internet.visibility = View.GONE
                    binding.scanning.visibility = View.GONE
                    binding.result.visibility = View.VISIBLE
                }
                ScanState.Idle -> Unit
            }
        }
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
        super.onDestroyView()
    }
}