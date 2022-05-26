package com.android.share.manager.send

import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.StateFlow

interface SendManager {

    val requestState: StateFlow<SendManagerImpl.RequestState>

    suspend fun sendRequest(receiver: String, documentFile: DocumentFile)

    fun closeClientSocket()
}