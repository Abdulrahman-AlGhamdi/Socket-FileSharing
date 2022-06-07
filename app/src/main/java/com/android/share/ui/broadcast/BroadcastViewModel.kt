package com.android.share.ui.broadcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.share.manager.broadcast.BroadcastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BroadcastViewModel @Inject constructor(
    private val broadcastManager: BroadcastManager
) : ViewModel() {

    val receiveState = broadcastManager.receiveState
    val requestState = broadcastManager.requestState

    fun startReceiving() = viewModelScope.launch {
        broadcastManager.startReceiving()
    }

    fun requestCallback(respond: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        if (respond) broadcastManager.requestCallback?.accept()
        else broadcastManager.requestCallback?.refuse()
    }

    fun closeServerSocket() = broadcastManager.closeServerSocket()
}