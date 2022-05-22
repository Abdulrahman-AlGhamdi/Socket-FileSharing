package com.android.share.manager.request

import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.StateFlow

interface RequestManager {

    val requestState: StateFlow<RequestManagerImpl.RequestState>

    suspend fun requestConnection(receiver: String, documentFile: DocumentFile)

    fun closeClientSocket()
}