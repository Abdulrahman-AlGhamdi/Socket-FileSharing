package com.android.share.manager.request

import com.android.share.manager.request.RequestManagerImpl.RequestState.*
import com.android.share.util.readStringFromStream
import com.android.share.util.writeStringAsStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject

class RequestManagerImpl @Inject constructor() : RequestManager {

    private val _requestState: MutableStateFlow<RequestState> = MutableStateFlow(Idle)
    override val requestState = _requestState.asStateFlow()

    private lateinit var clientSocket: Socket
    private lateinit var clientInputStream: InputStream
    private lateinit var clientOutputStream: OutputStream

    override suspend fun requestConnection(receiver: String) = withContext(Dispatchers.IO) {
        try {
            _requestState.value = RequestStarted
            clientSocket = Socket(receiver, 52525)
            clientInputStream = clientSocket.getInputStream()
            clientOutputStream = clientSocket.getOutputStream()

            clientOutputStream.writeStringAsStream("connect")
            val respond = clientInputStream.readStringFromStream()
            if (respond == "accept") _requestState.value = RequestAccepted
            if (respond == "refuse") _requestState.value = RequestRefused
        } catch (exception: Exception) {
            _requestState.value = RequestFailed
            exception.printStackTrace()
        } finally {
            closeClientSocket()
            _requestState.value = Idle
        }
    }

    override fun closeClientSocket() {
        if (::clientOutputStream.isInitialized) {
            clientOutputStream.flush()
            clientOutputStream.close()
        }
        if (::clientInputStream.isInitialized) clientInputStream.close()
        if (::clientSocket.isInitialized) clientSocket.close()
    }

    sealed class RequestState {
        object Idle : RequestState()
        object RequestFailed : RequestState()
        object RequestStarted : RequestState()
        object RequestRefused : RequestState()
        object RequestAccepted : RequestState()
    }
}