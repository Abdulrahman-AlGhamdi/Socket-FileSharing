package com.android.share.manager.authenticate

import kotlinx.coroutines.flow.StateFlow

interface AuthenticateManager {

    val authenticateState: StateFlow<AuthenticateManagerImpl.AuthenticateState>

    suspend fun startAuthentication()

    suspend fun acceptConnection(accept: Boolean)

    fun closeServerSocket()
}