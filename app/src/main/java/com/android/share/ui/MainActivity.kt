package com.android.share.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.android.share.R
import com.android.share.databinding.ActivityMainBinding
import com.android.share.util.viewBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val binding by viewBinding(ActivityMainBinding::inflate)
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        navController = findNavController(R.id.fragment_container)
        navigateToSenderFragment(fileIntent = this.intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        navigateToSenderFragment(fileIntent = intent)
    }

    private fun navigateToSenderFragment(fileIntent: Intent) {
        if (fileIntent.action != Intent.ACTION_SEND) return
        val currentFragmentId = navController.currentDestination?.id
        if (currentFragmentId != R.id.scanFragment) navController.navigate(R.id.scanFragment)
        this.intent = fileIntent
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
