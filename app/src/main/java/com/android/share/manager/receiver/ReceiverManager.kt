package com.android.share.manager.receiver

import com.android.share.manager.sender.SenderCallback
import kotlinx.coroutines.flow.StateFlow

interface ReceiverManager {

    var senderCallback: SenderCallback?

    val receiveState: StateFlow<ReceiverManagerImpl.ReceiveState>

    suspend fun startReceiving()

    fun closeServerSocket()
}