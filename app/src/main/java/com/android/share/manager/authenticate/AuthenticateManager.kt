package com.android.share.manager.authenticate

import com.android.share.manager.request.RequestCallback
import kotlinx.coroutines.flow.StateFlow

interface AuthenticateManager {

    var requestCallback: RequestCallback?

    val authenticateState: StateFlow<AuthenticateManagerImpl.AuthenticateState>

    suspend fun startAuthentication()

    fun closeServerSocket()
}