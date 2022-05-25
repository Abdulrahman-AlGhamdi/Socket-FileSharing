package com.android.share.ui.sender

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.share.manager.scan.ScanManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SenderViewModel @Inject constructor(private val scanManager: ScanManager) : ViewModel() {

    val scanState = scanManager.scanState

    fun startScanning() = viewModelScope.launch {
        scanManager.startScanning()
    }
}