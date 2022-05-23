package com.android.share.ui.sender

import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.share.manager.sender.SenderManager
import com.android.share.manager.scan.ScanManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SenderViewModel @Inject constructor(
    private val scanManager: ScanManager,
    private val senderManager: SenderManager
) : ViewModel() {

    val scanState = scanManager.scanState
    val requestState = senderManager.sendState

    fun startScanning() = viewModelScope.launch {
        scanManager.startScanning()
    }

    fun sendRequest(receiver: String, documentFile: DocumentFile) = viewModelScope.launch {
        senderManager.sendRequest(receiver, documentFile)
    }

    fun closeClientSocket() = senderManager.closeClientSocket()
}