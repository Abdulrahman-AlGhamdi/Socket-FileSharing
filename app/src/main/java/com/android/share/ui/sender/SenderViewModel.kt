package com.android.share.ui.sender

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.share.manager.request.RequestManager
import com.android.share.manager.scan.ScanManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SenderViewModel @Inject constructor(
    private val scanManager: ScanManager,
    private val requestManager: RequestManager
) : ViewModel() {

    val scanState = scanManager.scanState
    val requestState = requestManager.requestState

    fun startScanning() = viewModelScope.launch {
        scanManager.startScanning()
    }

    fun requestConnection(receiver: String) = viewModelScope.launch {
        requestManager.requestConnection(receiver)
    }

    fun closeClientSocket() = requestManager.closeClientSocket()
}