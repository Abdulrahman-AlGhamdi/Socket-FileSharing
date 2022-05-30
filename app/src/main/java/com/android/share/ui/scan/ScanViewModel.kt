package com.android.share.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.share.manager.scan.ScanManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(private val scanManager: ScanManager) : ViewModel() {

    val scanState = scanManager.scanState

    fun startScanning() = viewModelScope.launch(Dispatchers.IO) {
        scanManager.startScanning()
    }
}