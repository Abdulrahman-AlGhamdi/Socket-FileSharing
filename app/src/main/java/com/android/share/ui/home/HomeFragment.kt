package com.android.share.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.share.R
import com.android.share.databinding.FragmentHomeBinding
import com.android.share.util.navigateTo
import com.android.share.util.viewBinding

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val binding by viewBinding(FragmentHomeBinding::bind)
    private val directions = HomeFragmentDirections

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()
    }

    private fun init() {
        binding.receiver.setOnClickListener {
            val action = directions.actionHomeFragmentToReceiverFragment()
            findNavController().navigateTo(action, R.id.homeFragment)
        }
        binding.sender.setOnClickListener {
            val action = directions.actionHomeFragmentToSenderFragment()
            findNavController().navigateTo(action, R.id.homeFragment)
        }
    }

    companion object {
        private const val TAG = "Scanning"
    }
}