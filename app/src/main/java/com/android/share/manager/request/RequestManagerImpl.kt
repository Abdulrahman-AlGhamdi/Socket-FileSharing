package com.android.share.manager.request

import androidx.documentfile.provider.DocumentFile
import com.android.share.manager.request.RequestManagerImpl.RequestState.*
import com.android.share.util.readStringFromStream
import com.android.share.util.writeStringAsStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*
import javax.inject.Inject

class RequestManagerImpl @Inject constructor() : RequestManager {

    private val _requestState: MutableStateFlow<RequestState> = MutableStateFlow(Idle)
    override val requestState = _requestState.asStateFlow()

    private lateinit var clientSocket: Socket

    override suspend fun requestConnection(
        receiver: String,
        documentFile: DocumentFile
    ) = withContext(Dispatchers.IO) {
        try {
            _requestState.value = RequestStarted
            clientSocket = Socket(receiver, 52525)

            clientSocket.getOutputStream().use { output ->
                val name = documentFile.name ?: UUID.randomUUID().toString()
                output.writeStringAsStream("share:$name")

                clientSocket.getInputStream().use { input ->
                    val respond = input.readStringFromStream()
                    if (respond == "accept") _requestState.value = RequestAccepted
                    if (respond == "refuse") _requestState.value = RequestRefused
                }
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
            _requestState.value = RequestFailed
        } finally {
            clientSocket.close()
        }
    }

    override fun closeClientSocket() {
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