package com.android.share.manager.sender

import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.StateFlow

interface RequestManager {

    val requestState: StateFlow<RequestManagerImpl.RequestState>

    suspend fun sendRequest(receiver: String, documentFile: DocumentFile)

    fun closeClientSocket()
}