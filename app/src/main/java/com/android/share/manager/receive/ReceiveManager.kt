package com.android.share.manager.receive

import kotlinx.coroutines.flow.StateFlow

interface ReceiveManager {

    var receiveCallback: ReceiveCallback?

    val receiveState: StateFlow<ReceiveManagerImpl.ReceiveState>

    val requestState: StateFlow<ReceiveManagerImpl.RequestState>

    suspend fun startReceiving()

    fun closeServerSocket()
}