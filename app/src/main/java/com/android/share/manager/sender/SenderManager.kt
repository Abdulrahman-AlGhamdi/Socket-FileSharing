package com.android.share.manager.sender

import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.StateFlow

interface SenderManager {

    val sendState: StateFlow<SenderManagerImpl.SendState>

    suspend fun sendRequest(receiver: String, documentFile: DocumentFile)

    fun closeClientSocket()
}