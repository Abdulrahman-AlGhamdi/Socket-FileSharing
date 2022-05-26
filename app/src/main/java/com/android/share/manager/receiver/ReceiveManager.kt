package com.android.share.manager.receiver

import kotlinx.coroutines.flow.StateFlow

interface ReceiveManager {

    var receiveCallback: ReceiveCallback?

    val receiveState: StateFlow<ReceiveManagerImpl.ReceiveState>

    suspend fun startReceiving()

    fun closeServerSocket()
}