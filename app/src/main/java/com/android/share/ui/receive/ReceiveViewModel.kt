package com.android.share.ui.receive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.share.manager.receive.ReceiveManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReceiveViewModel @Inject constructor(
    private val receiveManager: ReceiveManager
) : ViewModel() {

    val receiveState = receiveManager.receiveState
    val requestState = receiveManager.requestState

    fun startReceiving() = viewModelScope.launch {
        receiveManager.startReceiving()
    }

    fun requestCallback(respond: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        if (respond) receiveManager.receiveCallback?.accept()
        else receiveManager.receiveCallback?.refuse()
    }

    fun closeServerSocket() = receiveManager.closeServerSocket()
}