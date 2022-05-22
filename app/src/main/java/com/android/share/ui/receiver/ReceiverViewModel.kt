package com.android.share.ui.receiver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.share.manager.authenticate.AuthenticateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReceiverViewModel @Inject constructor(
    private val authenticateManager: AuthenticateManager
) : ViewModel() {

    val authenticateState = authenticateManager.authenticateState

    fun startAuthentication() = viewModelScope.launch {
        authenticateManager.startAuthentication()
    }

    fun requestCallback(respond: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        if (respond) authenticateManager.requestCallback?.accept()
        else authenticateManager.requestCallback?.refuse()
    }

    fun closeServerSocket() = authenticateManager.closeServerSocket()
}