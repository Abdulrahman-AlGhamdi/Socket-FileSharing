package com.android.share.ui.send

import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.share.manager.send.SendManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SendViewModel @Inject constructor(private val sendManager: SendManager) : ViewModel() {

    val sendState = sendManager.requestState

    fun sendRequest(receiver: String, documentFile: DocumentFile) = viewModelScope.launch {
        sendManager.sendRequest(receiver, documentFile)
    }

    fun closeClientSocket() = sendManager.closeClientSocket()
}