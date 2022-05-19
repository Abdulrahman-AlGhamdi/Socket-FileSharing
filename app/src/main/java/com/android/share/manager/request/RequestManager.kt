package com.android.share.manager.request

import kotlinx.coroutines.flow.StateFlow

interface RequestManager {

    val requestState: StateFlow<RequestManagerImpl.RequestState>

    suspend fun requestConnection(receiver: String)

    fun closeClientSocket()
}