package com.android.share.manager.broadcast

import kotlinx.coroutines.flow.StateFlow

interface BroadcastManager {

    var requestCallback: RequestCallback?

    val receiveState: StateFlow<BroadcastManagerImpl.ReceiveState>

    val requestState: StateFlow<BroadcastManagerImpl.RequestState>

    suspend fun startReceiving()

    fun closeServerSocket()
}