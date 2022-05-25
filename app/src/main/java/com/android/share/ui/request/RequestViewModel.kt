package com.android.share.ui.request

import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.share.manager.sender.RequestManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RequestViewModel @Inject constructor(private val requestManager: RequestManager) : ViewModel() {

    val requestState = requestManager.requestState

    fun sendRequest(receiver: String, documentFile: DocumentFile) = viewModelScope.launch {
        requestManager.sendRequest(receiver, documentFile)
    }

    fun closeClientSocket() = requestManager.closeClientSocket()
}