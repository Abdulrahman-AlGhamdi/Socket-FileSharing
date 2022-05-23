package com.android.share.manager.authenticate

import com.android.share.manager.sender.SenderCallback
import kotlinx.coroutines.flow.StateFlow

interface ReceiverManager {

    var senderCallback: SenderCallback?

    val receiveState: StateFlow<ReceiverManagerImpl.ReceiveState>

    suspend fun startAuthentication()

    fun closeServerSocket()
}