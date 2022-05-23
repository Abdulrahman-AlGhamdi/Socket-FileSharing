package com.android.share.ui.receiver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.share.manager.authenticate.ReceiverManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReceiverViewModel @Inject constructor(
    private val receiverManager: ReceiverManager
) : ViewModel() {

    val authenticateState = receiverManager.receiveState

    fun startAuthentication() = viewModelScope.launch {
        receiverManager.startAuthentication()
    }

    fun requestCallback(respond: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        if (respond) receiverManager.senderCallback?.accept()
        else receiverManager.senderCallback?.refuse()
    }

    fun closeServerSocket() = receiverManager.closeServerSocket()
}